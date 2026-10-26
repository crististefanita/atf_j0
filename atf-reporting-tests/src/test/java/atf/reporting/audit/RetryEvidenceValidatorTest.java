package atf.reporting.audit;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class RetryEvidenceValidatorTest {
    private final RetryEvidenceValidator validator = new RetryEvidenceValidator();

    @Test
    void acceptsSequentialHistoryWithFinalStatusReconciliation() {
        var snapshot = new RetryEvidenceSnapshot(2, 1, 200, List.of(
                new RetryEvidenceSnapshot.Attempt(1, 503, "RETRYABLE_RESPONSE"),
                new RetryEvidenceSnapshot.Attempt(2, 200, "SUCCESS")));

        assertTrue(validator.validate(snapshot).isEmpty());
    }

    @Test
    void rejectsNonSequentialHistoryAndInvalidStatus() {
        var snapshot = new RetryEvidenceSnapshot(2, 1, 700, List.of(
                new RetryEvidenceSnapshot.Attempt(2, 503, "RETRYABLE_RESPONSE"),
                new RetryEvidenceSnapshot.Attempt(4, 0, "")));

        var violations = validator.validate(snapshot);

        assertTrue(violations.stream().anyMatch(v -> v.contains("sequential")));
        assertTrue(violations.stream().anyMatch(v -> v.contains("valid HTTP status")));
        assertTrue(violations.stream().anyMatch(v -> v.contains("outcome")));
    }
}
