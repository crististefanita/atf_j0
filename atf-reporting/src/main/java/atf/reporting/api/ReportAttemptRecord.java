package atf.reporting.api;

import java.time.Duration;
import atf.reporting.api.evidence.ReportTransportTiming;

public record ReportAttemptRecord(int number, int statusCode, Duration duration, String outcome, String reason) {
    /** -1 means the browser request failed before an HTTP response existed. */
    public ReportAttemptRecord {
        if (number < 1 || statusCode < -1) throw new IllegalArgumentException("invalid attempt");
        ReportTransportTiming.validate(duration);
        reason = reason == null ? "" : reason;
    }

    public String statusLabel() { return statusCode <= 0 ? "Unavailable" : Integer.toString(statusCode); }
}
