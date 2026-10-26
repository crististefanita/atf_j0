package atf.core.reporting.duration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Provides one duration policy for console output and generated reports.
 *
 * <p>Short timings remain precise and compact in milliseconds. Once a timing
 * is greater than 300 ms it is shown in seconds, preserving millisecond
 * precision without displaying a misleading unit.</p>
 */
public final class AtfDurationFormatter {
    public static final long SECONDS_DISPLAY_THRESHOLD_MILLIS = 300L;

    private AtfDurationFormatter() {
    }

    /** Converts an elapsed duration to whole display milliseconds, rounding a positive fraction up. */
    public static long elapsedMillis(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        if (duration.isZero()) {
            return 0;
        }
        return Math.max(1L, (duration.toNanos() + 999_999L) / 1_000_000L);
    }

    public static String format(Duration duration) {
        return formatMillis(elapsedMillis(duration));
    }

    public static String formatMillis(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        if (millis <= SECONDS_DISPLAY_THRESHOLD_MILLIS) {
            return millis + " ms";
        }
        BigDecimal seconds = BigDecimal.valueOf(millis, 3).stripTrailingZeros();
        return seconds.toPlainString() + " s";
    }
}
