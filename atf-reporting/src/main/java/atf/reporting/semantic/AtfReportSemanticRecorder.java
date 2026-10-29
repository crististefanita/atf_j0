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
    private final Runnable ensureSessionOpen;

    public AtfReportSemanticRecorder() { this(() -> { }); }
    public AtfReportSemanticRecorder(Runnable ensureSessionOpen) {
        this.ensureSessionOpen = Objects.requireNonNull(ensureSessionOpen, "ensureSessionOpen");
    }

    public ScenarioHandle startScenario(String testClass, String testPackage,
                                        String scenario, Collection<String> tags) {
        ensureSessionOpen.run();
        return new ScenarioHandle(lifecycle.startScenario(testClass, testPackage, scenario, tags));
    }

    public AtfReportSemanticSnapshot snapshot() {
        return lifecycle.snapshot();
    }

    public final class ScenarioHandle {
        public void ensureOpen() { ensureSessionOpen.run(); state.ensureOpen("scenario"); }
        public boolean isComplete() { return state.completed(); }

        private final AtfReportSemanticState.ScenarioState state;

        private ScenarioHandle(AtfReportSemanticState.ScenarioState state) {
            this.state = state;
        }

        public String id() { return state.id(); }

        public PhaseHandle startPhase(String name) {
            ensureOpen();
            return new PhaseHandle(state.startPhase(name));
        }

        public void complete(AtfReportStatus status) {
            ensureOpen();
            state.complete(status);
        }
    }

    public final class PhaseHandle {
        public void ensureOpen() { ensureSessionOpen.run(); state.ensureOpen("phase"); }
        public boolean isComplete() { return state.completed(); }

        public void executionTiming(atf.reporting.model.timing.ReportPhaseTiming timing) {
            ensureOpen();
            state.executionTiming(timing);
        }
        private final AtfReportSemanticState.PhaseState state;

        private PhaseHandle(AtfReportSemanticState.PhaseState state) {
            this.state = state;
        }

        public RequestHandle startRequest(String title, String method, String path) {
            ensureOpen();
            return new RequestHandle(state.startRequest(title, method, path));
        }

        public AssertionStepHandle startAssertionStep(String title) {
            ensureOpen();
            return new AssertionStepHandle(state.startAssertionStep(title));
        }

        public void detail(String name) {
            ensureOpen();
            state.detail(name);
        }

        public void complete(AtfReportStatus status) {
            ensureOpen();
            state.complete(status);
        }
    }

    public final class AssertionStepHandle {
        public void ensureOpen() { ensureSessionOpen.run(); state.ensureOpen(); }
        public boolean isComplete() { return state.completed(); }

        private final AtfReportSemanticState.AssertionStepState state;

        private AssertionStepHandle(AtfReportSemanticState.AssertionStepState state) {
            this.state = state;
        }

        public void assertions(Collection<AtfAssertion> assertions) {
            ensureOpen();
            state.assertions(assertions);
        }
        public void complete(AtfReportStatus status) {
            ensureOpen();
            state.complete(status);
        }
    }

    public final class RequestHandle {
        public void ensureOpen() { ensureSessionOpen.run(); state.ensureOpen(); }
        public boolean isComplete() { return state.completed(); }

        private final AtfReportSemanticState.RequestState state;

        private RequestHandle(AtfReportSemanticState.RequestState state) {
            this.state = state;
        }

        public void detail(String name) {
            ensureOpen();
            state.detail(name);
        }

        public void json(String label) {
            ensureOpen();
            state.json(label);
        }

        public void execution() {
            ensureOpen();
            state.execution();
        }

        public void networkAggregate() {
            ensureOpen();
            state.networkAggregate();
        }

        public void uiActions(Collection<AtfUiActionRecord> actions) {
            ensureOpen();
            state.uiActions(actions);
        }

        public void screenshot() {
            ensureOpen();
            state.screenshot();
        }

        public ValidationHandle startValidation(String title) {
            ensureOpen();
            return new ValidationHandle(state.startValidation(title));
        }

        public void complete(AtfReportStatus status) {
            ensureOpen();
            state.complete(status);
        }
    }

    public final class ValidationHandle {
        public void ensureOpen() { ensureSessionOpen.run(); state.ensureOpen(); }
        public boolean isComplete() { return state.completed(); }

        private final AtfReportSemanticState.ValidationState state;

        private ValidationHandle(AtfReportSemanticState.ValidationState state) {
            this.state = state;
        }

        public void evidenceOwner(ReportEvidenceOwner owner) {
            ensureOpen();
            state.evidenceOwner(owner);
        }

        public void assertions(Collection<AtfAssertion> assertions) {
            ensureOpen();
            state.assertions(assertions);
        }

        public void uiActions(Collection<AtfUiActionRecord> actions) {
            ensureOpen();
            state.uiActions(actions);
        }

        public void screenshot() {
            ensureOpen();
            state.screenshot();
        }

        public RequestHandle startRequest(String title, String method, String path) {
            ensureOpen();
            return new RequestHandle(state.startRequest(title, method, path));
        }

        public void complete(AtfReportStatus status) {
            ensureOpen();
            state.complete(Objects.requireNonNull(status, "status"));
        }
    }
}
