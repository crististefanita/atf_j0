package atf.core.lifecycle;
import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import java.util.function.Consumer;

/**
 * Executes one lifecycle phase and translates its control signals into the
 * renderer-neutral phase contract.  The runner owns ordering; this class owns
 * only phase invocation, signal mapping and phase cleanup.
 */
final class ApiScenarioPhaseExecutor {

    PhaseResult execute(ApiScenarioLifecycleRunner.ThrowingAction action) {
        if (action == null) return new PhaseResult(AtfReportStatus.PASS, null);
        try {
            action.run();
            return new PhaseResult(AtfReportStatus.PASS, null);
        } catch (ApiScenarioLifecycleRunner.SkipSignal skip) {
            return new PhaseResult(AtfReportStatus.SKIP, skip);
        } catch (ApiScenarioLifecycleRunner.WarningSignal warning) {
            return new PhaseResult(AtfReportStatus.WARNING, warning);
        } catch (Exception failure) {
            return new PhaseResult(AtfReportStatus.FAIL, failure);
        }
    }

    PhaseResult execute(AtfReportPorts.Scenario scenario, String name,
                        ApiScenarioPlan.PhaseAction action) {
        var phase = scenario.startPhase(name);
        var result = new PhaseResult(AtfReportStatus.PASS, null);
        try {
            action.execute(phase);
        } catch (ApiScenarioSkipException skip) {
            result = new PhaseResult(AtfReportStatus.SKIP, skip);
        } catch (ApiScenarioLifecycleRunner.WarningSignal warning) {
            result = new PhaseResult(AtfReportStatus.WARNING, warning);
        } catch (Throwable failure) {
            result = new PhaseResult(AtfReportStatus.FAIL, failure);
        } finally {
            if (result.status() == AtfReportStatus.FAIL && result.failure() != null) {
                publishFailureEvidence(phase, name, result.failure());
            }
            phase.complete(result.status());
            phase.close();
        }
        return result;
    }

    private static void publishFailureEvidence(AtfReportPorts.Phase phase,
                                               String phaseName,
                                               Throwable failure) {
        if (failure instanceof ApiScenarioLifecycleRunner.FailureSignal signal) {
            phase.detail("Failure cause", signal.getMessage());
            signal.evidence().forEach(phase::detail);
            return;
        }
        String message = failure.getMessage() == null || failure.getMessage().isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getMessage();
        phase.detail("Failure cause", phaseName + " failure: " + message);
    }

    PhaseResult executeTeardown(Consumer<Boolean> teardown) {
        try {
            teardown.accept(true);
            return new PhaseResult(AtfReportStatus.PASS, null);
        } catch (Exception failure) {
            return new PhaseResult(AtfReportStatus.FAIL, failure);
        }
    }

    static boolean isPrimaryFailure(Throwable failure) {
        return failure != null
                && !(failure instanceof ApiScenarioLifecycleRunner.WarningSignal)
                && !(failure instanceof ApiScenarioSkipException);
    }

    record PhaseResult(AtfReportStatus status, Throwable failure) {
        ApiScenarioOutcome.PhaseOutcome outcome() {
            return new ApiScenarioOutcome.PhaseOutcome(status, failure);
        }
    }
}
