package atf.validation.ai;

/** Explicit acceptance threshold for the deterministic evaluator's first increment. */
public record AiEvaluationPolicy(int minimumPassingScore, boolean rejectUndeclaredOutputFields) {
    public AiEvaluationPolicy {
        if (minimumPassingScore < 0 || minimumPassingScore > 100) {
            throw new IllegalArgumentException("minimumPassingScore must be between 0 and 100");
        }
    }

    public AiEvaluationPolicy(int minimumPassingScore) {
        this(minimumPassingScore, true);
    }

    public static AiEvaluationPolicy defaultPolicy() {
        return new AiEvaluationPolicy(80, true);
    }
}
