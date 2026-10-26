package atf.reporting.runtime.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configurable, renderer-neutral performance policy for a report artifact.
 *
 * <p>It intentionally does not choose a business verdict. A browser/component
 * audit can decide whether missing optional browser metrics are acceptable and
 * can publish the returned violations as assertions.</p>
 */
public record ReportRuntimePerformanceBudget(
        long maxPageLoadMs,
        long maxReadinessMs,
        long maxFirstDisclosureToggleMs,
        long maxDisclosureToggleDurationMs,
        long maxNormalizationDurationMs,
        long maxNormalizationSchedules,
        long maxNormalizationRuns,
        long maxRelevantMutationBatches,
        long maxDisclosureToggles,
        long maxLongTaskCount,
        long maxLongTaskDurationMs,
        boolean readinessRequired,
        boolean firstDisclosureToggleRequired) {

    public ReportRuntimePerformanceBudget {
        requireNonNegative(maxPageLoadMs, "maxPageLoadMs");
        requireNonNegative(maxReadinessMs, "maxReadinessMs");
        requireNonNegative(maxFirstDisclosureToggleMs, "maxFirstDisclosureToggleMs");
        requireNonNegative(maxDisclosureToggleDurationMs, "maxDisclosureToggleDurationMs");
        requireNonNegative(maxNormalizationDurationMs, "maxNormalizationDurationMs");
        requireNonNegative(maxNormalizationSchedules, "maxNormalizationSchedules");
        requireNonNegative(maxNormalizationRuns, "maxNormalizationRuns");
        requireNonNegative(maxRelevantMutationBatches, "maxRelevantMutationBatches");
        requireNonNegative(maxDisclosureToggles, "maxDisclosureToggles");
        requireNonNegative(maxLongTaskCount, "maxLongTaskCount");
        requireNonNegative(maxLongTaskDurationMs, "maxLongTaskDurationMs");
    }

    /** Explicit starter policy for a medium report; callers should tune it to their fixture. */
    public static ReportRuntimePerformanceBudget componentDefaults() {
        return new ReportRuntimePerformanceBudget(
                2500, 1000, 250, 1200, 900,
                16, 12, 24, 12, 8, 750,
                true, true);
    }

    /**
     * Starter policy for a generated browser artifact whose audit expands a
     * representative set of request signatures. The disclosure budget is
     * intentionally larger than the deterministic component fixture. The
     * browser artifact also gets a bounded long-task allowance because parsing,
     * disclosure expansion and browser scheduling are part of this proof; this
     * is tolerance for measured scheduler variance, not a disabled gate.
     */
    public static ReportRuntimePerformanceBudget browserAuditDefaults() {
        return new ReportRuntimePerformanceBudget(
                2500, 1000, 250, 1200, 900,
                16, 12, 24, 64, 16, 1_500,
                true, true);
    }

    public List<String> violations(ReportRuntimePerformanceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<String> violations = new ArrayList<>();
        if (readinessRequired && !snapshot.readinessAvailable()) {
            violations.add("readiness metric is unavailable");
        } else if (snapshot.readinessAvailable()) {
            check(violations, "readinessMs", snapshot.readinessMs(), maxReadinessMs);
        }
        if (snapshot.pageLoadMs() != ReportRuntimePerformanceSnapshot.UNAVAILABLE) {
            check(violations, "pageLoadMs", snapshot.pageLoadMs(), maxPageLoadMs);
        }
        if (firstDisclosureToggleRequired && !snapshot.firstDisclosureToggleAvailable()) {
            violations.add("firstDisclosureToggleMs metric is unavailable");
        } else if (snapshot.firstDisclosureToggleAvailable()) {
            check(violations, "firstDisclosureToggleMs",
                    snapshot.firstDisclosureToggleMs(), maxFirstDisclosureToggleMs);
        }
        check(violations, "disclosureToggleDurationMs",
                snapshot.disclosureToggleDurationMs(), maxDisclosureToggleDurationMs);
        check(violations, "normalizationDurationMs",
                snapshot.normalizationDurationMs(), maxNormalizationDurationMs);
        check(violations, "normalizationSchedules",
                snapshot.normalizationSchedules(), maxNormalizationSchedules);
        check(violations, "normalizationRuns", snapshot.normalizationRuns(), maxNormalizationRuns);
        check(violations, "relevantMutationBatches",
                snapshot.relevantMutationBatches(), maxRelevantMutationBatches);
        check(violations, "disclosureToggles", snapshot.disclosureToggles(), maxDisclosureToggles);
        if (snapshot.longTaskObservationSupported()) {
            check(violations, "longTaskCount", snapshot.longTaskCount(), maxLongTaskCount);
            check(violations, "longTaskDurationMs",
                    snapshot.longTaskDurationMs(), maxLongTaskDurationMs);
        }
        return List.copyOf(violations);
    }

    public boolean accepts(ReportRuntimePerformanceSnapshot snapshot) {
        return violations(snapshot).isEmpty();
    }

    private static void check(List<String> violations, String metric, long actual, long limit) {
        if (actual > limit) violations.add(metric + " exceeds " + limit + ": " + actual);
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must be non-negative");
    }
}
