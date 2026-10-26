package atf.reporting.api.execution;
import atf.testkit.AtfComponentTest;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionException;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.config.ApiSettings;
import atf.core.config.ApiBootstrapConfiguration;
import atf.core.config.AtfConfiguration;
import atf.core.config.ReportingSettings;
import atf.core.http.transport.ApiTransportProvenance;
import atf.core.reporting.console.AtfConsoleEvent;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class ApiModuleFactoryTest {
    @Test
    void bootstrapPreservesConsoleDetailsAndExplicitTransportProvenance() {
        var events = new ArrayList<AtfConsoleEvent>();
        ApiTransport acceptance = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "{\"ok\":true}", Map.of(), Duration.ZERO, url));
        var configuration = ApiBootstrapConfiguration.acceptance(
                new ApiSettings(URI.create("https://example.test"), "none"));
        var module = ApiModuleFactory.bootstrap(configuration, acceptance, null, events::add);
        var result = module.executor().execute(ApiRequest.of("GET", "/health"));
        assertEquals(ApiTransportProvenance.ACCEPTANCE_MOCK, result.provenance());
        assertTrue(events.stream().anyMatch(event -> event.type() == AtfConsoleEvent.Type.CODE));
        assertTrue(events.stream().anyMatch(event -> "response.body".equals(event.detailName())));
    }

    @Test
    void composesOneScopedGraphAndPreservesSettingsSnapshot() {
        ApiTransport transport = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "[]", Map.of(), Duration.ZERO, url));
        var module = ApiModuleFactory.fromTransport(transport, URI.create("https://example.test"));

        assertEquals(URI.create("https://example.test"), module.settings().baseUri());
        assertEquals(200, module.users().getCollection().response().status());
        assertEquals(200, module.posts().getCollection().response().status());
    }

    @Test
    void moduleAndExecutorAreComposedFromTheSameImmutableSnapshot() {
        ApiTransport transport = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, url, Map.of(), Duration.ZERO, url));
        var configured = new ApiSettings(URI.create("https://snapshot.example"), "snapshot",
                Duration.ofSeconds(3), java.util.Set.of(429), 2);
        var module = ApiModuleFactory.fromTransport(transport, configured);

        var result = module.executor().execute(ApiRequest.of("GET", "/health"));

        assertEquals("https://snapshot.example/health", result.response().url());
        assertEquals(Duration.ofSeconds(3), module.settings().timeout());
        assertEquals(2, module.settings().maxAttempts());
    }

    @Test
    void bootstrapSelectsAcceptanceWithoutAllowingAHiddenLiveSwitch() {
        ApiTransport acceptance = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "acceptance", Map.of(), Duration.ZERO, url));
        ApiTransport live = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "live", Map.of(), Duration.ZERO, url));
        var configuration = ApiBootstrapConfiguration.acceptance(
                new ApiSettings(URI.create("https://configured.example"), "none"));

        var module = ApiModuleFactory.bootstrap(configuration, acceptance, live);

        assertEquals("acceptance", module.executor().execute(ApiRequest.of("GET", "/health"))
                .response().body());
        assertEquals(ApiTransportProvenance.ACCEPTANCE_MOCK,
                module.executor().execute(ApiRequest.of("GET", "/health")).provenance());
    }

    @Test
    void explicitLiveBootstrapMarksTheExchangeAsNetworkBacked() {
        ApiTransport live = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "live", Map.of(), Duration.ZERO, url));
        var configuration = ApiBootstrapConfiguration.live(
                new ApiSettings(URI.create("https://configured.example"), "none"));

        var module = ApiModuleFactory.bootstrap(configuration, live, live);

        assertEquals(ApiTransportProvenance.LIVE_HTTP,
                module.executor().execute(ApiRequest.of("GET", "/health")).provenance());
    }

    @Test
    void composesExecutionFromTheUnifiedRuntimeSnapshot() {
        ApiTransport transport = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, url, Map.of(), Duration.ZERO, url));
        var runtime = new AtfConfiguration(
                new ApiSettings(URI.create("https://runtime.example"), "runtime",
                        Duration.ofSeconds(4), java.util.Set.of(503), 2),
                new ReportingSettings(96)).snapshot();

        var module = ApiModuleFactory.fromTransport(transport, runtime);
        var result = module.executor().execute(ApiRequest.of("GET", "/health"));

        assertEquals("https://runtime.example/health", result.response().url());
        assertEquals(Duration.ofSeconds(4), module.settings().timeout());
        assertEquals(2, module.settings().maxAttempts());
    }

    @Test
    void composedExecutorUsesConfiguredTimeoutForTheDefaultExecutePath() {
        ApiTransport pending = (request, url, auth) -> new CompletableFuture<>();
        var settings = new ApiSettings(URI.create("https://example.test"), "default",
                Duration.ofMillis(80), java.util.Set.of(), 1);
        var module = ApiModuleFactory.fromTransport(pending, settings);

        assertThrows(ApiExecutionException.class, () -> module.executor().execute(ApiRequest.of("GET", "/pending")));
    }

    @Test
    void parallelModulesKeepIndependentSnapshotsAndTransportUrls() throws Exception {
        ApiTransport transport = (request, url, auth) -> CompletableFuture.completedFuture(
                new ApiResponse(200, url, Map.of(), Duration.ZERO, url));
        var firstSettings = new ApiSettings(URI.create("https://first.example"), "first");
        var secondSettings = new ApiSettings(URI.create("https://second.example"), "second");

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> ApiModuleFactory.fromTransport(transport, firstSettings)
                    .executor().execute(ApiRequest.of("GET", "/health")));
            var second = executor.submit(() -> ApiModuleFactory.fromTransport(transport, secondSettings)
                    .executor().execute(ApiRequest.of("GET", "/health")));

            assertEquals("https://first.example/health", first.get(5, TimeUnit.SECONDS).response().url());
            assertEquals("https://second.example/health", second.get(5, TimeUnit.SECONDS).response().url());
        }
    }
}
