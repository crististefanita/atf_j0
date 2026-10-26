package atf.reporting.ui.audit.runtime;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ValidationResult;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class SectionAuditCheckPolicyTest {
    @Test
    void executesOnlyChecksDeclaredForEvidence() {
        ValidationResult full = ValidationResult.of(List.of(
                new AtfAssertion("Evidence tables", "visible", "visible", true),
                new AtfAssertion("Request JSON", "JSON", "{}", true),
                new AtfAssertion("Response JSON", "JSON", "{}", true)));

        ValidationResult selected = SectionAuditCheckPolicy.select(ExtentSection.EVIDENCE,
                EnumSet.of(ExtentSectionCheck.JSON_CONTRACT), full);

        assertTrue(selected.valid());
        assertEquals(List.of("Request JSON", "Response JSON"),
                selected.assertions().stream().map(AtfAssertion::name).toList());
    }

    @Test
    void failedAssertionsRemainFailedAfterCheckProjection() {
        ValidationResult full = ValidationResult.of(List.of(
                new AtfAssertion("Inline screenshot", "canonical link", "missing", false)));

        ValidationResult selected = SectionAuditCheckPolicy.select(ExtentSection.SCREEN_VALIDATION,
                EnumSet.of(ExtentSectionCheck.SCREENSHOT_LINK), full);

        assertTrue(!selected.valid());
        assertEquals(1, selected.assertions().size());
    }

    @Test
    void unselectedFailedCheckDoesNotPoisonTheSelectedContract() {
        ValidationResult full = ValidationResult.of(List.of(
                new AtfAssertion("Category surface", "styled", "unstyled", false),
                new AtfAssertion("Request JSON", "JSON", "{}", true)));

        ValidationResult selected = SectionAuditCheckPolicy.select(ExtentSection.EVIDENCE,
                EnumSet.of(ExtentSectionCheck.JSON_CONTRACT), full);

        assertTrue(selected.valid());
        assertEquals(List.of("Request JSON"),
                selected.assertions().stream().map(AtfAssertion::name).toList());
    }
}
