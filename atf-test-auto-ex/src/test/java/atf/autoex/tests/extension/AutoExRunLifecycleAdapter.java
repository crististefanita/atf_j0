package atf.autoex.tests.extension;

import atf.testkit.lifecycle.FailureAwareCleanup;
import atf.testkit.lifecycle.FailureEvidenceCleanup;
import atf.autoex.runtime.AutoExRunLifecycle;

/** Reuses the neutral Kit ordering without bringing Kit into the application runtime. */
public enum AutoExRunLifecycleAdapter implements AutoExRunLifecycle {
    INSTANCE;

    @Override
    public Throwable finish(Throwable primary, Completion stages) {
        Throwable failure = FailureEvidenceCleanup.finish(primary, stages.cleanup()::run,
                stages.diagnostics()::run, stages.evidence()::accept, stages.closeBrowser()::run);
        boolean phaseFailed = failure != null;
        failure = FailureAwareCleanup.runAll(failure, () -> stages.completePhase().accept(phaseFailed));
        boolean scenarioFailed = failure != null;
        return FailureAwareCleanup.runAll(failure, () -> stages.completeScenario().accept(scenarioFailed));
    }
}
