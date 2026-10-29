package atf.reporting.model.timing;

import java.time.Duration;
import java.util.Objects;

/** Measured evidence work, excluded from the caller's business execution interval. */
public record ReportPhaseEvidenceTiming(Duration capture, Duration publication) {
    public ReportPhaseEvidenceTiming {
        Objects.requireNonNull(capture, "capture");
        Objects.requireNonNull(publication, "publication");
        if (capture.isNegative() || publication.isNegative()) throw new IllegalArgumentException("Negative evidence timing");
    }
}
