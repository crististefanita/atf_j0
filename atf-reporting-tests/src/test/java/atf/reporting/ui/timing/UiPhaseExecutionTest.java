package atf.reporting.ui.timing;
import atf.reporting.model.timing.ReportPhaseTiming;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UiPhaseExecutionTest {
    @Test void completedMeasurementRejectsLateEvidenceBeforeRunningIt() {
        var time = new UiPhaseExecution(() -> 19);
        var before = time.complete();
        var invoked = new java.util.concurrent.atomic.AtomicBoolean();
        assertThrows(IllegalStateException.class, () -> time.capture(() -> { invoked.set(true); return "late"; }));
        assertThrows(IllegalStateException.class, () -> time.publish(() -> invoked.set(true)));
        assertFalse(invoked.get()); assertSame(before, time.complete()); assertSame(before, time.snapshot());
    }

    @Test void businessCapturePublicationAndPostExecutionCleanupAreDistinct() {
        var ticks = new AtomicLong(); var time = new UiPhaseExecution(ticks::get);
        ticks.addAndGet(200_000_000);
        assertEquals("image", time.capture(() -> { ticks.addAndGet(50_000_000); return "image"; }));
        time.publish(() -> ticks.addAndGet(70_000_000));
        time.finishExecution();
        ticks.addAndGet(900_000_000); // Resource release and cleanup are outside the closed business interval.
        time.publish(() -> ticks.addAndGet(30_000_000));
        var result = time.snapshot();
        assertEquals(Duration.ofMillis(200), result.execution());
        assertEquals(Duration.ofMillis(50), result.evidence().orElseThrow().capture());
        assertEquals(Duration.ofMillis(100), result.evidence().orElseThrow().publication());
        assertEquals(result, time.snapshot(), "completion is idempotent");
    }
    @Test void zeroMeasuredIsPresentAndLegacyMissingEvidenceIsStillUnknown() {
        var time = new UiPhaseExecution(() -> 17);
        assertEquals(Duration.ZERO, time.snapshot().execution());
        assertTrue(time.snapshot().evidence().isPresent());
        assertTrue(new ReportPhaseTiming(Duration.ZERO, 1).evidence().isEmpty());
    }
    @Test void failedEvidenceRetainsItsDurationAndItsOriginalCause() {
        var ticks = new AtomicLong(); var time = new UiPhaseExecution(ticks::get);
        var primary = new IllegalStateException("capture failed");
        assertSame(primary, assertThrows(IllegalStateException.class, () -> time.capture(() -> {
            ticks.addAndGet(50_000_000); throw primary;
        })));
        assertEquals(Duration.ofMillis(50), time.snapshot().evidence().orElseThrow().capture());
        assertEquals(Duration.ZERO, time.snapshot().execution());
    }
    @Test void secondaryClockFailureCannotReplaceTheOriginalCaptureFailure() {
        var ticks = new AtomicLong(10); var time = new UiPhaseExecution(ticks::get);
        var primary = new AssertionError("original capture failure");
        assertSame(primary, assertThrows(AssertionError.class, () -> time.capture(() -> { ticks.set(1); throw primary; })));
        assertEquals(1, primary.getSuppressed().length);
        assertInstanceOf(IllegalStateException.class, primary.getSuppressed()[0]);
    }
    @Test void overlappingEvidenceAndAnUnfinishedCaptureCannotBeSilentlyCountedTwice() {
        var time = new UiPhaseExecution(() -> Long.MIN_VALUE);
        time.capture(() -> {
            assertThrows(IllegalStateException.class, () -> time.publish(() -> { }));
            assertThrows(IllegalStateException.class, time::finishExecution);
            return null;
        });
        assertEquals(Duration.ZERO, time.snapshot().execution());
    }
}
