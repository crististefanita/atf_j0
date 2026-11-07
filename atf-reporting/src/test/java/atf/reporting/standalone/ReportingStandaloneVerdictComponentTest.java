package atf.reporting.standalone;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.ui.failure.UiFailureEvidencePublisher;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.params.provider.ValueSource;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.semantic.AtfReportSemanticRecorder;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static atf.reporting.contract.AtfReportStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the complete reporting component without another ATF module or browser. */
@Tag("component")
class ReportingStandaloneVerdictComponentTest {
    @TempDir Path directory;

    enum Renderer {
        EXTENT, FALLBACK;
        AtfReportPorts.Session open(Path file) {
            return this == EXTENT ? new ExtentReportSession(file) : new HtmlReportSession(file);
        }
        AtfReportSemanticSnapshot snapshot(AtfReportPorts.Session session) {
            return this == EXTENT ? ((ExtentReportSession) session).semanticSnapshot()
                    : ((HtmlReportSession) session).semanticSnapshot();
        }
    }

    @ParameterizedTest
    @EnumSource(Renderer.class)
    void failedRowsDominateCallerPassSkipAndMissingStatusThroughNestedScopes(Renderer renderer) {
        try (var session = renderer.open(directory.resolve(renderer + "-contradiction.html"))) {
            int index = 0;
            for (var requested : Arrays.asList(PASS, SKIP, null)) {
                var scenario = session.startScenario("Verdict", "negative-" + index++, List.of("component"));
                try (var phase = scenario.startPhase("TEST BODY");
                     var request = phase.startRequest("checkpoint", "TRACE", "local");
                     var validation = request.startValidation("UI validation");
                     var nested = validation.startRequest("nested exchange", "GET", "/fixture");
                     var leaf = nested.startValidation("API validation")) {
                    validation.assertions(List.of(new AtfAssertion("Skipped parent policy", "ready", "aborted", false, SKIP)));
                    leaf.assertions(List.of(new AtfAssertion("Independent comparison", "expected", "different", false)));
                    leaf.complete(requested);
                    nested.complete(requested);
                    validation.complete(requested);
                    request.complete(requested);
                    phase.complete(requested);
                }
                scenario.complete(requested);
            }
            publish(session, "positive sibling", new AtfAssertion("Expected rejection", "IOException", caughtException(), true), PASS);
            var scenarios = renderer.snapshot(session).classes().getFirst().scenarios();
            assertEquals(List.of(FAIL, FAIL, FAIL, PASS), scenarios.stream().map(AtfReportSemanticSnapshot.ScenarioSnapshot::status).toList());
            for (var scenario : scenarios.subList(0, 3)) {
                var phase = scenario.phases().getFirst();
                assertEquals(FAIL, phase.status());
                var request = assertInstanceOf(AtfReportSemanticSnapshot.RequestSnapshot.class, phase.nodes().getFirst());
                assertEquals(FAIL, request.status());
                var validation = request.validations().getFirst();
                assertEquals(FAIL, validation.status());
                assertEquals(FAIL, validation.requests().getFirst().status());
                assertEquals(FAIL, validation.requests().getFirst().validations().getFirst().status());
            }
            if (session instanceof HtmlReportSession fallback) {
                assertEquals(3, fallback.executionModel().failures().items().size(),
                        "A parent SKIP comparison is not the cause of its failed nested request");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Renderer.class)
    void effectiveRowPolicyPreservesSkipInfoWarningAndExpectedExceptionSuccess(Renderer renderer) {
        try (var session = renderer.open(directory.resolve(renderer + "-policy.html"))) {
            publish(session, "skip", new AtfAssertion("Aborted comparison", "ready", "not available", false, SKIP), PASS);
            publish(session, "observation", new AtfAssertion("Optional observation", "captured", "captured", true, INFO), INFO);
            publish(session, "warning", new AtfAssertion("Advisory", "preferred", "accepted", true, WARNING), PASS);
            publish(session, "expected exception", new AtfAssertion("Expected rejection", "IOException", caughtException(), true), PASS);
            var statuses = renderer.snapshot(session).classes().getFirst().scenarios().stream()
                    .map(AtfReportSemanticSnapshot.ScenarioSnapshot::status).toList();
            assertEquals(List.of(SKIP, INFO, WARNING, PASS), statuses);
        }
    }

    @ParameterizedTest
    @EnumSource(Renderer.class)
    void componentAuditRespectsAnExplicitSkipInsteadOfRecomputingRawComparison(Renderer renderer) {
        var session = renderer.open(directory.resolve(renderer + "-component.html"));
        var identity = new AssertionAuditReport.Identity("Component", "standalone", "abort", "comparison", List.of("component"));
        try (var report = new AssertionAuditReport(session, identity, new SensitiveDataRedactor())) {
            report.phase("TEST BODY", List.of(new AtfAssertion("Unavailable fixture", "ready", "aborted", false, SKIP)), SKIP, "");
            report.complete(SKIP);
            var scenario = renderer.snapshot(session).classes().getFirst().scenarios().getFirst();
            assertEquals(SKIP, scenario.status());
            var step = assertInstanceOf(AtfReportSemanticSnapshot.AssertionStepSnapshot.class,
                    scenario.phases().getFirst().nodes().getFirst());
            assertEquals(SKIP, step.status());
        }
    }

    @Test
    void rejectsAnExplicitPassForAFailedComparisonWithoutForbiddingLegitimateSkip() {
        assertThrows(IllegalArgumentException.class,
                () -> new AtfAssertion("comparison", "expected", "different", false, PASS));
        assertDoesNotThrow(() -> new AtfAssertion("comparison", "expected", "unavailable", false, SKIP));
        assertEquals(FAIL, new AtfAssertion("policy rejection", "allowed", "rejected", true, FAIL).resultStatus());
    }

    @Test
    void semanticRecorderUsesTheSameEvidencePolicyWithoutKeepingRawValues() {
        var recorder = new AtfReportSemanticRecorder();
        var scenario = recorder.startScenario("Semantic", "standalone", "contradiction", List.of("component"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("checkpoint", "GET", "/fixture");
        var validation = request.startValidation("API validation");
        validation.assertions(List.of(new AtfAssertion("independent comparison", "expected", "different", false)));
        validation.complete(PASS); request.complete(PASS); phase.complete(PASS); scenario.complete(PASS);
        var recorded = recorder.snapshot().classes().getFirst().scenarios().getFirst();
        assertEquals(FAIL, recorded.status());
        assertEquals(FAIL, recorded.phases().getFirst().status());
        var node = assertInstanceOf(AtfReportSemanticSnapshot.RequestSnapshot.class, recorded.phases().getFirst().nodes().getFirst());
        assertEquals(FAIL, node.status());
        assertEquals(FAIL, node.validations().getFirst().status());
    }

    @ParameterizedTest
    @EnumSource(Renderer.class)
    void replacingUnpublishedRowsKeepsTheRenderedAndSemanticPopulationIdentical(Renderer renderer) {
        try (var session = renderer.open(directory.resolve(renderer + "-replacement.html"))) {
            var scenario = session.startScenario("Verdict", "builder replacement", List.of("component"));
            try (var phase = scenario.startPhase("TEST BODY");
                 var request = phase.startRequest("checkpoint", "GET", "/fixture");
                 var validation = request.startValidation("API validation")) {
                validation.assertions(List.of(new AtfAssertion("draft", "expected", "different", false)));
                validation.assertions(List.of(new AtfAssertion("final", "expected", "expected", true)));
                validation.complete(PASS); request.complete(PASS); phase.complete(PASS);
            }
            scenario.complete(PASS);
            var snapshot = renderer.snapshot(session).classes().getFirst().scenarios().getFirst();
            assertEquals(PASS, snapshot.status());
            var request = assertInstanceOf(AtfReportSemanticSnapshot.RequestSnapshot.class, snapshot.phases().getFirst().nodes().getFirst());
            assertEquals(List.of("final"), request.validations().getFirst().assertionNames());
        }
    }

    private static void publish(AtfReportPorts.Session session, String name, AtfAssertion row, AtfReportStatus status) {
        var scenario = session.startScenario("Verdict", name, List.of("component"));
        try (var phase = scenario.startPhase("TEST BODY");
             var request = phase.startRequest("checkpoint", "TRACE", "local");
             var validation = request.startValidation("UI validation")) {
            validation.assertions(List.of(row)); validation.complete(status);
            request.complete(status); phase.complete(status);
        }
        scenario.complete(status);
    }

    private static String caughtException() {
        return assertThrows(IOException.class, () -> { throw new IOException("expected fixture rejection"); })
                .getClass().getSimpleName();
    }


    @ParameterizedTest
    @ValueSource(strings = {"", "IO_ERROR", "TIMEOUT", "CANCELLED"})
    void absentResponseRequiresAnExplicitVerdictEvenWithOnlyInformationalAssertions(String outcome) {
        var evidence = terminal(outcome);
        for (var assertions : List.of(List.<AtfAssertion>of(),
                List.of(new AtfAssertion("Observation", "unknown", "unknown", true, INFO)))) {
            for (var status : java.util.Arrays.asList(null, INFO)) {
                var failure = assertThrows(IllegalArgumentException.class,
                        () -> new UiApiValidationEvidence.Exchange(evidence, assertions, status));
                assertTrue(failure.getMessage().contains("explicit verdict"));
            }
        }
        assertTrue(UiApiValidationEvidence.empty().isEmpty());
        assertEquals(INFO, UiApiValidationEvidence.empty().status());
    }

    @ParameterizedTest
    @EnumSource(value = AtfReportStatus.class, names = {"PASS", "FAIL", "SKIP", "WARNING"})
    void callerStatusIsPreservedWithoutInterpretingTheTerminalOutcome(AtfReportStatus expected) {
        var evidence = terminal("IO_ERROR");
        var exchange = new UiApiValidationEvidence.Exchange(evidence, List.of(), expected);
        assertEquals(expected, exchange.status());
        assertSame(evidence, exchange.evidence());
        assertNull(exchange.evidence().response());
    }

    @ParameterizedTest
    @EnumSource(value = AtfReportStatus.class, names = {"PASS", "FAIL", "SKIP", "WARNING"})
    void evaluatedAssertionsSupplyTheVerdictAndNeverLeakToSiblings(AtfReportStatus expected) {
        var row = new AtfAssertion("Caller policy", "expected", "observed", expected != FAIL, expected);
        var exchange = new UiApiValidationEvidence.Exchange(terminal("TIMEOUT"), List.of(row));
        var sibling = new UiApiValidationEvidence.Exchange(captured(200), List.of());
        assertEquals(expected, exchange.status());
        assertEquals(PASS, sibling.status());
        assertEquals(expected, new UiApiValidationEvidence(List.of(), List.of(sibling, exchange)).status());
    }

    @Test
    void failedAndSkippedAssertionsCannotBeOverwrittenByAnExplicitPass() {
        for (var status : List.of(FAIL, SKIP)) {
            var row = new AtfAssertion("Caller policy", "ready", "unavailable", false, status);
            assertEquals(status, new UiApiValidationEvidence.Exchange(terminal("CANCELLED"), List.of(row), PASS).status());
        }
    }

    @Test
    void rejectsAnUnpublishableExchangeBeforeItCanDisappearFromTheReport() {
        var evidence = new ReportExecutionEvidence(null, 0, Duration.ZERO, List.of(), List.of(),
                null, ReportTransportProvenance.UNKNOWN);
        var failure = assertThrows(NullPointerException.class,
                () -> new UiApiValidationEvidence.Exchange(evidence, List.of(), FAIL));
        assertEquals("captured request", failure.getMessage());
    }

    @Test
    void capturedResponseKeepsCompatibilityWithoutInterpretingHttpBusinessPolicy() {
        assertEquals(PASS, new UiApiValidationEvidence.Exchange(captured(503), List.of()).status());
        assertEquals(WARNING, new UiApiValidationEvidence.Exchange(captured(-1), List.of()).status());
    }

    @Test void terminalDiagnosticAndIndependentComparisonWithTheSameLabelKeepSeparateRoles() throws Exception {
        Path report = directory.resolve("terminal-action.html");
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("TerminalFixture", "Checkout", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            var comparison = phase.startRequest("Business checkpoint", "", "");
            var validation = comparison.startValidation("Independent comparison");
            validation.assertions(List.of(new AtfAssertion("Failure cause", "confirmed", "pending", false)));
            validation.complete(FAIL);
            comparison.complete(FAIL);
            publish(phase, ReportUiActionDetailsMode.INCLUDED_COLLAPSED, List.of(failedUiRow(AtfUiEvidenceSource.ACTION)));
            phase.complete(FAIL);
            scenario.complete(FAIL);
        }
        var html = Jsoup.parse(report);
        var failures = html.select("#fails a.fail-row");
        assertEquals(List.of("Failure cause", "UI action · Inspect #1 → #submit"), failures.select("strong").eachText());
        assertEquals(List.of("1", "2"), failures.eachAttr("data-atf-failure-ordinal"));
        assertEquals(2, html.select("#tests table.assertions tbody tr").size());
        assertEquals("Info", html.select("#tests table.assertions tbody tr").last().select("td").last().text());
    }

    @Test void hiddenActionAndAnUnrecordedExceptionRetainANavigableCause() throws Exception {
        verifySingleFailure("hidden-action", List.of(failedUiRow(AtfUiEvidenceSource.ACTION)), "Failure cause");
        verifySingleFailure("exception-only", List.of(), "Failure cause");
        verifySingleFailure("visible-observation", List.of(failedUiRow(AtfUiEvidenceSource.OBSERVE)), "Audit observation · Inspect #1 → #submit");
    }

    @Test void aVisibleIndependentObservationDoesNotReplaceTheHiddenTerminalAction() throws Exception {
        var observation = new AtfUiActionRecord(2, "Inspect result", "#outcome", "confirmed", "pending",
                9, FAIL, AtfUiEvidenceSource.OBSERVE);
        var html = renderSummary("mixed-sources", List.of(failedUiRow(AtfUiEvidenceSource.ACTION), observation));
        var failures = html.select("#fails a.fail-row");
        assertEquals(List.of("Audit observation · Inspect result #2 → #outcome", "Failure cause"), failures.select("strong").eachText());
        assertEquals(List.of("pending", "IllegalStateException; first failed evidence: Inspect #1 -> #submit; elapsed=7 ms; expected=checkout visible"),
                failures.select("span:last-child").eachText());
        assertEquals(List.of("1", "2"), failures.eachAttr("data-atf-failure-ordinal"));
    }

    private void verifySingleFailure(String name, List<AtfUiActionRecord> rows, String label) throws Exception {
        var html = renderSummary(name, rows);
        var failures = html.select("#fails a.fail-row");
        assertEquals(List.of(label), failures.select("strong").eachText(), name);
        assertEquals(List.of("1"), failures.eachAttr("data-atf-failure-ordinal"));
        assertTrue(html.select("#tests .atf-ui-action-table:not(.atf-ui-observation-table)").isEmpty());
        assertTrue(html.select("#tests table.assertions").text().contains("IllegalStateException"));
    }

    private Document renderSummary(String name, List<AtfUiActionRecord> rows) throws Exception {
        Path report = directory.resolve(name + ".html");
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("TerminalFixture", name, List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            publish(phase, ReportUiActionDetailsMode.SUMMARY_ONLY, rows);
            phase.complete(FAIL);
            scenario.complete(FAIL);
        }
        return Jsoup.parse(report);
    }

    private static void publish(AtfReportPorts.Phase phase,
                                ReportUiActionDetailsMode mode, List<AtfUiActionRecord> rows) {
        new UiFailureEvidencePublisher(phase, ReportScreenshotPolicy.NONE, mode)
                .publish("Checkout did not complete", "#submit", rows,
                        new IllegalStateException("terminal diagnostic"), null, false);
    }

    private static AtfUiActionRecord failedUiRow(AtfUiEvidenceSource source) {
        return new AtfUiActionRecord(1, "Inspect", "#submit", "checkout visible", "missing",
                7, FAIL, source);
    }

    private static ReportExecutionEvidence terminal(String outcome) {
        var evidence = new ReportExecutionEvidence(null, 1, Duration.ofMillis(7), List.of(), List.of(),
                ReportRequest.builder("GET", "/terminal").build(), ReportTransportProvenance.LIVE_HTTP);
        return outcome.isEmpty() ? evidence : evidence.withTermination(outcome, "Controlled terminal diagnostic");
    }

    private static ReportExecutionEvidence captured(int status) {
        return new ReportExecutionEvidence(new ReportResponse(status, "", Map.of(), Duration.ZERO, ""),
                1, Duration.ZERO, List.of(), List.of(), ReportRequest.builder("GET", "/sibling").build(),
                ReportTransportProvenance.ACCEPTANCE_MOCK);
    }
}
