package atf.reporting.ui.timing;

import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.UiExtentReportOptions;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.*;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.TestReportPaths;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiPhaseTimingLifecycleComponentTest {
    @ParameterizedTest
    @CsvSource({"false,SCREEN_CHECKPOINTS,PASS", "true,SCREEN_CHECKPOINTS,FAIL", "false,UNIFIED_FLOW,SKIP", "true,UNIFIED_FLOW,PASS"})
    void measuredUiPhasesKeepTheSameExecutionAcrossOutcomesAndBufferedPublication(boolean fallback, ReportEvidenceFlowMode flow,
                                                                                 AtfReportStatus status) throws Exception {
        var ticks = new AtomicLong();
        var publicationWork = new AtomicLong();
        Path file = TestReportPaths.report("ui-phase-timing-" + fallback + "-" + flow + "-" + System.nanoTime() + ".html");
        AtfConsoleSink console = event -> {
            if (event.scope() == AtfConsoleEvent.Scope.REQUEST) {
                if (event.type() == AtfConsoleEvent.Type.START) { ticks.addAndGet(70_000_000); publicationWork.addAndGet(70_000_000); }
                if (event.type() == AtfConsoleEvent.Type.COMPLETE) { ticks.addAndGet(30_000_000); publicationWork.addAndGet(30_000_000); }
            }
        };
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, ReportingSettings.defaults(), console)
                : new ExtentReportSession(file, ReportingSettings.defaults(), new SensitiveDataRedactor(), console);
        try (session) {
            var profile = new ReportEvidenceProfile(flow, ReportScreenshotPolicy.NONE, ReportEvidenceCaptureSource.CUSTOM_PROVIDER);
            var scenario = UiExtentReportSession.adaptScenario(session.startScenario("UiTiming", "measured UI phase", List.of("component")), profile, "timed UI");
            var phase = scenario.startPhase("TEST BODY");
            var execution = phase.measureExecution(ticks::get);
            assertThrows(IllegalStateException.class, () -> phase.measureExecution(ticks::get));
            assertThrows(IllegalStateException.class, () -> phase.executionTiming(new ReportPhaseTiming(Duration.ZERO, 1)));
            ticks.addAndGet(200_000_000);
            execution.capture(() -> { ticks.addAndGet(50_000_000); return "captured"; });
            phase.step(AtfGherkinKeyword.THEN, "the independently checked result is published", "local", "",
                    List.of(new AtfAssertion("result", "ready", status == AtfReportStatus.FAIL ? "missing" : "ready", status != AtfReportStatus.FAIL)), status);
            execution.finishExecution();
            ticks.addAndGet(900_000_000); // A teardown failure/resource release must not inflate TEST BODY.
            phase.completeFinal(status);
            scenario.complete(status);
            var snapshot = fallback ? ((HtmlReportSession) session).semanticSnapshot() : ((ExtentReportSession) session).semanticSnapshot();
            var measured = snapshot.classes().getFirst().scenarios().getFirst().phases().getFirst();
            assertEquals(status, measured.status());
            var timing = measured.timing().orElseThrow();
            assertEquals(Duration.ofMillis(200), timing.execution());
            assertEquals(Duration.ofMillis(50), timing.evidence().orElseThrow().capture());
            assertTrue(publicationWork.get() > 0, "the fixture actually performs bounded publication work");
            assertEquals(Duration.ofNanos(publicationWork.get()), timing.evidence().orElseThrow().publication());
        }
        String html = Files.readString(file);
        assertTrue(html.contains("data-atf-execution-duration-ms=\"200\""));
        assertTrue(html.contains("data-atf-capture-duration-ms=\"50\""));
    }

    @Test void publicUiFacadePropagatesTypedTimingAndRejectsASecondValueWithoutChangingIt() throws Exception {
        Path file = TestReportPaths.report("ui-public-timing-" + System.nanoTime() + ".html");
        try (var session = UiExtentReportSession.openConfigured(UiExtentReportOptions.builder(file.getParent())
                .fileName(file.getFileName().toString()).timestampEnabled(false).build())) {
            var scenario = session.startScenario("UiPublicTiming", "atf.reporting.ui", "zero remains measured", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            phase.executionTiming(new ReportPhaseTiming(Duration.ZERO, 1));
            assertThrows(IllegalStateException.class, () -> phase.executionTiming(new ReportPhaseTiming(Duration.ofMillis(9), 1)));
            phase.complete(AtfReportStatus.SKIP); scenario.complete(AtfReportStatus.SKIP);
            assertEquals(Duration.ZERO, session.semanticSnapshot().classes().getFirst().scenarios().getFirst().phases().getFirst().timing().orElseThrow().execution());
        }
        assertTrue(Files.readString(file).contains("data-atf-execution-duration-ms=\"0\""));
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void failedSetupHasMeasuredTimingAndDoesNotInventATestBody(boolean fallback) throws Exception {
        Path file = TestReportPaths.report("ui-setup-timing-" + fallback + "-" + System.nanoTime() + ".html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file)) {
            var scenario = lifecycle(session, ReportEvidenceFlowMode.SCREEN_CHECKPOINTS);
            var setup = scenario.startPhase("SETUP"); var ticks = new AtomicLong();
            var execution = setup.measureExecution(ticks::get); ticks.addAndGet(37_000_000);
            var primary = new IllegalStateException("setup capture unavailable");
            assertSame(primary, assertThrows(IllegalStateException.class, () -> execution.capture(() -> {
                ticks.addAndGet(5_000_000); throw primary;
            })));
            setup.close(); scenario.close();
            var phases = snapshot(session).classes().getFirst().scenarios().getFirst().phases();
            assertEquals(1, phases.size()); assertEquals("SETUP", phases.getFirst().name());
            assertEquals(AtfReportStatus.FAIL, phases.getFirst().status());
            assertEquals(Duration.ofMillis(37), phases.getFirst().timing().orElseThrow().execution());
            assertEquals(Duration.ofMillis(5), phases.getFirst().timing().orElseThrow().evidence().orElseThrow().capture());
        }
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void failedTeardownKeepsTheAlreadyMeasuredBodyAndItsOwnFailureTiming(boolean fallback) throws Exception {
        Path file = TestReportPaths.report("ui-teardown-timing-" + fallback + "-" + System.nanoTime() + ".html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file)) {
            var scenario = lifecycle(session, ReportEvidenceFlowMode.SCREEN_CHECKPOINTS);
            var ticks = new AtomicLong(); var body = scenario.startPhase("TEST BODY");
            body.measureExecution(ticks::get); ticks.addAndGet(200_000_000); body.complete(AtfReportStatus.PASS);
            var teardown = scenario.startPhase("TEARDOWN"); teardown.measureExecution(ticks::get);
            ticks.addAndGet(9_000_000); teardown.close(); scenario.complete(AtfReportStatus.PASS);
            var result = snapshot(session).classes().getFirst().scenarios().getFirst();
            assertEquals(AtfReportStatus.FAIL, result.status());
            assertEquals(Duration.ofMillis(200), result.phases().getFirst().timing().orElseThrow().execution());
            assertEquals(Duration.ofMillis(9), result.phases().getLast().timing().orElseThrow().execution());
            assertEquals(AtfReportStatus.FAIL, result.phases().getLast().status());
        }
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void aCrossPhaseUnifiedFlowRetainsEachExecutionOnceAndPublishesAtTheTerminalPhase(boolean fallback) throws Exception {
        Path file = TestReportPaths.report("ui-cross-phase-timing-" + fallback + "-" + System.nanoTime() + ".html");
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file)) {
            var scenario = lifecycle(session, ReportEvidenceFlowMode.UNIFIED_FLOW); scenario.unifyAcrossPhases();
            var ticks = new AtomicLong(); var setup = scenario.startPhase("SETUP"); setup.measureExecution(ticks::get);
            ticks.addAndGet(11_000_000);
            setup.step(AtfGherkinKeyword.GIVEN, "setup is ready", "local", "", AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase("TEST BODY"); body.measureExecution(ticks::get); ticks.addAndGet(23_000_000);
            body.step(AtfGherkinKeyword.THEN, "business is ready", "local", "", AtfReportStatus.PASS);
            body.completeFinal(AtfReportStatus.PASS); scenario.complete(AtfReportStatus.PASS);
            var phases = snapshot(session).classes().getFirst().scenarios().getFirst().phases();
            assertEquals(List.of(11L, 23L), phases.stream().map(value -> value.timing().orElseThrow().durationMillis()).toList());
            assertEquals(Duration.ZERO, phases.getFirst().timing().orElseThrow().evidence().orElseThrow().publication());
            assertEquals(0, phases.getFirst().nodes().size());
            assertEquals(1, phases.getLast().nodes().size(), "one terminal unified flow owns both checkpoints");
        }
    }

    private static UiExtentReportSession.UiScenario lifecycle(AtfReportPorts.Session session, ReportEvidenceFlowMode flow) {
        return UiExtentReportSession.adaptScenario(session.startScenario("UiTiming", "boundary failure", List.of("component")),
                new ReportEvidenceProfile(flow, ReportScreenshotPolicy.NONE, ReportEvidenceCaptureSource.CUSTOM_PROVIDER), "boundary failure");
    }
    private static AtfReportSemanticSnapshot snapshot(AtfReportPorts.Session session) {
        return session instanceof HtmlReportSession html ? html.semanticSnapshot() : ((ExtentReportSession) session).semanticSnapshot();
    }
}
