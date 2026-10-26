package atf.reporting.ui.audit.section;
import atf.testkit.AtfUnitTest;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ReportProjection;
import atf.reporting.contract.AtfReportStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class FailsSectionValidatorTest {
    private final FailsSectionValidator validator = new FailsSectionValidator();

    @Test
    void acceptsActionableFailureRows() {
        assertTrue(validator.validate(new ReportAuditSnapshot.Failures(2, 2, 2, false)).valid());
    }

    @Test
    void acceptsAnHonestEmptyState() {
        assertTrue(validator.validate(new ReportAuditSnapshot.Failures(0, 0, 0, true)).valid());
        assertFalse(validator.validate(new ReportAuditSnapshot.Failures(0, 1, 0, true)).valid());
    }

    @Test
    void reconcilesFailsRowsWithExpectedScenarioStatuses() {
        var projection = new ReportProjection(
                List.of(new ReportProjection.Owner("LifecycleApiScenarios", "LIFECYCLE")),
                List.of("Lifecycle failure", "Lifecycle setup failure"), 2, 2, 2,
                List.of("Request details"), List.of("Tests", "Fails"),
                List.of(AtfReportStatus.FAIL, AtfReportStatus.WARNING));

        assertTrue(validator.validate(new ReportAuditSnapshot.Failures(1, 1, 1, false), projection).valid());
        assertFalse(validator.validate(new ReportAuditSnapshot.Failures(0, 0, 0, true), projection).valid());
    }
}
