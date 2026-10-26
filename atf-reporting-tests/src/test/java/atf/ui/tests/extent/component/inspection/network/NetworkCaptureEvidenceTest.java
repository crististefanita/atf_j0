package atf.ui.tests.extent.component.inspection.network;

import atf.reporting.ui.network.ReportNetworkCaptureStatus;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.ui.network.ReportNetworkAggregateMarkup;

import atf.debugui.DebugUiConfiguration;
import atf.debugui.DebugUiServer;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.contract.ReportUiContract;
import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.locators.network.ReportNetworkCaptureLocators;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class NetworkCaptureEvidenceTest {
    private static final ReportNetworkCaptureLocators CAPTURE = new ReportNetworkCaptureLocators();
    @TempDir Path directory;

    @Test
    void zeroExchangesDistinguishesCompleteSelectionFromOverflowAndPendingCapture() {
        var complete = new ReportNetworkCaptureStatus("xhr, fetch; host allowlist", 0, 0, 400, 0, 0, false, 0);
        var truncated = new ReportNetworkCaptureStatus("xhr, fetch; host allowlist", 2, 1, 400, 1, 0, true, 1);
        var pending = new ReportNetworkCaptureStatus("xhr, fetch; host allowlist", 1, 0, 400, 0, 1, false, 0);
        for (var capture : List.of(complete, truncated, pending)) {
            var aggregate = ReportNetworkInspector.aggregate(List.of(), List.of(), capture);
            String html = ReportNetworkAggregateMarkup.substepBody(aggregate);
            assertTrue(ReportNetworkAggregateMarkup.hasContent(aggregate));
            assertTrue(html.contains("Capture coverage"));
            assertTrue(html.contains(capture.complete() ? "Complete within scope" : "Incomplete within scope"));
            assertTrue(html.contains("Setup exchanges"));
            assertTrue(html.contains("Causal exchanges"));
            assertEquals(0, aggregate.observations(), "Setup/pending capture cannot fabricate a causal exchange");
        }
        assertFalse(ReportNetworkAggregateMarkup.hasContent(ReportNetworkInspector.aggregate(List.of(), List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportNetworkCaptureStatus("fetch", 2, 1, 0, 0, 0, false, 0));
    }

    @Test
    void browserShowsOriginalNonApiRequestUnderItsCheckpointAndCoverageSurvivesSessionSnapshot() throws Exception {
        Path artifact = createReport("network-complete.html",
                new ReportNetworkCaptureStatus("xhr, fetch; host allowlist", 2, 2, 435, 0, 0, false, 1));
        String html = Files.readString(artifact);
        assertTrue(html.contains("Capture coverage"));
        assertTrue(html.contains("Complete within scope"));
        assertTrue(html.contains("Request details"));
        assertTrue(html.contains("Response details"));
        assertTrue(html.contains("Execution evidence"));
        assertTrue(html.contains("/cart/add/1"));

        try (var server = DebugUiServer.start(DebugUiConfiguration.forHtml(artifact, 0));
             var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = browser.page();
            page.navigate(server.uriFor(artifact) + "#test-view");
            var locators = new ReportUiLocators();
            locators.navTab(page, ReportUiContract.Tab.TESTS).click();
            locators.testOwnerItem(page, 0).click();
            for (String title : List.of("Selected browser traffic", "TEST BODY", "WHEN item is added to cart",
                    "API validation", "GET /cart/add/1", "Browser network summary", "Network aggregate")) {
                expand(page, title);
            }
            CAPTURE.coverage(page).click();
            String text = CAPTURE.document(page).innerText();
            assertTrue(text.contains("WHEN item is added to cart"));
            assertTrue(text.contains("GET /cart/add/1"));
            assertTrue(text.contains("Complete within scope"));
            assertTrue(text.contains("Setup exchanges"));
            assertTrue(text.contains("Causal exchanges"));
            assertFalse(text.contains("Incomplete within scope"));
        }
    }

    @Test
    void incompleteCaptureIsRenderedWithoutInventingAFailedBusinessStep() throws Exception {
        Path artifact = createReport("network-incomplete.html",
                new ReportNetworkCaptureStatus("xhr, fetch; host allowlist", 3, 2, 435, 1, 0, true, 1));
        String html = Files.readString(artifact);
        assertTrue(html.contains("Incomplete within scope"));
        assertFalse(html.contains("Runtime diagnostic"));
        assertTrue(html.contains("Dropped"));
        assertTrue(html.contains("Limit reached"));
        try (var server = DebugUiServer.start(DebugUiConfiguration.forHtml(artifact, 0));
             var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = browser.page();
            page.navigate(server.uriFor(artifact).toString());
            var locators = new ReportUiLocators();
            locators.navTab(page, ReportUiContract.Tab.TESTS).click();
            locators.testOwnerItem(page, 0).click();
            for (String title : List.of("Selected browser traffic", "TEST BODY",
                    "Browser network summary", "Network aggregate")) expand(page, title);
            CAPTURE.coverage(page).click();
            assertTrue(CAPTURE.document(page).innerText().contains("Incomplete within scope"));
            assertFalse(CAPTURE.document(page).innerText().contains("Complete within scope"));
        }
    }

    private Path createReport(String fileName, ReportNetworkCaptureStatus capture) {
        var setup = exchange("/catalog/lookup");
        var causal = exchange("/cart/add/1");
        Path artifact;
        try (var session = UiExtentReportSession.openConfigured(directory, fileName, AtfConsoleSink.noOp())) {
            artifact = session.reportPath();
            var scenario = session.startScenario("NetworkJourney", "component.network", "Selected browser traffic", List.of("ui", "api"));
            var phase = scenario.startPhase("TEST BODY");
            phase.setup(AtfGherkinKeyword.GIVEN, "catalog precondition", "/catalog",
                    List.of(new AtfAssertion("Product", "available", "available", true)),
                    List.of(), observedExchanges(List.of(setup)), AtfReportStatus.PASS);
            var assertions = List.of(new AtfAssertion("Cart", "contains product", "contains product", true));
            phase.step(AtfGherkinKeyword.WHEN, "item is added to cart", "/cart", "",
                    assertions, List.of(), observedExchanges(List.of(causal)), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            phase.networkInspector("Browser network summary", List.of(causal),
                    List.of(ReportNetworkInspector.Step.from("WHEN", "item is added to cart", assertions, List.of(causal))),
                    capture);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        return artifact;
    }

    @Test
    void terminalScreenRetainsDocumentExchangeWithoutDuplicatingItsSentence() throws Exception {
        Path artifact;
        try (var session = UiExtentReportSession.openConfigured(directory, "terminal.html", AtfConsoleSink.noOp())) {
            artifact = session.reportPath();
            var scenario = session.startScenario("TerminalJourney", "component.network", "Terminal document", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            phase.screenValidation("cleanup is confirmed", "/account/deleted", "", List.of(), List.of(),
                    observedExchanges(List.of(exchange("/account/delete"))),
                    ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(artifact);
        assertTrue(html.contains("UI validation"));
        assertTrue(html.contains("API validation"));
        assertTrue(html.contains("GET /account/delete"));
        assertEquals(1, html.split("cleanup is confirmed", -1).length - 1);
    }

    private static ReportExecutionEvidence exchange(String path) {
        var request = new ReportRequest("GET", path, Map.of(), Map.of(), "none", "");
        var response = new ReportResponse(200, "{\"accepted\":true}", Map.of("content-type", "text/html"),
                Duration.ofMillis(2), "https://service.test" + path);
        return new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(), List.of(), request,
                ReportTransportProvenance.ACCEPTANCE_MOCK);
    }

    private static void expand(Page page, String title) {
        var heading = CAPTURE.heading(page, title);
        if ("false".equals(heading.getAttribute("aria-expanded"))) heading.click();
    }

    @Test
    void fallbackSnapshotPreservesCoverageAndTheSharedPresentation() throws Exception {
        var capture = new ReportNetworkCaptureStatus("fetch; host allowlist", 3, 2, 50, 1, 0, true, 1);
        Path artifact = directory.resolve("fallback-network.html");
        try (var session = new HtmlReportSession(artifact)) {
            var scenario = session.startScenario("FallbackNetwork", "Captured traffic", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("Browser network summary", "TRACE", "network");
            request.networkAggregate(ReportNetworkInspector.aggregate(List.of(exchange("/cart/add/1")),
                    List.of(), capture));
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(artifact);
        assertTrue(html.contains("Incomplete within scope"));
        assertTrue(html.contains("Capture coverage"));
        assertTrue(html.contains("Setup exchanges"));
        assertTrue(html.contains("GET"));
        assertTrue(html.contains("/cart/add/1"));
    }
}
