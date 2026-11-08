package atf.core.lifecycle;
import atf.core.reporting.contract.AtfAssertion;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void detailFailureKeepsTheBusinessCauseAndStillCompletesAndClosesThePhase() {
        var businessFailure = new IllegalStateException("business failure");
        var detailFailure = new IllegalStateException("detail publisher failure");
        var phase = new RecordingPhase();
        phase.detailFailure = detailFailure;

        var result = executor.execute(new RecordingScenario(phase), "TEST BODY", ignored -> {
            throw businessFailure;
        });

        assertAll(
                () -> assertEquals(AtfReportStatus.FAIL, result.status()),
                () -> assertSame(businessFailure, result.failure()),
                () -> assertEquals(List.of(detailFailure), List.of(result.failure().getSuppressed())),
                () -> assertEquals(AtfReportStatus.FAIL, phase.completedStatus),
                () -> assertEquals(1, phase.completeCalls),
                () -> assertEquals(1, phase.closeCalls));
    }

    @Test
    void completionAndCloseFailuresAreSuppressedInOrderWithoutSkippingCleanup() {
        var businessFailure = new IllegalStateException("business failure");
        var completionFailure = new IllegalStateException("phase completion failure");
        var closeFailure = new IllegalStateException("phase close failure");
        var phase = new RecordingPhase();
        phase.completeFailure = completionFailure;
        phase.closeFailure = closeFailure;

        var result = executor.execute(new RecordingScenario(phase), "TEST BODY", ignored -> {
            throw businessFailure;
        });

        assertAll(
                () -> assertSame(businessFailure, result.failure()),
                () -> assertEquals(List.of(completionFailure, closeFailure),
                        List.of(result.failure().getSuppressed())),
                () -> assertEquals(1, phase.completeCalls),
                () -> assertEquals(1, phase.closeCalls));
    }

    @Test
    void lifecycleFailureBecomesPrimaryWhenTheBusinessPhasePassed() {
        var completionFailure = new IllegalStateException("phase completion failure");
        var phase = new RecordingPhase();
        phase.completeFailure = completionFailure;

        var result = executor.execute(new RecordingScenario(phase), "TEST BODY", ignored -> { });

        assertAll(
                () -> assertEquals(AtfReportStatus.FAIL, result.status()),
                () -> assertSame(completionFailure, result.failure()),
                () -> assertEquals(1, phase.completeCalls),
                () -> assertEquals(1, phase.closeCalls));
    }

    @Test
    void closeFailureBecomesPrimaryWhenPhaseBodyAndCompletionPassed() {
        var closeFailure = new IllegalStateException("phase close failure");
        var phase = new RecordingPhase();
        phase.closeFailure = closeFailure;

        var result = executor.execute(new RecordingScenario(phase), "TEST BODY", ignored -> { });

        assertAll(
                () -> assertEquals(AtfReportStatus.FAIL, result.status()),
                () -> assertSame(closeFailure, result.failure()),
                () -> assertEquals(1, phase.completeCalls),
                () -> assertEquals(1, phase.closeCalls));
    }

    @Test
    void detailCapabilityFailurePreservesTheBusinessCauseAndClosesThePhase() {
        var businessFailure = new IllegalStateException("business failure");
        var capabilityFailure = new IllegalStateException("capability query failed");
        var phase = new RecordingPhase();
        phase.detailsCapabilityFailure = capabilityFailure;

        var result = executor.execute(new RecordingScenario(phase), "TEST BODY", ignored -> {
            throw businessFailure;
        });

        assertAll(
                () -> assertSame(businessFailure, result.failure()),
                () -> assertEquals(List.of(capabilityFailure), List.of(result.failure().getSuppressed())),
                () -> assertEquals(AtfReportStatus.FAIL, phase.completedStatus),
                () -> assertEquals(1, phase.completeCalls),
                () -> assertEquals(1, phase.closeCalls));
    }

    @Test
    void unsupportedDetailPortIsVisibleAsSuppressedEvidenceCapabilityFailure() {
        var businessFailure = new IllegalStateException("business failure");
        var phase = new RecordingPhase();
        phase.detailsSupported = false;

        var result = executor.execute(new RecordingScenario(phase), "TEST BODY", ignored -> {
            throw businessFailure;
        });

        assertAll(
                () -> assertSame(businessFailure, result.failure()),
                () -> assertEquals(1, result.failure().getSuppressed().length),
                () -> assertTrue(result.failure().getSuppressed()[0] instanceof UnsupportedOperationException),
                () -> assertTrue(phase.details.isEmpty()),
                () -> assertEquals(AtfReportStatus.FAIL, phase.completedStatus),
                () -> assertEquals(1, phase.closeCalls));
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
        private int completeCalls;
        private int closeCalls;
        private RuntimeException detailFailure;
        private RuntimeException detailsCapabilityFailure;
        private RuntimeException completeFailure;
        private RuntimeException closeFailure;
        private boolean detailsSupported = true;

        @Override public AtfReportPorts.Request startRequest(String title, String method, String path) {
            return new NoOpRequest();
        }

        @Override public boolean supportsDetails() {
            if (detailsCapabilityFailure != null) throw detailsCapabilityFailure;
            return detailsSupported;
        }

        @Override public void detail(String name, String value) {
            details.add(name + ": " + value);
            if (detailFailure != null) throw detailFailure;
        }

        @Override public void complete(AtfReportStatus status) {
            completeCalls++;
            completedStatus = status;
            if (completeFailure != null) throw completeFailure;
        }
        @Override public void close() {
            closeCalls++;
            closed = true;
            if (closeFailure != null) throw closeFailure;
        }
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
