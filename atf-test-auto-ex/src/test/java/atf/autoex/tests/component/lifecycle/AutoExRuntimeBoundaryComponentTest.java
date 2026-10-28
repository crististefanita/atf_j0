package atf.autoex.tests.component.lifecycle;

import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.autoex.tests.extension.AutoExComponentTest;
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
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.support.validation.AutoExStoreAssertions;
import atf.autoex.page.ProductPage;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.core.http.request.HttpMethod.GET;
import static atf.core.http.request.HttpMethod.POST;
import static atf.core.http.request.MediaTypes.APPLICATION_JSON;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.XHR;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.FETCH;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.DOCUMENT;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

/** Local DOM only: exercises the main runtime without a JUnit context or remote application. */
@AutoExComponentTest
class AutoExRuntimeBoundaryComponentTest {
    private static final AtomicLong OBSERVATION_SEQUENCE = new AtomicLong();
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
                assertFalse(run.awaitCartExchange("1").responseObserved());
                var completed = inspector.capture(cartObservation(configuration, "1?quantity=2", GET.value(), XHR))
                        .orElseThrow();
                assertTrue(completed.url().endsWith("?redacted-query"));
                var mutation = run.awaitCartExchange("1");
                assertTrue(mutation.successfulResponse(), mutation.toString());
                assertEquals(200, mutation.status());
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
        return cartObservation(configuration, product, method, kind, 200, "");
    }

    private static UiNetworkObservation cartObservation(AutoExConfiguration configuration, String product,
                                                         String method, UiNetworkCapturePolicy.ResourceKind kind,
                                                         int status, String failureReason) {
        String path = AutoExRoutes.ADD_TO_CART.path() + product;
        var headers = Map.of(HttpHeader.CONTENT_TYPE.value(), APPLICATION_JSON);
        return new UiNetworkObservation(OBSERVATION_SEQUENCE.incrementAndGet(), Instant.EPOCH, method,
                configuration.baseUrl().resolve(path).toString(),
                path, status, headers, status < 0 ? Map.of() : headers,
                POST.value().equals(method) ? "{}" : "", status < 0 ? "" : "{}", "none", "none",
                "controlled fixture", "component", "", status >= 0, failureReason, kind);
    }

    @Test
    void cartMutationAndModalOutcomesStayTogetherForSuccessHttpFailureRequestFailureAndMissingResponse()
            throws Exception {
        var source = AutoExE2eRun.withNetworkInspection(AutoExConfigurationLoader.load());
        var base = source.browser();
        var network = base.networkInspection()
                .withCapturePolicy(UiNetworkCapturePolicy.asynchronous().includingDocuments())
                .withSettlementPolicy(UiNetworkSettlementPolicy.bounded(Duration.ofMillis(80), Duration.ofMillis(5)));
        var browserConfiguration = new UiBrowserConfiguration(base.baseUrl(), base.browser(), true,
                base.timeout(), base.navigationTimeout(), base.viewportWidth(), base.viewportHeight(), base.zoomPercent(),
                base.screenshotOnFailure(), base.retryPolicy(), network);
        var configuration = new AutoExConfiguration(source.baseUrl(), false, browserConfiguration,
                source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(),
                source.storeAvailabilityPolicy(), source.evidence());
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.NONE);
        Path output = Path.of(System.getProperty("atf.test.reportDirectory", "target/autoex-runtime-reports"))
                .toAbsolutePath().resolve("ui");
        Files.createDirectories(output);
        Path reportPath;
        try (var reports = AutoExReportSession.open(output,
                AutoExReportSession.fileName("cart-mutation-correlation"), AtfConsoleSink.noOp(),
                profile, browserConfiguration)) {
            reportPath = reports.report().reportPath();
            var scenario = reports.report().startScenario("CartMutationCorrelation",
                    getClass().getPackageName(), "one observed add action owns its response and modal result",
                    List.of("component", "local-dom", "controlled-network"));
            var browser = PlaywrightUiSession.open(browserConfiguration, AtfConsoleSink.noOp());
            var run = new AutoExE2eRun(configuration, browser, scenario, scenario.startPhase("TEST BODY"),
                    profile, true, AutoExRunLifecycleAdapter.INSTANCE);
            try {
                var cases = List.of(
                        new CartMutationCase("HTTP 201", 201, "", true),
                        new CartMutationCase("HTTP 503", 503, "", false),
                        new CartMutationCase("request failed", -1, "net::ERR_CONNECTION_RESET", false),
                        new CartMutationCase("response missing", null, "", false));
                for (CartMutationCase testCase : cases) {
                    browser.page().setContent(cartMutationDom(testCase.successful()));
                    var product = new ProductPage(run);
                    product.addQuantityToCart(1);
                    captureCartNeighbors(browser, configuration);
                    if (testCase.status() != null) {
                        var kind = testCase.status() < 0 ? FETCH : XHR;
                        browser.networkInspector().capture(cartObservation(configuration, "1", GET.value(), kind,
                                testCase.status(), testCase.reason()));
                    }

                    var mutation = run.awaitCartExchange("1");
                    if (testCase.status() == null) {
                        assertFalse(mutation.responseObserved(), testCase.name());
                    } else {
                        assertTrue(mutation.responseObserved(), testCase.name());
                        assertEquals(testCase.status(), mutation.status(), testCase.name());
                    }
                    var confirmation = mutation.successfulResponse()
                            ? product.addConfirmationAfterResponse() : product.addConfirmationIfVisible();
                    var uiCheck = AutoExStoreAssertions.cartMutationConfirmation(
                            mutation.successfulResponse(), confirmation);
                    var step = new LocalStep(AtfGherkinKeyword.WHEN,
                            "cart add correlation — " + testCase.name());
                    if (testCase.successful()) {
                        run.cartAddCheckpoint(step, mutation, List.of(uiCheck));
                    } else {
                        assertThrows(AssertionError.class,
                                () -> run.cartAddCheckpoint(step, mutation, List.of(uiCheck)), testCase.name());
                    }
                }
                run.finish(null);
            } catch (Exception | Error failure) {
                run.finish(failure);
                throw failure;
            }
        }
        String html = Files.readString(reportPath).replaceAll(
                "(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertTrue(html.contains("Cart add response"));
        assertTrue(html.contains("HTTP 201"));
        assertTrue(html.contains("HTTP 503"));
        assertTrue(html.contains("net::ERR_CONNECTION_RESET"));
        assertTrue(html.contains("no settled matching GET XHR/FETCH"));
        assertTrue(html.contains("Add-to-cart confirmation"));
        assertTrue(html.contains("Click"));
        assertCheckpointStatus(html, "WHEN cart add correlation — HTTP 503", null, "fail-bg");
        assertCheckpointStatus(html, "WHEN cart add correlation — HTTP 503", "UI validation", "pass-bg");
        assertCheckpointStatus(html, "WHEN cart add correlation — HTTP 503", "API validation", "fail-bg");
    }

    private static void assertCheckpointStatus(String html, String checkpoint, String validation,
                                              String expectedStatusClass) {
        int checkpointStart = html.indexOf("<span>" + checkpoint + "</span>");
        assertTrue(checkpointStart >= 0, "missing checkpoint: " + checkpoint);
        int titleStart = validation == null ? checkpointStart
                : html.indexOf("<span>" + validation + "</span>", checkpointStart);
        assertTrue(titleStart >= 0, "missing " + validation + " in " + checkpoint);
        int toolbarStart = html.lastIndexOf("<div class=\"card-toolbar node-info\">", titleStart);
        assertTrue(toolbarStart >= 0 && (validation == null || toolbarStart >= checkpointStart),
                "missing status toolbar for "
                + (validation == null ? checkpoint : validation));
        int statusStart = html.indexOf("<span class=\"badge ", toolbarStart);
        int statusEnd = html.indexOf("</span>", statusStart);
        assertTrue(statusStart >= 0 && statusEnd > statusStart,
                "missing rendered status for " + (validation == null ? checkpoint : validation));
        assertTrue(html.substring(statusStart, statusEnd).contains(expectedStatusClass),
                "unexpected status for " + (validation == null ? checkpoint : validation));
    }

    private static String cartMutationDom(boolean successful) {
        String modalStyle = successful ? "display:block" : "display:none";
        String clickHandler = successful
                ? "document.getElementById('cartModal').style.display='block'" : "void 0";
        return "<input id='quantity' value='1'><button type='button' class='cart' onclick=\""
                + clickHandler + "\">Add</button><div id='cartModal' style='" + modalStyle
                + "'>Added! Product added to cart</div>";
    }

    private static void captureCartNeighbors(PlaywrightUiSession browser, AutoExConfiguration configuration) {
        var inspector = browser.networkInspector();
        inspector.capture(cartObservation(configuration, "10", GET.value(), XHR));
        inspector.capture(cartObservation(configuration, "1", POST.value(), XHR));
        inspector.capture(cartObservation(configuration, "1", GET.value(), DOCUMENT));
    }

    private record CartMutationCase(String name, Integer status, String reason, boolean successful) { }
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
