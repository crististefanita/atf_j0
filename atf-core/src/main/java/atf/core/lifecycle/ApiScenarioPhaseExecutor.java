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
        AtfReportPorts.Phase phase;
        try {
            phase = scenario.startPhase(name);
        } catch (Throwable failure) {
            return new PhaseResult(AtfReportStatus.FAIL, failure);
        }
        var result = new PhaseResult(AtfReportStatus.PASS, null);
        try {
            action.execute(phase);
        } catch (ApiScenarioSkipException skip) {
            result = new PhaseResult(AtfReportStatus.SKIP, skip);
        } catch (ApiScenarioLifecycleRunner.WarningSignal warning) {
            result = new PhaseResult(AtfReportStatus.WARNING, warning);
        } catch (Throwable failure) {
            result = new PhaseResult(AtfReportStatus.FAIL, failure);
        }

        Throwable primaryFailure = isPrimaryFailure(result.failure()) ? result.failure() : null;
        if (result.status() == AtfReportStatus.FAIL && primaryFailure != null) {
            primaryFailure = publishFailureEvidence(phase, name, primaryFailure);
        }

        AtfReportStatus status = primaryFailure == null ? result.status() : AtfReportStatus.FAIL;
        try {
            phase.complete(status);
        } catch (Throwable completionFailure) {
            primaryFailure = appendFailure(primaryFailure, completionFailure);
            status = AtfReportStatus.FAIL;
        }
        try {
            phase.close();
        } catch (Throwable closeFailure) {
            primaryFailure = appendFailure(primaryFailure, closeFailure);
            status = AtfReportStatus.FAIL;
        }
        return new PhaseResult(status, primaryFailure == null ? result.failure() : primaryFailure);
    }

    private static Throwable publishFailureEvidence(AtfReportPorts.Phase phase,
                                                    String phaseName,
                                                    Throwable failure) {
        boolean detailsSupported;
        try {
            detailsSupported = phase.supportsDetails();
        } catch (Throwable capabilityFailure) {
            return appendFailure(failure, capabilityFailure);
        }
        if (!detailsSupported) {
            return appendFailure(failure, new UnsupportedOperationException(
                    "Phase report port does not retain failure details"));
        }
        if (failure instanceof ApiScenarioLifecycleRunner.FailureSignal signal) {
            Throwable result = publishDetail(phase, "Failure cause", signal.getMessage(), failure);
            for (var item : signal.evidence().entrySet()) {
                result = publishDetail(phase, item.getKey(), item.getValue(), result);
            }
            return result;
        }
        String message = failure.getMessage() == null || failure.getMessage().isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getMessage();
        return publishDetail(phase, "Failure cause", phaseName + " failure: " + message, failure);
    }

    private static Throwable publishDetail(AtfReportPorts.Phase phase, String name, String value,
                                           Throwable primaryFailure) {
        try {
            phase.detail(name, value);
            return primaryFailure;
        } catch (Throwable publicationFailure) {
            return appendFailure(primaryFailure, publicationFailure);
        }
    }

    static Throwable appendFailure(Throwable primary, Throwable secondary) {
        if (primary == null) return secondary;
        if (secondary != null && secondary != primary) primary.addSuppressed(secondary);
        return primary;
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
