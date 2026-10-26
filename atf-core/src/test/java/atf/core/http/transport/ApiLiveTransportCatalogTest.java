package atf.core.http.transport;

import atf.core.config.source.ApiEnvironment;
import atf.core.config.ApiTransportPolicy;
import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ApiLiveTransportCatalogTest {
    private static final ConfiguredApiAuthenticator NO_AUTH =
            new ConfiguredApiAuthenticator(Map.of());

    @Test
    void acceptancePolicyCannotOpenTheLiveCatalog() {
        var catalog = ApiLiveTransportCatalog.single("service", URI.create("https://service.example"),
                "none", Duration.ofSeconds(2), (endpoint, authenticator) ->
                        (request, url, auth) -> CompletableFuture.failedFuture(
                                new AssertionError("network factory must not be opened")));

        assertThrows(IllegalArgumentException.class,
                () -> catalog.open("service", ApiTransportPolicy.acceptance(), NO_AUTH));
    }

    @Test
    void acceptancePlaceholderCannotBePromotedToLiveByOptInAlone() {
        var catalog = ApiLiveTransportCatalog.single("service", ApiEnvironment.MOCK_BASE_URI,
                "none", Duration.ofSeconds(2), (endpoint, authenticator) ->
                        (request, url, auth) -> CompletableFuture.failedFuture(
                                new AssertionError("network factory must not be opened")));

        assertThrows(IllegalArgumentException.class,
                () -> catalog.open("service", ApiTransportPolicy.explicitLive(), NO_AUTH));
    }

    @Test
    void explicitLiveCatalogUsesTheNamedEndpointAndReturnsTransportEvidence() {
        URI base = URI.create("https://service.example");
        var catalog = ApiLiveTransportCatalog.single("service", base, "none",
                Duration.ofSeconds(2), (endpoint, authenticator) ->
                        (request, url, auth) -> CompletableFuture.completedFuture(
                                new ApiResponse(200, "{\"status\":\"ok\"}", Map.of(),
                                        Duration.ZERO, url)));
        var transport = catalog.open("service", ApiTransportPolicy.explicitLive(), NO_AUTH);

        var response = transport.send(ApiRequest.of("GET", "/health"),
                base + "/health", "none").join();

        assertEquals(200, response.status());
        assertEquals("{\"status\":\"ok\"}", response.body());
    }

    @Test
    void namedTransportRejectsARequestThatEscapesItsOrigin() {
        URI base = URI.create("http://localhost:32123");
        var catalog = ApiLiveTransportCatalog.single("service", base, "none",
                Duration.ofSeconds(2), (endpoint, authenticator) ->
                        (request, url, auth) -> CompletableFuture.failedFuture(
                                new AssertionError("escaped request must not reach transport")));
        var transport = catalog.open("service", ApiTransportPolicy.explicitLive(), NO_AUTH);

        assertThrows(CompletionException.class,
                () -> transport.send(ApiRequest.of("GET", "/health"),
                        "http://127.0.0.1:32123/health", "none").join());
    }
}
