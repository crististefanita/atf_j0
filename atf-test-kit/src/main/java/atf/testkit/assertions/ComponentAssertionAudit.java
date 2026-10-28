package atf.testkit.assertions;

import org.opentest4j.TestAbortedException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/** Renderer-independent result policy and evaluated checks for one JUnit invocation. */
public final class ComponentAssertionAudit {
    public enum Phase { SETUP, TEST_BODY, TEARDOWN }
    public enum Outcome { PASS, FAIL, ABORTED }
    public record PhaseResult(Phase phase, List<RecordedAssertions.Check> checks,
                              Outcome outcome, String diagnostic) { }
    public record Result(List<PhaseResult> phases, Outcome outcome, Throwable failure,
                         boolean propagate) { }

    private final AssertionValueDisplay display;
    private final EnumMap<Phase, RecordedAssertions> recorders = new EnumMap<>(Phase.class);
    private final EnumMap<Phase, Throwable> failures = new EnumMap<>(Phase.class);
    private final java.util.Set<Phase> started = java.util.EnumSet.noneOf(Phase.class);

    public ComponentAssertionAudit(AssertionValueDisplay display) {
        this.display = Objects.requireNonNull(display, "display");
        for (Phase phase : Phase.values()) recorders.put(phase, new RecordedAssertions(display));
    }

    public RecordedAssertions recorder(Phase phase) { return recorders.get(Objects.requireNonNull(phase)); }
    public void start(Phase phase) { started.add(Objects.requireNonNull(phase)); }
    public void failed(Phase phase, Throwable failure) { failures.putIfAbsent(phase, failure); }

    public Result finish(Throwable executionFailure) {
        Throwable unconsumed = recorders.values().stream().map(RecordedAssertions::failure)
                .filter(Objects::nonNull).findFirst().orElse(null);
        Throwable failure = executionFailure;
        boolean propagate = false;
        if (unconsumed != null && (failure == null || failure instanceof TestAbortedException)) {
            if (failure != null && failure != unconsumed) unconsumed.addSuppressed(failure);
            failure = unconsumed;
            propagate = true;
        }
        boolean empty = recorders.values().stream().allMatch(recorder -> recorder.checks().isEmpty());
        if (failure == null && empty) {
            failure = new AssertionError("Component test passed without an evaluated audited assertion");
            failures.put(Phase.TEST_BODY, failure);
            started.add(Phase.TEST_BODY);
            propagate = true;
        }
        if (failure != null && failures.isEmpty() && unconsumed == null) {
            Phase phase = started.contains(Phase.TEST_BODY) ? Phase.TEARDOWN : Phase.SETUP;
            failures.put(phase, failure);
            started.add(phase);
        }
        var phases = new ArrayList<PhaseResult>();
        for (Phase phase : Phase.values()) {
            var recorder = recorder(phase);
            Throwable phaseFailure = failures.get(phase);
            if (!started.contains(phase) && recorder.checks().isEmpty() && phaseFailure == null) continue;
            Outcome outcome = recorder.checks().stream().anyMatch(check -> !check.passed())
                    ? Outcome.FAIL : outcome(phaseFailure);
            String diagnostic = phaseFailure == null ? "" : display.display(null,
                    (AssertionValueDisplay.Value) phaseFailure::toString);
            phases.add(new PhaseResult(phase, recorder.checks(), outcome, diagnostic));
        }
        return new Result(List.copyOf(phases), outcome(failure), failure, propagate);
    }

    public static Outcome outcome(Throwable failure) {
        return failure == null ? Outcome.PASS
                : failure instanceof TestAbortedException ? Outcome.ABORTED : Outcome.FAIL;
    }
}
