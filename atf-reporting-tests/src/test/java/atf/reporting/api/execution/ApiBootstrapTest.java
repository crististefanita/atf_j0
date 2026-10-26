package atf.reporting.api.execution;
import atf.testkit.AtfComponentTest;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.config.AtfConfiguration;
import atf.core.config.ReportingSettings;
import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest

class ApiBootstrapTest {
    private static final ApiTransport TRANSPORT = (request, url, auth) ->
            CompletableFuture.completedFuture(new ApiResponse(200, "{}", Map.of(), Duration.ZERO, url));

    @Test
    void acceptanceIsTheTypedDefaultAndKeepsRuntimeSnapshot() {
        var runtime = new AtfConfiguration(
                new ApiSettings(URI.create("https://acceptance.example"), "mock"),
                new ReportingSettings(96)).snapshot();

        var module = ApiBootstrap.acceptance(runtime, TRANSPORT);

        assertEquals(ApiTransportMode.ACCEPTANCE, ApiTransportBinding.acceptance(TRANSPORT).mode());
        assertEquals(runtime.api(), module.settings());
        assertEquals(200, module.executor().execute(ApiRequest.of("GET", "/health"))
                .response().status());
    }

    @Test
    void liveRequiresAnExplicitOptIn() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiTransportBinding(ApiTransportMode.LIVE, TRANSPORT, false));
        assertEquals(ApiTransportMode.LIVE, ApiTransportBinding.live(TRANSPORT).mode());
    }

    @Test
    void modeParsingDefaultsToAcceptanceAndRejectsUnknownValues() {
        assertEquals(ApiTransportMode.ACCEPTANCE, ApiTransportMode.parse(""));
        assertEquals(ApiTransportMode.LIVE, ApiTransportMode.parse(" live "));
        assertThrows(IllegalArgumentException.class, () -> ApiTransportMode.parse("shadow"));
    }
}
