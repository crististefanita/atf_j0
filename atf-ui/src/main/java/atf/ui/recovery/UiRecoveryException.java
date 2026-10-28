package atf.ui.recovery;

/** Terminal refusal/exhaustion; callers keep their original action failure when it already exists. */
public final class UiRecoveryException extends IllegalStateException {
    private final UiRecoveryTrace trace;
    public UiRecoveryException(UiRecoveryTrace trace) {
        super("BE recovery " + trace.decision() + " for " + trace.operation() + ": " + trace.reason());
        this.trace = trace;
    }
    public UiRecoveryTrace trace() { return trace; }
}
