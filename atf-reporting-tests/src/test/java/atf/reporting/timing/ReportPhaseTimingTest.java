package atf.reporting.timing;

import atf.testkit.AtfUnitTest;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.model.timing.ReportPhaseDurations;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.contract.AtfReportStatus;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportPhaseTimingTest {
    @Test void keepsExactObservationAndUsesTheSharedDisplayResolutionWithoutOverflow() {
        assertEquals(0, new ReportPhaseTiming(Duration.ZERO, 1).durationMillis());
        assertEquals(1, new ReportPhaseTiming(Duration.ofNanos(1), 1).durationMillis());
        var large = new ReportPhaseTiming(Duration.ofNanos(Long.MAX_VALUE), 1);
        assertEquals(9_223_372_036_855L, large.durationMillis());
        assertThrows(IllegalArgumentException.class, () -> new ReportPhaseTiming(Duration.ofMillis(-1), 1));
        assertThrows(IllegalArgumentException.class, () -> new ReportPhaseTiming(Duration.ZERO, 0));
    }
    @Test void absentTimingIsUnknownAndDirectPhasesAreCountedOnce() {
        var measured = new ReportExecutionModel.Phase("TEST BODY", List.of(), List.of(), AtfReportStatus.PASS,
                Optional.of(new ReportPhaseTiming(Duration.ofMillis(123), 1)), 9_000L);
        var unknown = new ReportExecutionModel.Phase("SETUP", List.of(), List.of(), AtfReportStatus.PASS);
        var known = scenario(List.of(measured, measured));
        assertEquals(246, ReportPhaseDurations.scenario(known).orElseThrow());
        assertEquals(492, ReportPhaseDurations.total(List.of(known, known)).orElseThrow());
        assertTrue(ReportPhaseDurations.scenario(scenario(List.of(measured, unknown))).isEmpty());
        assertTrue(ReportPhaseDurations.total(List.of()).isEmpty());
    }
    private static ReportExecutionModel.Scenario scenario(List<ReportExecutionModel.Phase> phases) {
        return new ReportExecutionModel.Scenario("id", "Case", "case", List.of(), phases, AtfReportStatus.PASS);
    }
}
