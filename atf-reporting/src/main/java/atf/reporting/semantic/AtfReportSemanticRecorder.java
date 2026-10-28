package atf.reporting.semantic;
import atf.reporting.contract.evidence.ReportEvidenceOwner;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;

import java.util.Collection;
import java.util.Objects;

/**
 * Public lifecycle facade for the renderer-neutral semantic population.
 *
 * <p>The facade preserves the fluent handles used by Extent and fallback
 * sessions. Mutable state is owned by {@link AtfReportSemanticLifecycle};
 * callers receive only the immutable snapshot at inspection boundaries.</p>
 */
public final class AtfReportSemanticRecorder {
    private final AtfReportSemanticLifecycle lifecycle = new AtfReportSemanticLifecycle();

    public ScenarioHandle startScenario(String testClass, String testPackage,
                                        String scenario, Collection<String> tags) {
        return new ScenarioHandle(lifecycle.startScenario(testClass, testPackage, scenario, tags));
    }

    public AtfReportSemanticSnapshot snapshot() {
        return lifecycle.snapshot();
    }

    public final class ScenarioHandle {
        private final AtfReportSemanticState.ScenarioState state;

        private ScenarioHandle(AtfReportSemanticState.ScenarioState state) {
            this.state = state;
        }

        public String id() { return state.id(); }

        public PhaseHandle startPhase(String name) {
            return new PhaseHandle(state.startPhase(name));
        }

        public void complete(AtfReportStatus status) {
            state.complete(status);
        }
    }

    public final class PhaseHandle {
        public void executionTiming(atf.reporting.model.timing.ReportPhaseTiming timing) { state.executionTiming(timing); }
        private final AtfReportSemanticState.PhaseState state;

        private PhaseHandle(AtfReportSemanticState.PhaseState state) {
            this.state = state;
        }

        public RequestHandle startRequest(String title, String method, String path) {
            return new RequestHandle(state.startRequest(title, method, path));
        }

        public AssertionStepHandle startAssertionStep(String title) {
            return new AssertionStepHandle(state.startAssertionStep(title));
        }

        public void detail(String name) {
            state.detail(name);
        }

        public void complete(AtfReportStatus status) {
            state.complete(status);
        }
    }

    public final class AssertionStepHandle {
        private final AtfReportSemanticState.AssertionStepState state;

        private AssertionStepHandle(AtfReportSemanticState.AssertionStepState state) {
            this.state = state;
        }

        public void assertions(Collection<AtfAssertion> assertions) { state.assertions(assertions); }
        public void complete(AtfReportStatus status) { state.complete(status); }
    }

    public final class RequestHandle {
        private final AtfReportSemanticState.RequestState state;

        private RequestHandle(AtfReportSemanticState.RequestState state) {
            this.state = state;
        }

        public void detail(String name) {
            state.detail(name);
        }

        public void json(String label) {
            state.json(label);
        }

        public void execution() {
            state.execution();
        }

        public void uiActions(Collection<AtfUiActionRecord> actions) {
            state.uiActions(actions);
        }

        public void screenshot() {
            state.screenshot();
        }

        public ValidationHandle startValidation(String title) {
            return new ValidationHandle(state.startValidation(title));
        }

        public void complete(AtfReportStatus status) {
            state.complete(status);
        }
    }

    public final class ValidationHandle {
        private final AtfReportSemanticState.ValidationState state;

        private ValidationHandle(AtfReportSemanticState.ValidationState state) {
            this.state = state;
        }

        public void evidenceOwner(ReportEvidenceOwner owner) { state.evidenceOwner(owner); }

        public void assertions(Collection<AtfAssertion> assertions) {
            state.assertions(assertions);
        }

        public void uiActions(Collection<AtfUiActionRecord> actions) {
            state.uiActions(actions);
        }

        public void screenshot() {
            state.screenshot();
        }

        public RequestHandle startRequest(String title, String method, String path) {
            return new RequestHandle(state.startRequest(title, method, path));
        }

        public void complete(AtfReportStatus status) {
            state.complete(Objects.requireNonNull(status, "status"));
        }
    }
}
