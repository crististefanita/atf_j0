package atf.api.reporting.support.failurecorrelation;

import atf.api.reporting.console.AtfConsoleSink;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.api.reporting.evidence.profile.ReportEvidenceProfile;
import atf.api.reporting.support.ReportTestScreenshots;
import atf.api.reporting.support.TestReportPaths;
import atf.api.reporting.ui.UiExtentReportSession;
import atf.api.reporting.ui.failure.UiCheckpointFailure;
import atf.api.reporting.ui.model.AtfUiActionRecord;
import atf.api.reporting.semantic.AtfReportSemanticSnapshot.RequestSnapshot;
import atf.api.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Path;
import java.util.List;

import static atf.api.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.api.reporting.contract.AtfReportStatus.FAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Neutral, deterministic input: no consumer, credentials or public-site dependency. */
public final class UiFailureCorrelationFixture {
    public static final String OWNER = "UiFailureCorrelationFixture";
    public static final List<String> SCENARIOS = List.of(
            "Direct propagation", "Wrapped propagation", "Independent equal message",
            "Equal assertion different identity", "Foreign scenario assertion",
            "UI and API independent", "Independent suppressed failure", "Multiple UI assertions",
            "Independent terminal action");
    public static final List<Integer> FAILURES = List.of(1, 1, 2, 2, 2, 2, 2, 2, 2);
    public static final int FAILURE_COUNT = FAILURES.stream().mapToInt(Integer::intValue).sum();

    private UiFailureCorrelationFixture() { }

    public static Path publish(ReportEvidenceFlowMode mode) {
        Path requested = TestReportPaths.report("ui-failure-correlation-" + mode.name().toLowerCase(java.util.Locale.ROOT) + ".html");
        var profile = new ReportEvidenceProfile(mode, ReportScreenshotPolicy.EACH_CHECKPOINT,
                ReportEvidenceCaptureSource.CUSTOM_PROVIDER);
        Path report;
        try (var session = UiExtentReportSession.openConfigured(requested.getParent(),
                requested.getFileName().toString(), AtfConsoleSink.noOp(), profile)) {
            report = session.reportPath();
            AtfAssertion foreign = assertion();
            for (int index = 0; index < SCENARIOS.size(); index++) {
                AtfAssertion original = index == 0 ? foreign : assertion();
                List<AtfAssertion> assertions = index == 7
                        ? List.of(original, new AtfAssertion("Visible total", "20", "10", false))
                        : List.of(original);
                Throwable failure = switch (index) {
                    case 1 -> new IllegalStateException("JUnit adapter", new UiCheckpointFailure("checkpoint failed", assertions));
                    case 2 -> new AssertionError("checkpoint failed");
                    case 3 -> new UiCheckpointFailure("checkpoint failed", List.of(assertion()));
                    case 4 -> new UiCheckpointFailure("checkpoint failed", List.of(foreign));
                    default -> new UiCheckpointFailure("checkpoint failed", assertions);
                };
                if (index == 6) failure.addSuppressed(new IllegalStateException("cleanup independently failed"));
                var scenario = session.startScenario(OWNER, "atf.ui.tests.extent.component.audit",
                        SCENARIOS.get(index), List.of("ui", "component", "failure-correlation"));
                var phase = scenario.startPhase("TEST BODY");
                var api = index == 5 ? new UiApiValidationEvidence(
                        List.of(assertion()), List.of())
                        : UiApiValidationEvidence.empty();
                phase.step(THEN, SCENARIOS.get(index), "/checkout", ReportTestScreenshots.ONE_PIXEL_PNG,
                        assertions, List.of(), api, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, FAIL);
                var terminalActions = index == 8 ? List.of(
                        new AtfUiActionRecord(1, "Click", "button[id='cleanup']",
                                "cleanup completes", "independent action failed", 5, FAIL)) : List.<AtfUiActionRecord>of();
                phase.failureEvidence("Terminal diagnostics", "/checkout", terminalActions, failure,
                        ReportTestScreenshots.ONE_PIXEL_PNG);
                phase.complete(FAIL);
                scenario.complete(FAIL);
            }
            for (var scenario : session.semanticSnapshot().classes().getFirst().scenarios()) {
                var terminal = (RequestSnapshot)
                        scenario.phases().getFirst().nodes().stream()
                                .filter(node -> node.title().contains("Terminal diagnostics")).findFirst().orElseThrow();
                var evidence = terminal.validations().getFirst();
                assertEquals(1, evidence.screenshotCount(),
                        "the terminal screenshot must survive correlation");
                assertEquals(FAIL, evidence.status());
                boolean correlated = List.of(SCENARIOS.get(0), SCENARIOS.get(1),
                        SCENARIOS.get(5), SCENARIOS.get(7)).contains(scenario.name());
                assertEquals(correlated ? 0 : 1, evidence.assertionNames().size());
                assertTrue(evidence.uiActionCount() >= (correlated ? 2 : 1));
            }
        }
        return report;
    }

    private static AtfAssertion assertion() {
        return new AtfAssertion("Confirmation visible", "confirmation", "absent", false);
    }
}
