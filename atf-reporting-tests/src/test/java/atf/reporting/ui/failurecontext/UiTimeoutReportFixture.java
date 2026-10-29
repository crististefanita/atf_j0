package atf.reporting.ui.failurecontext;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.failure.UiFailureEvidencePublisher;
import atf.reporting.ui.model.AtfUiActionRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;

/** Local timeout evidence with distinct primary, causal and cleanup credentials. */
public final class UiTimeoutReportFixture {
    public static final String SCENARIO = "native timeout remains inspectable";
    public static final String TARGET = "button[data-test='login-submit']";
    public static final List<String> SECRETS = List.of("raw-timeout-password", "raw-timeout-token", "raw-cause-token", "raw-cleanup-password");
    private UiTimeoutReportFixture() { }

    public static Path publish(boolean fallback, int limit) {
        Path file = TestReportPaths.report("timeout-" + (fallback ? "fallback-" : "extent-") + limit + "-" + System.nanoTime() + ".html");
        var settings = new ReportingSettings(limit, 18);
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, settings) : new ExtentReportSession(file, settings)) {
            var scenario = session.startScenario("UiTimeout", "atf.reporting.context", SCENARIO, List.of("component", "expected-failure"));
            var phase = scenario.startPhase("TEST BODY");
            var failure = new TimeoutException("Timeout 20000ms exceeded; password=" + SECRETS.get(0)
                    + "; Authorization: Bearer " + SECRETS.get(1));
            failure.initCause(new IllegalStateException("timeout cause; token=" + SECRETS.get(2)));
            failure.addSuppressed(new IllegalArgumentException("timeout cleanup; password=" + SECRETS.get(3)));
            new UiFailureEvidencePublisher(phase, ReportScreenshotPolicy.NONE, ReportUiActionDetailsMode.INCLUDED_COLLAPSED)
                    .publish("login submit did not become visible", TARGET,
                            List.of(new AtfUiActionRecord(1, "Wait for visible", TARGET, "login submit becomes visible",
                                    "Timeout 20000ms exceeded", 137, AtfReportStatus.FAIL)), failure, null, false);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            var sibling = session.startScenario("UiTimeout", "unrelated passing scenario", List.of("component"));
            sibling.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            sibling.complete(AtfReportStatus.PASS);
        }
        return file;
    }
}
