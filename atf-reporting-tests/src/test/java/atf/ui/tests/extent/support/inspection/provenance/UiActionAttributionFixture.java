package atf.ui.tests.extent.support.inspection.provenance;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Neutral captured facts: context tags and operation names are not driver provenance. */
public final class UiActionAttributionFixture {
    public enum Mode { PASS, DISPATCH_FAIL, OBSERVE_ONLY }
    public static final String TARGET = "button=Continue";
    public static final String CAUSE = "controlled dispatch rejected at second action";
    public static final String LEGITIMATE_AUDIT_TOOL = "Playwright audited HTML offline";

    private UiActionAttributionFixture() { }

    public static Path publish(boolean fallback, String context, Mode mode) throws Exception {
        Path directory = Path.of(System.getProperty("atf.test.reportDirectory", System.getProperty("java.io.tmpdir")));
        Files.createDirectories(directory);
        Path report = Files.createTempDirectory(directory, "ui-attribution-")
                .resolve((fallback ? "fallback-" : "extent-") + context + "-" + mode + ".html");
        AtfReportStatus status = mode == Mode.DISPATCH_FAIL ? AtfReportStatus.FAIL : AtfReportStatus.PASS;
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(report) : new ExtentReportSession(report)) {
            var raw = session.startScenario("NeutralUiAttribution", "atf.fixture", context + " captured checkpoint",
                    List.of("component", "mock", context));
            var scenario = UiExtentReportSession.adaptScenario(raw,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT), "Controlled UI input");
            try (var phase = scenario.startPhase("TEST BODY")) {
                phase.step(AtfGherkinKeyword.THEN, "Inspect captured state", TARGET,
                        ReportTestScreenshots.ONE_PIXEL_PNG,
                        List.of(new AtfAssertion("Captured state", "ready", "ready", true)), actions(mode),
                        ReportScreenshotPolicy.CapturePoint.CHECKPOINT, status);
                phase.complete(status);
            }
            raw.complete(status);
        }
        System.out.println("ATTRIBUTION_REPORT=" + report);
        return report;
    }

    private static List<AtfUiActionRecord> actions(Mode mode) {
        var observation = new AtfUiActionRecord(3, "Inspect", TARGET, "Retain the audit tool name",
                LEGITIMATE_AUDIT_TOOL, 0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE);
        if (mode == Mode.OBSERVE_ONLY) return List.of(observation);
        return List.of(new AtfUiActionRecord(1, "Activate", "button=Start", "Dispatch accepted", "accepted", 7,
                        AtfReportStatus.PASS),
                new AtfUiActionRecord(2, "Activate", TARGET, "Dispatch accepted",
                        mode == Mode.DISPATCH_FAIL ? CAUSE : "accepted", 11,
                        mode == Mode.DISPATCH_FAIL ? AtfReportStatus.FAIL : AtfReportStatus.PASS), observation);
    }
}
