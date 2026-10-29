package atf.reporting.model.timing;
import java.util.Optional;

import atf.reporting.duration.AtfDurationFormatter;
import java.time.Duration;
import java.util.Objects;

/** Execution observed by a caller; publication time is measured separately by the renderer. */
public record ReportPhaseTiming(Duration execution, int intervals, Optional<ReportPhaseEvidenceTiming> evidence) {
    public ReportPhaseTiming(Duration execution, int intervals) { this(execution, intervals, Optional.empty()); }
    public ReportPhaseTiming {
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(evidence, "evidence");
        if (execution.isNegative() || intervals <= 0) throw new IllegalArgumentException("Invalid phase timing");
    }
    public long durationMillis() { return AtfDurationFormatter.elapsedMillis(execution); }
}
