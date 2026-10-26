package atf.core.lifecycle;
import atf.core.reporting.contract.AtfAssertion;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class ApiScenarioPhaseExecutorTest {
    private final ApiScenarioPhaseExecutor executor = new ApiScenarioPhaseExecutor();

    @Test
    void mapsControlSignalsWithoutAReportingDependency() {
        var pass = executor.execute(() -> { });
        var warning = executor.execute(() -> {
            throw new ApiScenarioLifecycleRunner.WarningSignal("known drift");
        });
        var skip = executor.execute(() -> {
            throw new ApiScenarioLifecycleRunner.SkipSignal("missing prerequisite");
        });
        var fail = executor.execute(() -> {
            throw new IllegalStateException("transport failure");
        });

        assertEquals(AtfReportStatus.PASS, pass.status());
        assertEquals(AtfReportStatus.WARNING, warning.status());
        assertEquals(AtfReportStatus.SKIP, skip.status());
        assertEquals(AtfReportStatus.FAIL, fail.status());
        assertEquals("transport failure", fail.failure().getMessage());
    }

    @Test
    void closesAndRecordsFailureCauseForRenderedPhase() {
        var phase = new RecordingPhase();
        var scenario = new RecordingScenario(phase);

        var result = executor.execute(scenario, "TEST BODY", ignored -> {
            throw new IllegalStateException("body failure");
        });

        assertEquals(AtfReportStatus.FAIL, result.status());
        assertEquals(AtfReportStatus.FAIL, phase.completedStatus);
        assertTrue(phase.closed);
        assertEquals(1, phase.details.size());
        assertTrue(phase.details.getFirst().contains("body failure"));
    }

    @Test
    void onlyUnexpectedFailuresCanBecomePrimaryCause() {
        assertFalse(ApiScenarioPhaseExecutor.isPrimaryFailure(
                new ApiScenarioLifecycleRunner.WarningSignal("warning")));
        assertFalse(ApiScenarioPhaseExecutor.isPrimaryFailure(
                new ApiScenarioLifecycleRunner.SkipSignal("skip")));
        assertTrue(ApiScenarioPhaseExecutor.isPrimaryFailure(
                new IllegalStateException("failure")));
    }

    private static final class RecordingScenario implements AtfReportPorts.Scenario {
        private final RecordingPhase phase;

        private RecordingScenario(RecordingPhase phase) {
            this.phase = phase;
        }

        @Override public String id() { return "scenario"; }
        @Override public AtfReportPorts.Phase startPhase(String name) { return phase; }
        @Override public void complete(AtfReportStatus status) { }
        @Override public boolean hasWarnings() { return false; }
        @Override public void close() { }
    }

    private static final class RecordingPhase implements AtfReportPorts.Phase {
        private final List<String> details = new java.util.ArrayList<>();
        private AtfReportStatus completedStatus;
        private boolean closed;

        @Override public AtfReportPorts.Request startRequest(String title, String method, String path) {
            return new NoOpRequest();
        }

        @Override public void detail(String name, String value) {
            details.add(name + ": " + value);
        }

        @Override public void complete(AtfReportStatus status) { completedStatus = status; }
        @Override public void close() { closed = true; }
    }

    private static final class NoOpRequest implements AtfReportPorts.Request {
        @Override public void detail(String name, String value) { }
        @Override public void json(String label, String json) { }
        @Override public AtfReportPorts.Validation startValidation(String title) { return new NoOpValidation(); }
        @Override public void complete(AtfReportStatus status) { }
        @Override public void close() { }
    }

    private static final class NoOpValidation implements AtfReportPorts.Validation {
        @Override public void assertions(Collection<AtfAssertion> assertions) { }
        @Override public void complete(AtfReportStatus status) { }
        @Override public void close() { }
    }
}
