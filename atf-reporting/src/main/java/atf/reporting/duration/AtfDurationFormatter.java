package atf.reporting.duration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/** Canonical duration presentation for standalone report consumers. */
public final class AtfDurationFormatter {
    public static final long SECONDS_DISPLAY_THRESHOLD_MILLIS = 300L;
    private AtfDurationFormatter() { }
    public static long elapsedMillis(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("duration must not be negative");
        long millis = duration.toMillis();
        return duration.getNano() % 1_000_000 == 0 ? millis : Math.addExact(millis, 1L);
    }
    public static String format(Duration duration) { return formatMillis(elapsedMillis(duration)); }
    public static String formatMillis(long millis) {
        if (millis < 0) throw new IllegalArgumentException("duration must not be negative");
        if (millis <= SECONDS_DISPLAY_THRESHOLD_MILLIS) return millis + " ms";
        return BigDecimal.valueOf(millis, 3).stripTrailingZeros().toPlainString() + " s";
    }
}
