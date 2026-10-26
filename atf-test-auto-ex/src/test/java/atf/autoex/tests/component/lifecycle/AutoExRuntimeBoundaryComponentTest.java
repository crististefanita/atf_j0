package atf.autoex.tests.component.lifecycle;

import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.testkit.AtfComponentTest;
import atf.ui.playwright.PlaywrightUiSession;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.AutoExObservedExchangeMapper;
import atf.ui.network.UiNetworkObservation;
import atf.ui.network.UiNetworkCapturePolicy;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.locator.AutoExRoutes;
import atf.core.http.request.HttpHeader;
import atf.reporting.api.request.ReportRequestHeaders;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static atf.core.http.request.HttpMethod.GET;
import static atf.core.http.request.HttpMethod.POST;
import static atf.core.http.request.MediaTypes.APPLICATION_JSON;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.XHR;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.DOCUMENT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Local DOM only: exercises the main runtime without a JUnit context or remote application. */
@AtfComponentTest
class AutoExRuntimeBoundaryComponentTest {
    @Test
    void cartReadBackRejectsProductIdPrefixesWrongMethodsAndDocumentNeighbors() throws Exception {
        var source = AutoExE2eRun.withNetworkInspection(AutoExConfigurationLoader.load());
        var base = source.browser();
        var network = base.networkInspection().withCapturePolicy(UiNetworkCapturePolicy.asynchronous().includingDocuments())
                .withSettlementPolicy(UiNetworkSettlementPolicy.bounded(Duration.ofMillis(100), Duration.ofMillis(5)));
        var browserConfiguration = new UiBrowserConfiguration(base.baseUrl(), base.browser(), true,
                base.timeout(), base.navigationTimeout(), base.viewportWidth(), base.viewportHeight(), base.zoomPercent(),
                base.screenshotOnFailure(), base.retryPolicy(), network);
        var configuration = new AutoExConfiguration(source.baseUrl(), false, browserConfiguration,
                source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(),
                source.storeAvailabilityPolicy(), source.evidence());
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS, ReportScreenshotPolicy.NONE);
        Path output = Files.createTempDirectory("autoex-cart-selection-");
        try (var reports = AutoExReportSession.open(output, AutoExReportSession.fileName("cart-selection"),
                AtfConsoleSink.noOp(), profile, browserConfiguration)) {
            var scenario = reports.report().startScenario("CartSelection", getClass().getPackageName(),
                    "controlled journal neighbors", List.of("component"));
            var browser = PlaywrightUiSession.open(browserConfiguration, AtfConsoleSink.noOp());
            var run = new AutoExE2eRun(configuration, browser, scenario, scenario.startPhase("TEST BODY"), profile, true,
                    AutoExRunLifecycleAdapter.INSTANCE);
            Throwable failure = null;
            try {
                var inspector = browser.networkInspector();
                assertTrue(inspector.capture(cartObservation(configuration, "10", GET.value(), XHR)).isPresent());
                assertTrue(inspector.capture(cartObservation(configuration, "1", POST.value(), XHR)).isPresent());
                assertTrue(inspector.capture(cartObservation(configuration, "1", GET.value(), DOCUMENT)).isPresent());
                assertThrows(AssertionError.class, () -> run.awaitCartExchange("1"));
                var completed = inspector.capture(cartObservation(configuration, "1?quantity=2", GET.value(), XHR))
                        .orElseThrow();
                assertTrue(completed.url().endsWith("?redacted-query"));
                assertDoesNotThrow(() -> run.awaitCartExchange("1"));
            } catch (Exception | Error unexpected) {
                failure = unexpected;
                throw unexpected;
            } finally {
                run.finish(failure);
            }
        }
    }

    private static UiNetworkObservation cartObservation(AutoExConfiguration configuration, String product,
                                                         String method, UiNetworkCapturePolicy.ResourceKind kind) {
        String path = AutoExRoutes.ADD_TO_CART.path() + product;
        var headers = Map.of(HttpHeader.CONTENT_TYPE.value(), APPLICATION_JSON);
        return new UiNetworkObservation(1, Instant.EPOCH, method, configuration.baseUrl().resolve(path).toString(),
                path, 200, headers, headers, POST.value().equals(method) ? "{}" : "", "{}", "none", "none",
                "controlled fixture", "component", "", true, "", kind);
    }
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
