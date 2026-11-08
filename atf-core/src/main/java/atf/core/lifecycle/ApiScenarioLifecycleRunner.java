package atf.core.lifecycle;
import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Owns setup/body/teardown precedence and guarantees that teardown is attempted
 * after a setup or body failure/skip. Reporting remains behind AtfReportPorts.
 */
public final class ApiScenarioLifecycleRunner {
    public enum Phase { SETUP, TEST_BODY, TEARDOWN }

    @FunctionalInterface
    public interface ThrowingAction {
        void run() throws Exception;
    }

    public static class SkipSignal extends ApiScenarioSkipException {
        public SkipSignal(String message) { super(message); }
    }

    public static class WarningSignal extends RuntimeException {
        public WarningSignal(String message) { super(message); }
    }

    /**
     * Semantic failure signal used by adapters that already published the
     * failing evidence and must still let the lifecycle runner aggregate the
     * phase as FAIL without aborting teardown.
     */
    public static class FailureSignal extends RuntimeException {
        private final Map<String, String> evidence;

        public FailureSignal(String message) {
            this(message, Map.of());
        }

        public FailureSignal(String message, Map<String, String> evidence) {
            super(message);
            this.evidence = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(evidence == null ? Map.of() : evidence));
        }

        /** Compact, renderer-neutral fields shown beside the failure cause. */
        public Map<String, String> evidence() {
            return evidence;
        }
    }

    private final ApiScenarioPhaseExecutor phaseExecutor = new ApiScenarioPhaseExecutor();

    /** Compatibility-friendly runner for unit tests and simple JUnit scenarios. */
    public ApiScenarioOutcome run(ThrowingAction setup, ThrowingAction body, Consumer<Boolean> teardown) {
        var execution = new PhaseAccumulator();
        if (setup != null) execution.record(Phase.SETUP, phaseExecutor.execute(setup));
        if (body != null && execution.allowsBody()) {
            execution.record(Phase.TEST_BODY, phaseExecutor.execute(body));
        } else if (body != null) {
            execution.skipBody();
        }
        if (teardown != null) {
            execution.record(Phase.TEARDOWN, phaseExecutor.executeTeardown(teardown));
        }
        return execution.outcome();
    }

    public ApiScenarioOutcome run(AtfReportPorts.Session session, ApiScenarioPlan plan) {
        if (session == null || plan == null) throw new NullPointerException("session/plan");
        var scenario = session.startScenario(plan.testClass(), plan.testPackage(), plan.scenario(), plan.tags());
        var execution = new PhaseAccumulator();
        try {
            if (plan.setup() != null) {
                execution.record(Phase.SETUP, phaseExecutor.execute(scenario, "SETUP", plan.setup()));
            }
            if (plan.body() != null && execution.allowsBody()) {
                execution.record(Phase.TEST_BODY,
                        phaseExecutor.execute(scenario, "TEST BODY", plan.body()));
            } else if (plan.body() != null) {
                // Preserve the logical outcome without manufacturing a rendered
                // phase that was never entered. The report exposes only
                // phases reached by lifecycle execution.
                execution.skipBody();
            }
            if (plan.teardown() != null) {
                execution.record(Phase.TEARDOWN,
                        phaseExecutor.execute(scenario, "TEARDOWN", plan.teardown()));
            }
            try {
                scenario.complete(execution.status());
            } catch (Throwable completionFailure) {
                execution.recordFinalizationFailure(completionFailure);
            }
        } finally {
            try {
                scenario.close();
            } catch (Throwable closeFailure) {
                execution.recordFinalizationFailure(closeFailure);
            }
        }
        return execution.outcome();
    }

    /** Keeps precedence, phase order and primary-cause selection identical for both entry points. */
    private static final class PhaseAccumulator {
        private final LinkedHashMap<Phase, ApiScenarioOutcome.PhaseOutcome> phases = new LinkedHashMap<>();
        private AtfReportStatus status = AtfReportStatus.PASS;
        private Throwable primaryFailure;

        private void record(Phase phase, ApiScenarioPhaseExecutor.PhaseResult result) {
            phases.put(phase, result.outcome());
            status = AtfReportStatus.worst(status, result.status());
            if (primaryFailure == null && ApiScenarioPhaseExecutor.isPrimaryFailure(result.failure())) {
                primaryFailure = result.failure();
            }
        }

        private void skipBody() {
            phases.put(Phase.TEST_BODY, new ApiScenarioOutcome.PhaseOutcome(AtfReportStatus.SKIP, null));
            status = AtfReportStatus.worst(status, AtfReportStatus.SKIP);
        }

        private void recordFinalizationFailure(Throwable failure) {
            status = AtfReportStatus.worst(status, AtfReportStatus.FAIL);
            if (primaryFailure == null) primaryFailure = failure;
            else ApiScenarioPhaseExecutor.appendFailure(primaryFailure, failure);
        }

        private boolean allowsBody() {
            return status != AtfReportStatus.SKIP && status != AtfReportStatus.FAIL;
        }

        private AtfReportStatus status() { return status; }

        private ApiScenarioOutcome outcome() {
            return ApiScenarioOutcome.fromPhaseOutcomes(status, phases, primaryFailure);
        }
    }
}
