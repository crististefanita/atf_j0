package atf.autoex.tests.support.reporting;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import atf.core.http.request.HttpHeader;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.ui.network.UiNetworkCapturePolicy;
import atf.ui.network.UiNetworkObservation;
import atf.ui.playwright.PlaywrightUiSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static atf.core.http.request.HttpMethod.GET;
import static atf.core.http.request.HttpMethod.POST;
import static atf.core.http.request.MediaTypes.APPLICATION_JSON;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.FETCH;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.XHR;

/** Produces a controlled Extent artifact for component and local browser presentation checks. */
public final class AutoExCatalogApiReportFixture {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    public enum CaptureCase { COMPLETE, MISSING_CATALOG_GET, AMBIGUOUS_CATALOG_GET }

    private AutoExCatalogApiReportFixture() { }

    public static Path create(Path reportRoot) throws Exception {
        return create(reportRoot, CaptureCase.COMPLETE);
    }

    public static Path create(Path reportRoot, CaptureCase captureCase) throws Exception {
        AutoExConfiguration source = AutoExE2eRun.withNetworkInspection(AutoExConfigurationLoader.load());
        UiBrowserConfiguration browserConfiguration = browserConfiguration(source);
        var network = browserConfiguration.networkInspection()
                .withCapturePolicy(UiNetworkCapturePolicy.asynchronous().includingOtherRequests())
                .withSettlementPolicy(UiNetworkSettlementPolicy.bounded(Duration.ofMillis(100), Duration.ofMillis(5)));
        browserConfiguration = new UiBrowserConfiguration(browserConfiguration.baseUrl(),
                browserConfiguration.browser(), true, browserConfiguration.timeout(),
                browserConfiguration.navigationTimeout(), browserConfiguration.viewportWidth(),
                browserConfiguration.viewportHeight(), browserConfiguration.zoomPercent(),
                browserConfiguration.screenshotOnFailure(), browserConfiguration.retryPolicy(), network);
        AutoExConfiguration configuration = new AutoExConfiguration(source.baseUrl(), false, browserConfiguration,
                source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(),
                source.storeAvailabilityPolicy(), source.evidence());
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.NONE);
        Path output = reportRoot.toAbsolutePath().resolve(UUID.randomUUID().toString());
        Files.createDirectories(output);
        Path reportPath;
        try (var reports = AutoExReportSession.open(output,
                AutoExReportSession.fileName("catalog-api"), AtfConsoleSink.noOp(),
                profile, browserConfiguration)) {
            reportPath = reports.report().reportPath();
            var scenario = reports.report().startScenario("CatalogApiCorrelation",
                    AutoExCatalogApiReportFixture.class.getPackageName(),
                    "one selected catalog response owns the business comparison",
                    List.of("component", "controlled-network"));
            var browser = PlaywrightUiSession.open(browserConfiguration, AtfConsoleSink.noOp());
            var run = new AutoExE2eRun(configuration, browser, scenario, scenario.startPhase("TEST BODY"), profile,
                    true, AutoExRunLifecycleAdapter.INSTANCE);
            Throwable failure = null;
            try {
                var inspector = browser.networkInspector();
                String path = AutoExRoutes.PRODUCTS_API.path();
                capture(inspector, observation(configuration, path, POST.value(), XHR));
                if (captureCase != CaptureCase.MISSING_CATALOG_GET) {
                    UiNetworkObservation selected = observation(configuration, path, GET.value(), XHR);
                    capture(inspector, selected);
                    if (captureCase == CaptureCase.AMBIGUOUS_CATALOG_GET) {
                        capture(inspector, selected);
                    } else {
                        capture(inspector, observation(configuration, path, GET.value(), FETCH));
                    }
                }
                capture(inspector, observation(configuration, "/api/unrelated", GET.value(), XHR));
                try {
                    run.apiCheckpoint(AutoExPurchaseTraceStep.CATALOG_API_PRECONDITION,
                            List.of(new AtfAssertion("Catalog result count", "one selected response",
                                    "one selected response", true)));
                    if (captureCase != CaptureCase.COMPLETE) {
                        throw new IllegalStateException("The API checkpoint passed without unique catalog evidence");
                    }
                } catch (AssertionError expectedFailure) {
                    if (captureCase == CaptureCase.COMPLETE) throw expectedFailure;
                    failure = expectedFailure;
                }
            } catch (Exception | Error unexpected) {
                failure = unexpected;
                throw unexpected;
            } finally {
                run.finish(failure);
            }
        }
        return reportPath;
    }

    public static UiBrowserConfiguration browserConfiguration() {
        return browserConfiguration(AutoExConfigurationLoader.load());
    }

    private static UiBrowserConfiguration browserConfiguration(AutoExConfiguration source) {
        UiBrowserConfiguration base = source.browser();
        return new UiBrowserConfiguration(base.baseUrl(), base.browser(), true, base.timeout(),
                base.navigationTimeout(), base.viewportWidth(), base.viewportHeight(), base.zoomPercent(),
                base.screenshotOnFailure(), base.retryPolicy(), base.networkInspection());
    }

    private static UiNetworkObservation observation(AutoExConfiguration configuration, String path,
                                                   String method, UiNetworkCapturePolicy.ResourceKind kind) {
        var headers = Map.of(HttpHeader.CONTENT_TYPE.value(), APPLICATION_JSON);
        return new UiNetworkObservation(SEQUENCE.incrementAndGet(), Instant.EPOCH, method,
                configuration.baseUrl().resolve(path).toString(), path, 200, headers, headers,
                POST.value().equals(method) ? "{}" : "", "{\"products\":[]}", "none", "none",
                "controlled fixture", "component", "", true, "", kind);
    }

    private static void capture(atf.ui.network.UiNetworkInspector inspector, UiNetworkObservation observation) {
        if (inspector.capture(observation).isEmpty()) {
            throw new IllegalStateException("The controlled catalog observation was rejected");
        }
    }
}
