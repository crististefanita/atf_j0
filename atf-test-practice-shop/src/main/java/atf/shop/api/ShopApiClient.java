package atf.shop.api;
import atf.api.console.AtfConsoleReporter;
import atf.api.console.junit.AtfConsoleTestContext;
import atf.api.core.reporting.console.AtfConsoleSink;
import atf.api.core.reporting.console.AtfConsoleEvent;
import atf.shop.support.ShopJsonEvidence;

import atf.api.core.config.ApiTransportMode;
import atf.api.core.http.auth.AuthProfiles;
import atf.api.core.http.auth.ConfiguredApiAuthenticator;
import atf.api.core.http.execution.ApiExecutor;
import atf.api.core.http.response.ApiExecutionResult;
import atf.api.core.http.transport.ApiLiveTransportCatalog;
import atf.api.core.http.transport.ApiTransport;
import atf.api.core.http.transport.UrlConnectionTransport;
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

    private ShopApiClient(URI endpoint, ApiTransport transport,
                                  ShopApiRequestFactory requests,
                                  java.time.Duration timeout,
                                  ApiTransportMode transportMode, AtfConsoleSink console) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.requests = Objects.requireNonNull(requests, "requests");
        var settings = new atf.api.core.config.ApiSettings(endpoint, AuthProfiles.NONE,
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
                atf.api.core.config.ApiTransportPolicy.explicitLive(),
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

    public ApiExecutionResult execute(atf.api.core.http.request.ApiRequest request) {
        ensureOpen();
        return executor.execute(request);
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
