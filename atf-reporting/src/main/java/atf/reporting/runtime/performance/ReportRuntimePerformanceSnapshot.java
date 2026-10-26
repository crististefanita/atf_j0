package atf.reporting.runtime.performance;

import java.util.Map;
import java.util.Objects;

/**
 * Browser-neutral projection of the runtime performance counters published by
 * the report navigation assets.
 *
 * <p>The snapshot is deliberately a value object. Browser adapters may map a
 * JavaScript object into it, while component tests can create deterministic
 * fixtures without starting a browser or changing the report verdict.</p>
 */
public record ReportRuntimePerformanceSnapshot(
        long pageLoadMs,
        long readinessMs,
        long firstDisclosureToggleMs,
        long disclosureToggleDurationMs,
        long normalizationDurationMs,
        long normalizationSchedules,
        long normalizationRuns,
        long relevantMutationBatches,
        long durationMutationBatches,
        long disclosureToggles,
        long scenarioToggles,
        long phaseToggles,
        long nodeToggles,
        long longTaskCount,
        long longTaskDurationMs,
        boolean longTaskObservationSupported,
        boolean ready) {

    public static final long UNAVAILABLE = -1L;

    public ReportRuntimePerformanceSnapshot {
        requireOptionalNonNegative(pageLoadMs, "pageLoadMs");
        requireOptionalNonNegative(readinessMs, "readinessMs");
        requireOptionalNonNegative(firstDisclosureToggleMs, "firstDisclosureToggleMs");
        requireNonNegative(disclosureToggleDurationMs, "disclosureToggleDurationMs");
        requireNonNegative(normalizationDurationMs, "normalizationDurationMs");
        requireNonNegative(normalizationSchedules, "normalizationSchedules");
        requireNonNegative(normalizationRuns, "normalizationRuns");
        requireNonNegative(relevantMutationBatches, "relevantMutationBatches");
        requireNonNegative(durationMutationBatches, "durationMutationBatches");
        requireNonNegative(disclosureToggles, "disclosureToggles");
        requireNonNegative(scenarioToggles, "scenarioToggles");
        requireNonNegative(phaseToggles, "phaseToggles");
        requireNonNegative(nodeToggles, "nodeToggles");
        requireNonNegative(longTaskCount, "longTaskCount");
        requireNonNegative(longTaskDurationMs, "longTaskDurationMs");
    }

    public static ReportRuntimePerformanceSnapshot empty() {
        return new ReportRuntimePerformanceSnapshot(UNAVAILABLE, UNAVAILABLE, UNAVAILABLE,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                false, false);
    }

    /** Maps the stable JavaScript snapshot vocabulary without exposing raw maps to consumers. */
    public static ReportRuntimePerformanceSnapshot from(Map<?, ?> values) {
        Objects.requireNonNull(values, "values");
        return new ReportRuntimePerformanceSnapshot(
                optionalLong(values, "pageLoadMs"), optionalLong(values, "readinessMs"),
                optionalLong(values, "firstDisclosureToggleMs"),
                nonNegativeLong(values, "disclosureToggleDurationMs"),
                nonNegativeLong(values, "normalizationDurationMs"),
                nonNegativeLong(values, "normalizationSchedules"),
                nonNegativeLong(values, "normalizationRuns"),
                nonNegativeLong(values, "relevantMutationBatches"),
                nonNegativeLong(values, "durationMutationBatches"),
                nonNegativeLong(values, "disclosureToggles"),
                nonNegativeLong(values, "scenarioToggles"),
                nonNegativeLong(values, "phaseToggles"),
                nonNegativeLong(values, "nodeToggles"),
                nonNegativeLong(values, "longTaskCount"),
                nonNegativeLong(values, "longTaskDurationMs"),
                booleanValue(values, "longTaskObservationSupported"),
                booleanValue(values, "ready"));
    }

    public boolean readinessAvailable() {
        return ready && readinessMs != UNAVAILABLE;
    }

    public boolean firstDisclosureToggleAvailable() {
        return firstDisclosureToggleMs != UNAVAILABLE;
    }

    private static long optionalLong(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value == null) return UNAVAILABLE;
        return asLong(value, key);
    }

    private static long nonNegativeLong(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? 0 : asLong(value, key);
    }

    private static long asLong(Object value, String key) {
        if (value instanceof Number number) {
            long result = Math.round(number.doubleValue());
            if (result < 0) throw new IllegalArgumentException(key + " must not be negative");
            return result;
        }
        try {
            long result = Math.round(Double.parseDouble(value.toString()));
            if (result < 0) throw new IllegalArgumentException(key + " must not be negative");
            return result;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be numeric", error);
        }
    }

    private static boolean booleanValue(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value == null) return false;
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(value.toString());
    }

    private static void requireOptionalNonNegative(long value, String field) {
        if (value < UNAVAILABLE) throw new IllegalArgumentException(field + " must be -1 or non-negative");
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must be non-negative");
    }
}
