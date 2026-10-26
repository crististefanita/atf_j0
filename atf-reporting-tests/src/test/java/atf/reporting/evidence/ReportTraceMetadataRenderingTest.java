package atf.reporting.evidence;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTraceMetadata;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ReportTraceMetadataRenderingTest {
    @Test
    void acceptsAnotherConsumersTypedEnvironmentAndCausalTraceThroughPublicPorts() throws Exception {
        Path report = Files.createTempDirectory("atf-consumer-contract-").resolve("report.html");
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "INTEGRATION")
                .put(ReportEnvironmentField.API_ENDPOINT, "https://catalog.example.test")
                .put(ReportEnvironmentField.SUITE, "private consumer suite")
                .put(ReportEnvironmentField.AUTHENTICATION, "Bearer raw-consumer-token")
                .build();
        var request = new ReportRequest("GET", "/inventory/42", Map.of(), Map.of(), "none", "");
        var response = new ReportResponse(200, "{\"available\":true}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(4),
                "https://catalog.example.test/inventory/42");
        var trace = ReportTraceMetadata.from(request)
                .withCapture("CATALOG-17", 17, "UI_ACTION", "ActionCaused",
                        "inventory-checkpoint", "Open inventory", "UI_BROWSER", "",
                        "available=true", "available=true")
                .withService("catalog-service");
        var execution = new ReportExecutionEvidence(response, 1, Duration.ofMillis(4), List.of(),
                List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(4), "SUCCESS", "final")),
                request, ReportTransportProvenance.LIVE_HTTP, trace);

        try (var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                new SensitiveDataRedactor(), AtfConsoleSink.noOp(), environment)) {
            var scenario = session.startScenario("CatalogContractTest", "inventory contract", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            var node = phase.startRequest("read inventory", "GET", "/inventory/42");
            node.execution(execution);
            node.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("https://catalog.example.test")),
                () -> assertTrue(html.contains("CATALOG-17")),
                () -> assertTrue(html.contains("inventory-checkpoint")),
                () -> assertTrue(html.contains("catalog-service")),
                () -> assertTrue(html.contains("REQUEST SIGNATURE")),
                () -> assertTrue(html.contains("***REDACTED***")),
                () -> assertFalse(html.contains("private consumer suite")),
                () -> assertFalse(html.contains("raw-consumer-token")));
    }

    @Test
    void rendersCausalTraceInsideExecutionEvidence() throws Exception {
        Path report = Files.createTempDirectory("atf-trace-render-").resolve("trace.html");
        var request = new ReportRequest("POST", "/carts/cart-1", Map.of(), Map.of(),
                "bearer", "{\"quantity\":2}");
        var response = new ReportResponse(200, "{\"ok\":true}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(4),
                "https://api.example.test/carts/cart-1");
        var trace = new ReportTraceMetadata("E7", 7, "UI_ACTION", "ActionCaused",
                "cart-checkpoint", "Click add to cart", "POST /carts/cart-1",
                "POST /carts/cart-1#body=body-digest", "request-digest", "response-digest",
                "UI_BROWSER", "HTTP 2xx + JSON", "status=200");
        var execution = new ReportExecutionEvidence(response, 1, Duration.ofMillis(4), List.of(),
                List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(4), "SUCCESS", "final")),
                request, ReportTransportProvenance.LIVE_HTTP, trace);

        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("TraceTests", "trace metadata", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            var node = phase.startRequest("cart request", "POST", "/carts/cart-1");
            node.execution(execution);
            node.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("REQUEST SIGNATURE")),
                () -> assertTrue(html.contains("EVENT ID")),
                () -> assertTrue(html.contains("E7")),
                () -> assertTrue(html.contains("ActionCaused")),
                () -> assertTrue(html.contains("cart-checkpoint")),
                () -> assertTrue(html.contains("#body=present")),
                () -> assertTrue(html.contains("present (internal digest hidden)")),
                () -> assertFalse(html.contains("body-digest")),
                () -> assertFalse(html.contains("request-digest")),
                () -> assertFalse(html.contains("response-digest")));
    }
}
