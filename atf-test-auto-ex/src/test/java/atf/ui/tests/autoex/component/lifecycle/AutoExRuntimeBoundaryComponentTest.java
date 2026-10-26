package atf.ui.tests.autoex.component.lifecycle;

import atf.api.core.reporting.console.AtfConsoleSink;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfGherkinKeyword;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.api.testkit.AtfComponentTest;
import atf.api.ui.playwright.PlaywrightUiSession;
import atf.ui.autoex.config.AutoExConfigurationLoader;
import atf.ui.autoex.runtime.AutoExE2eRun;
import atf.ui.autoex.runtime.AutoExReportSession;
import atf.ui.autoex.runtime.AutoExObservedExchangeMapper;
import atf.api.ui.network.UiNetworkObservation;
import atf.api.ui.network.UiNetworkCapturePolicy;
import atf.api.reporting.api.request.ReportRequestHeaders;
import atf.api.reporting.api.evidence.ReportBodyAvailability;
import atf.ui.autoex.runtime.config.AutoExEvidenceProfile;
import atf.ui.autoex.support.trace.AutoExTraceStep;
import atf.ui.tests.autoex.extension.AutoExRunLifecycleAdapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static atf.api.core.http.request.HttpMethod.GET;

/** Local DOM only: exercises the main runtime without a JUnit context or remote application. */
@AtfComponentTest
class AutoExRuntimeBoundaryComponentTest {
    @Test
    void mainNetworkBridgeRetainsObservedHeadersBodyAndMissingCapture() {
        String body = " \r\n ";
        var input = new UiNetworkObservation(1, Instant.EPOCH, GET.value(),
                "https://fixture.invalid/catalog", "/catalog", -1, Map.of("X-Observed", "yes"), Map.of(),
                body, "", "none", "none", "fixture", "component", "", false,
                "transport unavailable", UiNetworkCapturePolicy.ResourceKind.FETCH);
        var mapped = AutoExObservedExchangeMapper.map(input);
        assertEquals(ReportRequestHeaders.Source.OBSERVED, mapped.request().headerEvidence().source());
        assertEquals(Map.of("X-Observed", "yes"), mapped.request().headerEvidence().values());
        assertEquals(body, mapped.request().bodyText());
        assertEquals(ReportBodyAvailability.AVAILABLE, mapped.request().bodyAvailability());
        assertEquals(ReportBodyAvailability.NOT_CAPTURED, mapped.response().bodyAvailability());
        assertEquals("transport unavailable", mapped.attemptHistory().getFirst().reason());
    }

    @Test
    void mainRuntimePublishesObservedUiEvidenceAndClosesIdempotently() throws Exception {
        var configuration = AutoExConfigurationLoader.load();
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.FINAL_AND_FAILURE);
        var output = Path.of(System.getProperty("atf.test.reportDirectory", "target/autoex-runtime-reports"))
                .toAbsolutePath().resolve("ui");
        Files.createDirectories(output);
        Path reportPath;
        try (var reports = AutoExReportSession.open(output, AutoExReportSession.fileName("runtime-main-contract"),
                AtfConsoleSink.noOp(), profile, configuration.browser())) {
            reportPath = reports.report().reportPath();
            var scenario = reports.report().startScenario("AutoExRuntimeBoundaryComponentTest",
                    getClass().getPackageName(), "the main runtime publishes an observed local checkpoint",
                    List.of("component", "local-dom"));
            var phase = scenario.startPhase("TEST BODY");
            var browser = PlaywrightUiSession.open(configuration.browser(), AtfConsoleSink.noOp());
            var run = new AutoExE2eRun(configuration, browser, scenario, phase, profile, false,
                    AutoExRunLifecycleAdapter.INSTANCE);
            try {
                browser.page().setContent("<html><body><h1 id='runtime-label'>Ready</h1></body></html>");
                String actual = run.text("#runtime-label", "read the local checkpoint");
                var checks = List.of(new AtfAssertion("Local heading", "Ready", actual, "Ready".equals(actual)));
                run.checkpoint(new LocalStep(AtfGherkinKeyword.THEN, "the observed heading is ready"), checks);
                run.finalScreenshot("the terminal local state is captured", checks);
                run.finish(null);
                run.finish(null);
            } catch (Exception | Error failure) {
                run.finish(failure);
                throw failure;
            }
        }
        String html = Files.readString(reportPath).replaceAll(
                "(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertTrue(html.contains("Get text"));
        assertTrue(html.contains("atf-ui-action-screenshot-link"));
        assertTrue(html.contains("data-atf-evidence-owner=\"ui\""));
        assertFalse(html.contains("API network inspector — journey summary"));
        assertFalse(html.contains("data-atf-evidence-owner=\"api\""));
    }

    private record LocalStep(AtfGherkinKeyword keyword, String title) implements AutoExTraceStep { }
}
