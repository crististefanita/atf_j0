package atf.reporting.api.evidence;

import atf.reporting.duration.AtfDurationFormatter;
import java.time.Duration;

/** Null means no measurement; a measured zero remains a real zero. */
public final class ReportTransportTiming {
    public static final String NOT_CAPTURED = "Not captured";
    private ReportTransportTiming() { }
    public static String display(Duration duration) {
        return duration == null ? NOT_CAPTURED : AtfDurationFormatter.format(duration);
    }
    public static void validate(Duration duration) {
        if (duration != null && duration.isNegative()) throw new IllegalArgumentException("duration must not be negative");
    }
}
