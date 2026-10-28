package atf.ui.tests.extent.support.inspection.readability;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.failure.UiCheckpointFailure;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.contract.AtfReportStatus.FAIL;
import static atf.reporting.contract.AtfReportStatus.PASS;

/** Public UI/API ports publish the actual failure disclosure, without consumer or DOM fixtures. */
public final class NarrowEvidenceFailureFixture {
    private NarrowEvidenceFailureFixture() { }

    public static Path publish() {
        Path requested = TestReportPaths.report("narrow-failure-evidence.html");
        var profile = new ReportEvidenceProfile(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.EACH_CHECKPOINT, ReportEvidenceCaptureSource.CUSTOM_PROVIDER);
        Path report;
        try (var session = UiExtentReportSession.openConfigured(requested.getParent(),
                requested.getFileName().toString(), AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            var scenario = session.startScenario("NarrowEvidenceFailure", "atf.ui.tests.extent.component.readability",
                    "missing independent observation", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            var rq = new ReportRequest("GET", "/observation", Map.of("X-Diagnostic", "neutral audit fixture"),
                    Map.of(), "none", "");
            var execution = new ReportExecutionEvidence(new ReportResponse(200, "{}", Map.of(),
                    Duration.ofMillis(7), "https://fixture.example.test/observation"), 1, Duration.ofMillis(7),
                    List.of(), List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(7), "PASS", "response received")),
                    rq, ReportTransportProvenance.ACCEPTANCE_MOCK);
            var api = new UiApiValidationEvidence(List.of(), List.of(
                    new UiApiValidationEvidence.Exchange(execution, List.of())));
            var comparisons = IntStream.rangeClosed(1, 40).mapToObj(i -> new AtfAssertion(
                    "Independent comparison " + i, "configured value " + i,
                    i == 40 ? "not observed" : "configured value " + i, i != 40)).toList();
            phase.step(THEN, "independent observation matches its configuration", "/observation",
                    ReportTestScreenshots.ONE_PIXEL_PNG, comparisons, List.of(), api,
                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT, FAIL);
            var terminalReadback = IntStream.rangeClosed(1, 40).mapToObj(i -> new AtfUiActionRecord(
                    i, "Terminal observation", "neutral-observation-" + i, "configured value " + i,
                    i == 40 ? "not observed" : "configured value " + i, 0, i == 40 ? FAIL : PASS,
                    AtfUiEvidenceSource.OBSERVE)).toList();
            phase.failureEvidence("Terminal diagnostics", "/observation", terminalReadback,
                    new UiCheckpointFailure("independent observation failed", comparisons),
                    ReportTestScreenshots.ONE_PIXEL_PNG);
            phase.complete(FAIL);
            scenario.complete(FAIL);
        }
        return report;
    }
}