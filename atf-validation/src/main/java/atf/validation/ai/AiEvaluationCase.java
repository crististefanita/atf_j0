package atf.validation.ai;

import java.util.HashSet;
import java.util.List;

/** Approved, immutable expectations; deliberately constructed independently of observed output. */
public record AiEvaluationCase(String caseId, int version, String inputReference,
                               List<AiFieldExpectation> expectations) {
    public AiEvaluationCase {
        if (caseId == null || caseId.isBlank()) throw new IllegalArgumentException("caseId must not be blank");
        if (version <= 0) throw new IllegalArgumentException("version must be positive");
        if (inputReference == null || inputReference.isBlank() || inputReference.length() > 160
                || inputReference.chars().anyMatch(Character::isWhitespace)
                || inputReference.chars().anyMatch(Character::isISOControl) || inputReference.contains("://")) {
            throw new IllegalArgumentException("inputReference must be a bounded opaque fixture identifier");
        }
        expectations = List.copyOf(expectations);
        if (expectations.isEmpty()) throw new IllegalArgumentException("at least one expectation is required");
        var paths = new HashSet<String>();
        for (var expectation : expectations) {
            if (!paths.add(expectation.fieldPath())) {
                throw new IllegalArgumentException("duplicate expected field: " + expectation.fieldPath());
            }
        }
    }
}
