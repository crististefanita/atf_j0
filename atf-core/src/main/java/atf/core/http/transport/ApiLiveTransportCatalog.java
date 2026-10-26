package atf.core.http.transport;

import atf.core.config.ApiBootstrapConfiguration;
import atf.core.config.source.ApiEnvironment;
import atf.core.config.ApiTransportMode;
import atf.core.config.ApiTransportPolicy;
import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.auth.AuthProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Explicit, named catalog of live HTTP endpoints.
 *
 * <p>The catalog is an adapter, not a network singleton: the caller supplies
 * the policy, client and authenticator. Opening an endpoint fails closed unless
 * the policy is {@link ApiTransportMode#LIVE} with explicit opt-in. Acceptance
 * callers keep using their injected mock transport and never consult this
 * catalog.</p>
 */
public final class ApiLiveTransportCatalog {
    public static final String DEFAULT_ENDPOINT = "default";

    private final Map<String, LiveEndpoint> endpoints;
    private final LiveTransportFactory transportFactory;

    public ApiLiveTransportCatalog(Collection<LiveEndpoint> endpoints, HttpClient client) {
        this(endpoints, (endpoint, authenticator) ->
                new HttpClientTransport(client, authenticator, endpoint.timeout()));
    }

    public ApiLiveTransportCatalog(Collection<LiveEndpoint> endpoints,
                                   LiveTransportFactory transportFactory) {
        Objects.requireNonNull(endpoints, "endpoints");
        this.transportFactory = Objects.requireNonNull(transportFactory, "transportFactory");
        Map<String, LiveEndpoint> indexed = new LinkedHashMap<>();
        for (LiveEndpoint endpoint : endpoints) {
            LiveEndpoint value = Objects.requireNonNull(endpoint, "endpoint");
            if (indexed.putIfAbsent(value.name(), value) != null) {
                throw new IllegalArgumentException("Duplicate live endpoint: " + value.name());
            }
        }
        if (indexed.isEmpty()) throw new IllegalArgumentException("At least one live endpoint is required");
        this.endpoints = Map.copyOf(indexed);
    }

    public static ApiLiveTransportCatalog from(ApiBootstrapConfiguration configuration,
                                                HttpClient client) {
        Objects.requireNonNull(configuration, "configuration");
        var settings = configuration.settings();
        return single(DEFAULT_ENDPOINT, settings.baseUri(), settings.defaultAuthProfile(),
                settings.timeout(), client);
    }

    public static ApiLiveTransportCatalog single(String name, URI baseUri, String authProfile,
                                                 Duration timeout, HttpClient client) {
        return new ApiLiveTransportCatalog(
                java.util.List.of(new LiveEndpoint(name, baseUri, authProfile, timeout)), client);
    }

    public static ApiLiveTransportCatalog single(String name, URI baseUri, String authProfile,
                                                 Duration timeout, LiveTransportFactory factory) {
        return new ApiLiveTransportCatalog(
                java.util.List.of(new LiveEndpoint(name, baseUri, authProfile, timeout)), factory);
    }

    public LiveEndpoint endpoint(String name) {
        LiveEndpoint endpoint = endpoints.get(name);
        if (endpoint == null) throw new IllegalArgumentException("Unknown live endpoint: " + name);
        return endpoint;
    }

    /** Opens a transport only after the typed live policy has passed validation. */
    public ApiTransport open(String name, ApiTransportPolicy policy,
                             ApiAuthenticator authenticator) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(authenticator, "authenticator");
        if (policy.mode() != ApiTransportMode.LIVE || !policy.liveOptIn()) {
            throw new IllegalArgumentException(
                    "Live catalog access requires an explicit LIVE transport opt-in");
        }
        LiveEndpoint endpoint = endpoint(name);
        if (ApiEnvironment.MOCK_BASE_URI.equals(endpoint.baseUri())) {
            throw new IllegalArgumentException(
                    "The acceptance placeholder cannot be opened as a live endpoint");
        }
        ApiTransport delegate = transportFactory.create(endpoint, authenticator);
        Objects.requireNonNull(delegate, "transportFactory result");
        return (request, url, ignoredAuthProfile) -> {
            if (!sameOrigin(endpoint.baseUri(), URI.create(url))) {
                return CompletableFuture.failedFuture(new IllegalArgumentException(
                        "Live request escaped endpoint origin: " + endpoint.name()));
            }
            String authProfile = request.authProfile().equals(AuthProfiles.NONE)
                    ? endpoint.authProfile() : request.authProfile();
            return delegate.send(request, url, authProfile);
        };
    }

    public record LiveEndpoint(String name, URI baseUri, String authProfile, Duration timeout) {
        public LiveEndpoint {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
            baseUri = Objects.requireNonNull(baseUri, "baseUri");
            String scheme = baseUri.getScheme();
            if (!baseUri.isAbsolute() || !("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("baseUri must be an absolute HTTP(S) URI");
            }
            if (baseUri.getUserInfo() != null || baseUri.getFragment() != null) {
                throw new IllegalArgumentException("baseUri must not contain credentials or a fragment");
            }
            authProfile = AuthProfiles.normalize(authProfile);
            timeout = Objects.requireNonNull(timeout, "timeout");
            if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        }
    }

    @FunctionalInterface
    public interface LiveTransportFactory {
        ApiTransport create(LiveEndpoint endpoint, ApiAuthenticator authenticator);
    }

    private static boolean sameOrigin(URI expected, URI actual) {
        return expected.getScheme().equalsIgnoreCase(actual.getScheme())
                && Objects.equals(expected.getHost(), actual.getHost())
                && effectivePort(expected) == effectivePort(actual);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
