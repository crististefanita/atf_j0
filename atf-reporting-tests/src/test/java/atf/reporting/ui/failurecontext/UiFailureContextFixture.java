package atf.reporting.ui.failurecontext;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.failure.UiFailureEvidencePublisher;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import java.nio.file.Path;
import java.util.List;

/** Controlled negative report, independent of a remote site's availability. */
public final class UiFailureContextFixture {
    public static final String SECRET = "failure-context-private-credential";
    private UiFailureContextFixture() { }

    public static Path publish(boolean fallback, int limit) {
        Path output = TestReportPaths.report("failure-context-" + (fallback ? "fallback-" : "extent-")
                + limit + "-" + System.nanoTime() + ".html");
        var settings = new ReportingSettings(limit, 18);
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(output, settings)
                : new ExtentReportSession(output, settings)) {
            var scenario = session.startScenario("UiFailureContext", "atf.reporting.context",
                    "expected timeout retains the failed action", List.of("component", "expected-failure"));
            var phase = scenario.startPhase("TEST BODY");
            var failure = new IllegalStateException("technical stack prefix ".repeat(100)
                    + "; token=" + SECRET, new IllegalArgumentException("causal diagnostic; password=" + SECRET));
            failure.addSuppressed(new UnsupportedOperationException("cleanup diagnostic; token=" + SECRET));
            new UiFailureEvidencePublisher(phase, ReportScreenshotPolicy.EACH_CHECKPOINT,
                    ReportUiActionDetailsMode.INCLUDED_COLLAPSED)
                    .publish("search navigation was not confirmed", "#submit_search",
                    List.of(new AtfUiActionRecord(21, "Fill", "#search", "search value entered", "filled", 7, AtfReportStatus.PASS),
                            new AtfUiActionRecord(22, "Click", "#submit_search", "results navigation completes",
                                    failure.getMessage(), 1234, AtfReportStatus.FAIL)), failure, null, false);
            if (!fallback) phase.diagnostic(atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.JUNIT_FAILURE,
                    "IllegalStateException: technical parent phase stack ".repeat(40) + "; token=" + SECRET);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            var sibling = session.startScenario("UiFailureContext", "unrelated passing scenario", List.of("component"));
            sibling.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            sibling.complete(AtfReportStatus.PASS);
        }
        return output;
    }
}
