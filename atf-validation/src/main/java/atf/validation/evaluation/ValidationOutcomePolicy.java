package atf.validation.evaluation;

/**
 * Decides how a completed validation table becomes a public ATF outcome.
 *
 * <p>The table owns evidence collection; the policy owns the Pass/Warning/Fail
 * decision. This keeps warning-mode semantics explicit and independently
 * testable without Extent or a live HTTP call.</p>
 */
@FunctionalInterface
public interface ValidationOutcomePolicy {
    ValidationDecision decide(ValidationTable table);

    static ValidationOutcomePolicy strict() {
        return DefaultValidationOutcomePolicy.STRICT;
    }

    static ValidationOutcomePolicy warning() {
        return DefaultValidationOutcomePolicy.WARNING;
    }
}
