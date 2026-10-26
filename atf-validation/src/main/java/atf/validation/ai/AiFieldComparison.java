package atf.validation.ai;

/** One deterministic field comparison with a stable reason code. */
public record AiFieldComparison(
        String fieldPath,
        Object expected,
        Object observed,
        AiFieldExpectation.ComparisonMode comparisonMode,
        Outcome outcome,
        AiFieldExpectation.InputPresence sourcePresence,
        boolean critical,
        int weight,
        String reasonCode,
        String evidenceReference) {

    public AiFieldComparison {
        expected = AiEvaluationValues.immutable(expected);
        observed = AiEvaluationValues.immutable(observed);
    }

    public enum Outcome { MATCH, MISMATCH }
}
