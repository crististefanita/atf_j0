package atf.reporting.audit;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ValidationResultTest {
    @Test
    void adaptsAndMergesTypedAuditorOutcomes() {
        ValidationResult valid = ValidationResult.ofViolations(List.of());
        ValidationResult invalid = ValidationResult.ofViolations(List.of("missing response JSON"));

        assertTrue(valid.valid());
        assertFalse(ValidationResult.merge(valid, invalid).valid());
        assertTrue(ValidationResult.merge(valid, invalid).failureSummary().contains("missing response JSON"));
    }
}
