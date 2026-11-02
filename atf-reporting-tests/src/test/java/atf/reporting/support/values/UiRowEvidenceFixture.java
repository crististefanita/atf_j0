package atf.reporting.support.values;

import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.model.*;
import java.nio.file.Path;
import java.util.List;

/** Independent captured payloads: source, whitespace, line endings and final scalar are intentional fixture data. */
public final class UiRowEvidenceFixture {
    public static final String CANARY = "J14_CREDENTIAL_CANARY";
    private UiRowEvidenceFixture() { }
    public static String value(AtfUiEvidenceSource source) {
        return (source == AtfUiEvidenceSource.ACTION ? "\r\n" : "\n") + " \t\n"
                + "state ♠ <value> & ".repeat(30) + "last-scalar\n\t ";
    }
    public static AtfUiActionRecord row(int sequence, AtfUiEvidenceSource source) {
        return new AtfUiActionRecord(sequence, "operation token=" + CANARY, "target token=" + CANARY,
                value(source), value(source), 5, AtfReportStatus.INFO, source);
    }
    public static Path publish(boolean extent) {
        Path file = TestReportPaths.report("row-projection-" + extent + ".html");
        try (AtfReportPorts.Session session = extent ? new ExtentReportSession(file) : new HtmlReportSession(file)) {
            try (var scenario = session.startScenario("UiRows", "captured values", List.of("component"));
                 var phase = scenario.startPhase("TEST BODY"); var request = phase.startRequest("captured checkpoint", "", "")) {
                request.uiActions(List.of(row(7, AtfUiEvidenceSource.ACTION), row(11, AtfUiEvidenceSource.OBSERVE)));
                request.screenshot("checkpoint token=" + CANARY, ReportTestScreenshots.ONE_PIXEL_PNG);
                request.complete(AtfReportStatus.INFO); phase.complete(AtfReportStatus.INFO); scenario.complete(AtfReportStatus.INFO);
            }
        }
        return file;
    }
}
