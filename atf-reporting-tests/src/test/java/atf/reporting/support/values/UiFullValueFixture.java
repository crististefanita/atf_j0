package atf.reporting.support.values;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.*;
import java.nio.file.Path;
import java.util.List;

/** Neutral captured values; caller-owned suffixes distinguish otherwise identical previews. */
public final class UiFullValueFixture {
    public static final String PREFIX = "State ♠ −7 <safe> & emoji 😀\n" + "captured-value-\n".repeat(30);
    public static final String SECRET = "j13-captured-credential";
    private UiFullValueFixture() { }
    public static String value(boolean expected, int owner) {
        return PREFIX + " token=***REDACTED***; " + (expected ? " expected-last-" : " observed-last-") + owner;
    }
    public static Path publish(boolean extent) {
        Path path = TestReportPaths.report("full-ui-values-" + extent + ".html");
        try (AtfReportPorts.Session session = extent ? new ExtentReportSession(path, ReportingSettings.defaults())
                : new HtmlReportSession(path, ReportingSettings.defaults())) {
            for (int index = 1; index <= 2; index++) {
                var scenario = UiExtentReportSession.adaptScenario(session.startScenario("UiFullValues" + index,
                        "value owner " + index, List.of("component")), ReportEvidenceProfile.defaults(), "value owner " + index);
                try (scenario; var phase = scenario.startPhase("TEST BODY")) {
                    phase.uiValidation("captured values", "target", ReportTestScreenshots.ONE_PIXEL_PNG, List.of(), List.of(
                            new AtfUiActionRecord(1, "ACTION", "target", value(true, index).replace("***REDACTED***", SECRET), value(false, index).replace("***REDACTED***", SECRET), 1, AtfReportStatus.FAIL),
                            new AtfUiActionRecord(2, "OBSERVE", "target", value(true, index), value(false, index), 0,
                                    AtfReportStatus.INFO, AtfUiEvidenceSource.OBSERVE)),
                            ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.FAIL);
                    phase.completeFinal(AtfReportStatus.FAIL); scenario.complete(AtfReportStatus.FAIL);
                }
            }
        }
        return path;
    }
}
