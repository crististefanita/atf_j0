package atf.core.lifecycle;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/** Immutable outcome returned by the lifecycle coordinator. */
public final class ApiScenarioOutcome {
    private final AtfReportStatus status;
    private final Map<ApiScenarioLifecycleRunner.Phase, AtfReportStatus> phases;
    private final Map<ApiScenarioLifecycleRunner.Phase, PhaseOutcome> phaseOutcomes;
    private final Throwable primaryFailure;

    public ApiScenarioOutcome(AtfReportStatus status,
                              Map<ApiScenarioLifecycleRunner.Phase, AtfReportStatus> phases,
                              Throwable primaryFailure) {
        this.status = status == null ? AtfReportStatus.FAIL : status;
        // Preserve lifecycle order for report projections and callers iterating the outcome.
        // Map.copyOf is immutable but deliberately does not promise insertion order.
        this.phases = Collections.unmodifiableMap(
                new LinkedHashMap<>(phases == null ? Map.of() : phases));
        var semantic = new LinkedHashMap<ApiScenarioLifecycleRunner.Phase, PhaseOutcome>();
        this.phases.forEach((phase, phaseStatus) ->
                semantic.put(phase, new PhaseOutcome(phaseStatus, null)));
        this.phaseOutcomes = Collections.unmodifiableMap(semantic);
        this.primaryFailure = primaryFailure;
    }

    private ApiScenarioOutcome(AtfReportStatus status,
                               Map<ApiScenarioLifecycleRunner.Phase, PhaseOutcome> outcomes,
                               Throwable primaryFailure,
                               boolean semantic) {
        this.status = status == null ? AtfReportStatus.FAIL : status;
        var semanticCopy = new LinkedHashMap<>(outcomes == null ? Map.of() : outcomes);
        this.phaseOutcomes = Collections.unmodifiableMap(semanticCopy);
        var statuses = new LinkedHashMap<ApiScenarioLifecycleRunner.Phase, AtfReportStatus>();
        semanticCopy.forEach((phase, outcome) -> statuses.put(phase, outcome.status()));
        this.phases = Collections.unmodifiableMap(statuses);
        this.primaryFailure = primaryFailure;
    }

    static ApiScenarioOutcome fromPhaseOutcomes(AtfReportStatus status,
                                                Map<ApiScenarioLifecycleRunner.Phase, PhaseOutcome> outcomes,
                                                Throwable primaryFailure) {
        return new ApiScenarioOutcome(status, outcomes, primaryFailure, true);
    }

    public AtfReportStatus status() { return status; }
    public Map<ApiScenarioLifecycleRunner.Phase, AtfReportStatus> phases() { return phases; }
    /**
     * Returns the immutable semantic result for every executed or skipped phase.
     * The failure is retained as evidence even when it is not the primary
     * scenario failure (for example, a teardown failure after a body failure).
     */
    public Map<ApiScenarioLifecycleRunner.Phase, PhaseOutcome> phaseOutcomes() {
        return phaseOutcomes;
    }
    public Throwable primaryFailure() { return primaryFailure; }
    public boolean passed() { return status == AtfReportStatus.PASS; }
    public boolean failed() { return status == AtfReportStatus.FAIL; }

    public record PhaseOutcome(AtfReportStatus status, Throwable failure) {
        public PhaseOutcome {
            status = status == null ? AtfReportStatus.FAIL : status;
        }

        public boolean failed() { return status == AtfReportStatus.FAIL; }
        public boolean skipped() { return status == AtfReportStatus.SKIP; }
        public boolean warning() { return status == AtfReportStatus.WARNING; }
    }
}
