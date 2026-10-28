package atf.shop.api;
import atf.console.AtfConsoleReporter;
import atf.console.junit.AtfConsoleTestContext;
import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportPolicy;
import atf.core.http.request.ApiRequest;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.shop.support.ShopJsonEvidence;

import atf.core.config.ApiTransportMode;
import atf.core.http.auth.AuthProfiles;
import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.transport.ApiLiveTransportCatalog;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.UrlConnectionTransport;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.model.ShopCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** Small API adapter shared by screen-specific API modules and component tests. */
public final class ShopApiClient implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ShopApiRequestFactory requests;
    private final URI endpoint;
    private final ApiTransport transport;
    private final ApiExecutor executor;
    private String bearerToken = "";
    private boolean closed;
    private java.util.function.Consumer<ApiExecutionResult> observed = execution -> { };
    private java.util.function.BiConsumer<ApiRequest, Throwable> failed = (request, error) -> { };
    private boolean observerInstalled;

    private ShopApiClient(URI endpoint, ApiTransport transport,
                                  ShopApiRequestFactory requests,
                                  java.time.Duration timeout,
                                  ApiTransportMode transportMode, AtfConsoleSink console) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.requests = Objects.requireNonNull(requests, "requests");
        var settings = new ApiSettings(endpoint, AuthProfiles.NONE,
                timeout, java.util.Set.of(), 1);
        // The factory is intentionally transport-agnostic; the explicit mode keeps
        // report provenance aligned with the transport selected by the composition root.
        this.executor = new ApiExecutor(transport, settings.snapshot(), safeConsole(console),
                Objects.requireNonNull(transportMode, "transportMode"));
    }

    public static ShopApiClient live(ShopApiConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        if (configuration.mode() != ApiTransportMode.LIVE) {
            throw new IllegalArgumentException("Practice Shop live API client requires api.mode=live");
        }
        ApiLiveTransportCatalog catalog = ApiLiveTransportCatalog.single("practice-shop",
                configuration.endpoint(), AuthProfiles.NONE, configuration.timeout(),
                (endpoint, authenticator) -> new UrlConnectionTransport(authenticator, endpoint.timeout()));
        ApiTransport transport = catalog.open("practice-shop",
                ApiTransportPolicy.explicitLive(),
                new ConfiguredApiAuthenticator(Map.of()));
        return new ShopApiClient(configuration.endpoint(), transport,
                new ShopApiRequestFactory(), configuration.timeout(), ApiTransportMode.LIVE,
                configuredConsoleSink());
    }

    public static ShopApiClient forTransport(URI endpoint, ApiTransport transport) {
        return forTransport(endpoint, transport, ApiTransportMode.ACCEPTANCE);
    }

    /**
     * Exposes provenance selection for deterministic contract tests and custom transports.
     * The normal production entry points remain {@link #live(ShopApiConfiguration)}
     * and {@link #forTransport(URI, ApiTransport)}.
     */
    public static ShopApiClient forTransport(URI endpoint, ApiTransport transport,
                                                     ApiTransportMode transportMode) {
        return forTransport(endpoint, transport, transportMode, configuredConsoleSink());
    }

    private static AtfConsoleSink configuredConsoleSink() {
        return AtfConsoleTestContext.current() == null
                ? AtfConsoleReporter.configuredStdout()
                : AtfConsoleTestContext.configuredSink();
    }

    public static ShopApiClient forTransport(URI endpoint, ApiTransport transport,
                                             ApiTransportMode transportMode, AtfConsoleSink console) {
        return new ShopApiClient(endpoint, transport, new ShopApiRequestFactory(),
                java.time.Duration.ofSeconds(10), transportMode, Objects.requireNonNull(console, "console"));
    }

    public ApiExecutionResult execute(ApiRequest request) {
        ensureOpen();
        ApiExecutionResult result;
        try {
            result = executor.execute(request);
        } catch (RuntimeException | Error failure) {
            try { failed.accept(request, failure); } catch (RuntimeException | Error observerFailure) {
                if (failure != observerFailure) failure.addSuppressed(observerFailure);
            }
            throw failure;
        }
        observed.accept(result);
        return result;
    }

    /** One run-owned observer captures returned exchanges and failed requests without changing transport semantics. */
    public void observeExecutions(java.util.function.Consumer<ApiExecutionResult> observed,
                                  java.util.function.BiConsumer<ApiRequest, Throwable> failed) {
        ensureOpen();
        if (observerInstalled) throw new IllegalStateException("API execution observer is already installed");
        this.observed = Objects.requireNonNull(observed, "observed");
        this.failed = Objects.requireNonNull(failed, "failed");
        observerInstalled = true;
    }

    /** Consumer-owned PII policy is applied before the generic console formatter. */
    private static AtfConsoleSink safeConsole(AtfConsoleSink console) {
        Objects.requireNonNull(console, "console");
        return event -> {
            if (event.type() == AtfConsoleEvent.Type.CODE) {
                console.publish(AtfConsoleEvent.code(event.scope(), event.detailName(),
                        ShopJsonEvidence.redact(event.detailValue())));
            } else if (event.type() == AtfConsoleEvent.Type.DETAIL && "url".equals(event.detailName())) {
                String url = event.detailValue();
                int query = url.indexOf('?');
                console.publish(AtfConsoleEvent.detail(event.scope(), event.detailName(),
                        query < 0 ? url : url.substring(0, query) + "?<redacted>"));
            } else {
                console.publish(event);
            }
        };
    }

    public ShopApiSession login(ShopCredentials credentials) {
        ensureOpen();
        ApiExecutionResult result = execute(requests.login(credentials));
        var response = result.response();
        String token = ShopApiHttpStatus.isSuccess(response.status())
                ? token(response.body()) : "";
        bearerToken = token;
        return new ShopApiSession(token, result);
    }

    public ShopApiRequestFactory requests() { return requests; }
    public URI endpoint() { return endpoint; }
    public String bearerToken() { return bearerToken; }

    /**
     * Closes the logical API session owned by this client. The current HTTP
     * transport closes each URL connection per exchange, so cleanup here is
     * deliberately about session state: no bearer token can survive the test
     * boundary and later network use fails fast.
     */
    @Override
    public void close() {
        bearerToken = "";
        observed = execution -> { };
        failed = (request, error) -> { };
        closed = true;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Practice Shop API client is closed");
    }

    private static String token(String body) {
        try {
            JsonNode root = JSON.readTree(body == null ? "" : body);
            if (root == null || !root.isObject()) {
                throw new IllegalStateException("Practice Shop login response is not a JSON object");
            }
            for (ShopApiJsonField field : new ShopApiJsonField[]{
                    ShopApiJsonField.ACCESS_TOKEN,
                    ShopApiJsonField.ACCESS_TOKEN_CAMEL,
                    ShopApiJsonField.TOKEN}) {
                String value = root.path(field.wireValue()).asText("");
                if (!value.isBlank()) return value;
            }
            throw new IllegalStateException("Practice Shop login response does not contain an access token");
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("Practice Shop login response is malformed JSON", error);
        }
    }
}
