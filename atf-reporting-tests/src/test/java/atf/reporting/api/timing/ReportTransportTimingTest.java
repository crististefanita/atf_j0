package atf.reporting.api.timing;

import atf.testkit.AtfUnitTest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.model.ReportExecutionEvidence;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ReportTransportTimingTest {
    @Test
    void unknownDurationIsNotMeasuredZero() {
        var response = new ReportResponse(200, "{}", Map.of(), null, "");
        var execution = new ReportExecutionEvidence(response, 1, null, List.of(), List.of(), null, null);
        var attempt = new ReportAttemptRecord(1, 200, null, "SUCCESS", "");
        assertNull(response.duration());
        assertNull(execution.totalDuration());
        assertNull(attempt.duration());
        assertEquals(Duration.ZERO, new ReportResponse(200, "{}", Map.of(), Duration.ZERO, "").duration());
    }

    @Test
    void responseAndTotalRemainIndependentMeasurements() {
        var response = new ReportResponse(200, "{}", Map.of(), Duration.ofMillis(17), "");
        var execution = new ReportExecutionEvidence(response, 2, Duration.ofMillis(43), List.of(), List.of(), null, null);
        assertEquals(Duration.ofMillis(17), execution.response().duration());
        assertEquals(Duration.ofMillis(43), execution.totalDuration());
    }

    @Test
    void missingLegacyMeasurementIsNotDefaultedToZero() {
        var evidence = ReportModelAdapters.execution(new LegacyExecution(new LegacyResponse(200), 1));
        assertNull(evidence.response().duration());
        assertNull(evidence.totalDuration());
    }

    public record LegacyResponse(int status) { }
    public record LegacyExecution(LegacyResponse response, int attempts) { }

    @Test
    void malformedAndNegativeMeasurementsAreRejectedRatherThanInventingZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReportResponse(200, "{}", Map.of(), Duration.ofMillis(-1), ""));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportAttemptRecord(1, 200, Duration.ofMillis(-1), "SUCCESS", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportExecutionEvidence(new ReportResponse(200, "{}", Map.of(), null, ""),
                        1, Duration.ofMillis(-1), List.of(), List.of(), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> ReportModelAdapters.execution(new TimedExecution(new TimedResponse(200, "not-a-duration"), 1)));
    }

    public record TimedResponse(int status, Object duration) { }
    public record TimedExecution(TimedResponse response, int attempts) { }
}
