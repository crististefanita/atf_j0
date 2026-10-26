package atf.ui.tests.extent.component.audit;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.audit.ValidationResult;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.audit.screen.CategoryOutcomeFlowAuditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fast renderer-neutral coverage for the complete Categories status matrix.
 * DOM mechanics remain covered by the existing selected-report flow; this
 * component contract proves every mixed outcome is rendered as valid evidence
 * and that one broken projection is reported precisely.
 */
@AtfComponentTest
class CategoryOutcomeFlowAuditorTest {
    private static final List<AtfReportStatus> STATUSES = List.of(
            AtfReportStatus.PASS, AtfReportStatus.FAIL, AtfReportStatus.WARNING,
            AtfReportStatus.SKIP);

    @Test
    void acceptsPassFailWarningSkipCombinationAndClearConvergence() {
        CategoryOutcomeFlowAuditor.Observation observation = new CategoryOutcomeFlowAuditor.Observation(
                4, validSelections(), clearSelection(4), true);

        ValidationResult result = CategoryOutcomeFlowAuditor.validateObservation(observation);

        assertTrue(result.valid(), result::failureSummary);
        assertEquals(5, result.assertions().size(),
                "one assertion per status plus the final Clear convergence assertion");
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void identifiesTheBrokenStatusInAOtherwiseValidCombination() {
        List<CategoryOutcomeFlowAuditor.StatusSelection> selections = validSelections().stream()
                .map(selection -> selection.filter() == CategoryOutcomeFlowAuditor.CategoryFilter.WARNING
                        ? new CategoryOutcomeFlowAuditor.StatusSelection(
                        CategoryOutcomeFlowAuditor.CategoryFilter.WARNING,
                        true, 1, 0, true, true, List.of("api"))
                        : selection)
                .toList();
        CategoryOutcomeFlowAuditor.Observation observation = new CategoryOutcomeFlowAuditor.Observation(
                4, selections, clearSelection(4), true);

        ValidationResult result = CategoryOutcomeFlowAuditor.validateObservation(observation);

        assertFalse(result.valid());
        assertEquals(1, result.violations().stream()
                .filter(violation -> violation.startsWith("Categories Warning filter"))
                .count());
        assertTrue(result.violations().stream().noneMatch(
                violation -> violation.startsWith("Categories Pass filter")));
    }

    @Test
    void rejectsUnsupportedOrEmptyStatusProjectionWithoutAFalsePass() {
        for (AtfReportStatus status : STATUSES) {
            List<CategoryOutcomeFlowAuditor.StatusSelection> selections = validSelections().stream()
                    .map(selection -> selection.filter().status() == status
                            ? new CategoryOutcomeFlowAuditor.StatusSelection(
                            CategoryOutcomeFlowAuditor.CategoryFilter.fromStatus(status),
                            false, 0, 0, false, false, List.of())
                            : selection)
                    .toList();
            ValidationResult result = CategoryOutcomeFlowAuditor.validateObservation(
                    new CategoryOutcomeFlowAuditor.Observation(4, selections, clearSelection(4), true));

            assertFalse(result.valid(), "an unsupported " + status.displayValue()
                    + " projection must fail validation");
            assertTrue(result.violations().stream().anyMatch(
                    violation -> violation.startsWith("Categories " + status.displayLabel() + " filter")),
                    "the failing status must be named: " + status.displayValue());
        }
    }

    private static List<CategoryOutcomeFlowAuditor.StatusSelection> validSelections() {
        return STATUSES.stream()
                .map(status -> new CategoryOutcomeFlowAuditor.StatusSelection(
                        CategoryOutcomeFlowAuditor.CategoryFilter.fromStatus(status),
                        true, status == AtfReportStatus.FAIL ? 2 : 1,
                        status == AtfReportStatus.FAIL ? 3 : 2, true, true,
                        List.of("api")))
                .toList();
    }

    private static CategoryOutcomeFlowAuditor.StatusSelection clearSelection(int categories) {
        return new CategoryOutcomeFlowAuditor.StatusSelection(
                CategoryOutcomeFlowAuditor.CategoryFilter.CLEAR,
                true, categories, categories * 2, true, true, List.of("api", "users"));
    }
}
