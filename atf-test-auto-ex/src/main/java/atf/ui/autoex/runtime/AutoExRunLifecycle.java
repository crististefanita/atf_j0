package atf.ui.autoex.runtime;

import java.util.Objects;
import java.util.function.Consumer;

/** Runtime completion port; the caller owns acquisition, failure ordering and resource release. */
@FunctionalInterface
public interface AutoExRunLifecycle {
    Throwable finish(Throwable primary, Completion completion);

    @FunctionalInterface
    interface Work { void run() throws Exception; }

    record Completion(Work cleanup, Work diagnostics, Consumer<Throwable> evidence, Work closeBrowser,
                      Consumer<Boolean> completePhase, Consumer<Boolean> completeScenario) {
        public Completion {
            Objects.requireNonNull(cleanup, "cleanup");
            Objects.requireNonNull(diagnostics, "diagnostics");
            Objects.requireNonNull(evidence, "evidence");
            Objects.requireNonNull(closeBrowser, "closeBrowser");
            Objects.requireNonNull(completePhase, "completePhase");
            Objects.requireNonNull(completeScenario, "completeScenario");
        }
    }
}
