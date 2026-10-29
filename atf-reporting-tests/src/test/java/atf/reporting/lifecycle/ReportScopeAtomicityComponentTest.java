package atf.reporting.lifecycle;
import atf.reporting.model.ReportExecutionEvidence;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.*;
import atf.reporting.contract.*;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.TestReportPaths;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static atf.reporting.contract.AtfReportStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/** Public port regressions: an invalid transition must not poison the next publication. */
@AtfComponentTest
class ReportScopeAtomicityComponentTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void invalidActionCollectionsAreRejectedBeforeEitherPopulationChanges(boolean fallback) throws Exception {
        try (var report = open(fallback, AtfConsoleSink.noOp())) {
            var scenario = report.session.startScenario("Atomicity", "invalid input", List.of());
            var phase = scenario.startPhase("TEST BODY"); var request = phase.startRequest("request", "", "");
            var action = new AtfUiActionRecord(1, "Fill", "#input", "ready", "ready", 0, PASS);
            var invalid = Arrays.asList(action, null); var before = report.snapshot();
            assertThrows(NullPointerException.class, () -> request.uiActions(invalid)); assertEquals(before, report.snapshot());
            var validation = request.startValidation("validation"); before = report.snapshot();
            assertThrows(NullPointerException.class, () -> validation.uiActions(invalid)); assertEquals(before, report.snapshot());
            validation.complete(PASS); request.complete(PASS); phase.complete(PASS); scenario.complete(PASS);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void incompatibleInlineCaptureIsRejectedBeforeMutatingARecoverableRequest(boolean fallback) throws Exception {
        try (var report = open(fallback, AtfConsoleSink.noOp())) {
            var scenario = report.session.startScenario("Atomicity", "capture policy", List.of());
            var phase = scenario.startPhase("TEST BODY"); var request = phase.startRequest("one inline capture", "", "");
            var action = new AtfUiActionRecord(1, "Fill", "#input", "ready", "ready", 0, PASS);
            request.uiActions(List.of(action)); request.screenshot("first", ReportTestScreenshots.ONE_PIXEL_PNG);
            report.rejects(() -> request.screenshot("second", ReportTestScreenshots.SECOND_PIXEL_PNG));
            request.complete(PASS);
            var gallery = phase.startRequest("gallery without action ownership", "", "");
            gallery.screenshot("first", ReportTestScreenshots.ONE_PIXEL_PNG); gallery.screenshot("second", ReportTestScreenshots.SECOND_PIXEL_PNG);
            report.rejects(() -> gallery.uiActions(List.of(action))); gallery.complete(PASS);
            phase.complete(PASS); scenario.complete(PASS);
        }
    }

    @org.junit.jupiter.api.Test
    void fallbackPublicationFailureRetainsItsOriginalCauseAndCannotFabricateAnArtifact() throws Exception {
        Path file = TestReportPaths.report("atomicity-failed-publication-" + UUID.randomUUID() + ".html");
        var primary = new IllegalStateException("navigation asset unavailable");
        var session = new HtmlReportSession(file, ReportingSettings.defaults(), AtfConsoleSink.noOp(), () -> { throw primary; });
        var scenario = session.startScenario("Atomicity", "publication failure", List.of());
        scenario.complete(PASS);
        var before = session.semanticSnapshot();
        assertSame(primary, assertThrows(IllegalStateException.class, session::close));
        assertEquals(before, session.semanticSnapshot());
        assertFalse(Files.exists(file), "failed rendering cannot publish an apparently complete HTML");
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void repeatedCompletionPreservesEveryNodeAndPublishesTheOriginalVerdict(boolean fallback) throws Exception {
        var events = new ArrayList<AtfConsoleEvent>();
        try (var report = open(fallback, events::add)) {
            var scenario = report.session.startScenario("Atomicity", "original verdict", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("original request", "GET", "/local");
            var validation = request.startValidation("original validation");
            validation.assertions(List.of(new AtfAssertion("original comparison", "ready", "ready", true)));
            validation.complete(PASS);
            report.rejects(() -> validation.complete(FAIL));
            request.complete(PASS);
            report.rejects(() -> request.complete(FAIL));
            var assertions = phase.startAssertionStep("Component assertions");
            assertions.assertions(List.of(new AtfAssertion("independent comparison", "ready", "ready", true)));
            assertions.complete(PASS);
            report.rejects(() -> assertions.complete(SKIP));
            phase.complete(PASS);
            report.rejects(() -> phase.complete(FAIL));
            scenario.complete(PASS);
            report.rejects(() -> scenario.complete(FAIL));
            int eventCount = events.size();
            validation.close(); request.close(); assertions.close(); phase.close(); scenario.close();
            assertEquals(eventCount, events.size(), "close after complete does not repeat notifications");
            assertEquals(PASS, report.snapshot().classes().getFirst().status());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void everyLateMutationIsRejectedBeforeEvidenceOrCountersChange(boolean fallback) throws Exception {
        try (var report = open(fallback, AtfConsoleSink.noOp())) {
            var scenario = report.session.startScenario("Atomicity", "late evidence", List.of());
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("request", "", "");
            var validation = request.startValidation("validation");
            validation.complete(PASS); request.complete(PASS);
            report.rejects(() -> request.detail("REJECTED detail", "must not appear"));
            report.rejects(() -> request.json("REJECTED json", "{}"));
            report.rejects(() -> request.body(new ReportJsonBody("REJECTED body", "{}")));
            report.rejects(() -> request.screenshot("REJECTED screenshot", "eA=="));
            report.rejects(() -> request.uiActions(List.of()));
            report.rejects(() -> request.execution((ReportExecutionEvidence) null));
            report.rejects(() -> request.networkAggregate(null));
            report.rejects(() -> request.startValidation("REJECTED validation"));
            report.rejects(() -> validation.assertions(List.of()));
            report.rejects(() -> validation.evidenceOwner(ReportEvidenceOwner.UI));
            report.rejects(() -> validation.startRequest("REJECTED nested", "", ""));
            phase.complete(PASS);
            report.rejects(() -> phase.detail("REJECTED phase", "late"));
            report.rejects(() -> phase.executionTiming(new ReportPhaseTiming(Duration.ZERO, 1)));
            report.rejects(() -> phase.startRequest("REJECTED child", "", ""));
            report.rejects(() -> phase.startAssertionStep("REJECTED assertions"));
            scenario.complete(PASS);
            report.rejects(() -> scenario.startPhase("REJECTED phase"));
            report.session.flush();
            report.session.close();
            assertFalse(Files.readString(report.file).contains("REJECTED"));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void prematureParentCompletionAndCloseAreAtomicAndRecoverable(boolean fallback) throws Exception {
        try (var report = open(fallback, AtfConsoleSink.noOp())) {
            var scenario = report.session.startScenario("Atomicity", "recoverable parent", List.of());
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("outer", "", "");
            var validation = request.startValidation("nested evidence");
            var nested = validation.startRequest("nested", "", "");
            report.rejects(() -> validation.complete(PASS));
            report.rejects(validation::close);
            report.rejects(() -> request.complete(PASS));
            report.rejects(request::close);
            report.rejects(() -> phase.complete(PASS));
            report.rejects(phase::close);
            report.rejects(() -> scenario.complete(PASS));
            report.rejects(scenario::close);
            nested.complete(FAIL);
            validation.complete(PASS); request.complete(PASS);
            phase.complete(PASS); scenario.complete(PASS);
            assertEquals(FAIL, report.snapshot().classes().getFirst().status(), "nested FAIL survives caller PASS");
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void closeWithoutCompletionRecordsFailOnceAndLeavesNoOpenSemanticChild(boolean fallback) throws Exception {
        var events = new ArrayList<AtfConsoleEvent>();
        try (var report = open(fallback, events::add)) {
            var scenario = report.session.startScenario("Atomicity", "incomplete scope", List.of());
            var phase = scenario.startPhase("TEST BODY");
            var step = phase.startAssertionStep("Component assertions");
            step.close(); step.close();
            var request = phase.startRequest("interrupted request", "", "");
            var validation = request.startValidation("interrupted validation");
            validation.close(); validation.close();
            request.close(); request.close(); phase.close(); phase.close(); scenario.close(); scenario.close();
            assertEquals(FAIL, report.snapshot().classes().getFirst().status());
            assertEquals(5, events.stream().filter(event -> event.type() == AtfConsoleEvent.Type.COMPLETE).count());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void consoleFailureIsDiagnosticAndCannotInterruptPublication(boolean fallback) throws Exception {
        try (var report = open(fallback, event -> { throw new IllegalStateException("console unavailable"); })) {
            var scenario = report.session.startScenario("Atomicity", "console recovery", List.of());
            var phase = scenario.startPhase("TEST BODY"); phase.complete(PASS); scenario.complete(PASS);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void openHandlesCannotMutateAClosedSession(boolean fallback) throws Exception {
        var report = open(fallback, AtfConsoleSink.noOp());
        var scenario = report.session.startScenario("Atomicity", "closed session", List.of());
        report.session.close();
        var before = report.snapshot();
        assertThrows(IllegalStateException.class, () -> scenario.startPhase("REJECTED late phase"));
        assertEquals(before, report.snapshot());
    }

    private static Published open(boolean fallback, AtfConsoleSink console) {
        Path file = TestReportPaths.report("atomicity-" + (fallback ? "fallback-" : "extent-") + UUID.randomUUID() + ".html");
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, ReportingSettings.defaults(), console)
                : new ExtentReportSession(file, ReportingSettings.defaults(), new SensitiveDataRedactor(), console);
        return new Published(file, session);
    }

    private record Published(Path file, AtfReportPorts.Session session) implements AutoCloseable {
        AtfReportSemanticSnapshot snapshot() {
            return session instanceof HtmlReportSession html ? html.semanticSnapshot() : ((ExtentReportSession) session).semanticSnapshot();
        }
        void rejects(org.junit.jupiter.api.function.Executable transition) {
            var before = snapshot();
            assertThrows(IllegalStateException.class, transition);
            assertEquals(before, snapshot(), "rejected transition must not mutate the semantic population");
        }
        @Override public void close() throws Exception { session.close(); assertTrue(Files.size(file) > 0); }
    }
}
