package atf.core.reporting;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.duration.AtfDurationFormatter;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest

class AtfDurationFormatterTest {
    @Test
    void preservesZeroButRoundsPositiveSubMillisecondDurationsUpToOneMillisecond() {
        assertEquals(0, AtfDurationFormatter.elapsedMillis(Duration.ZERO));
        assertEquals(1, AtfDurationFormatter.elapsedMillis(Duration.ofNanos(1)));
        assertEquals(1, AtfDurationFormatter.elapsedMillis(Duration.ofNanos(999_999)));
        assertEquals(2, AtfDurationFormatter.elapsedMillis(Duration.ofNanos(1_000_001)));
    }

    @Test
    void preservesWholeMillisecondsAndRejectsNegativeDurations() {
        assertEquals(12, AtfDurationFormatter.elapsedMillis(Duration.ofMillis(12)));
        assertThrows(IllegalArgumentException.class,
                () -> AtfDurationFormatter.elapsedMillis(Duration.ofNanos(-1)));
    }

    @Test
    void keepsTheThresholdInMillisecondsAndConvertsOnlyValuesAboveItToSeconds() {
        assertEquals("300 ms", AtfDurationFormatter.formatMillis(300));
        assertEquals("0.301 s", AtfDurationFormatter.formatMillis(301));
        assertEquals("0.999 s", AtfDurationFormatter.formatMillis(999));
    }

    @Test
    void usesReadableSecondPrecisionWithoutTrailingZeros() {
        assertEquals("1 s", AtfDurationFormatter.formatMillis(1_000));
        assertEquals("1.5 s", AtfDurationFormatter.formatMillis(1_500));
        assertEquals("12.345 s", AtfDurationFormatter.formatMillis(12_345));
        assertEquals("0 ms", AtfDurationFormatter.format(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> AtfDurationFormatter.formatMillis(-1));
    }
}
