package atf.ui.tests.extent.component.inspection;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.component.inspection.duration.ApiReportDurationProjectionPolicy;

import atf.reporting.audit.ReportAuditSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiReportDurationProjectionPolicyTest {
    @Test
    void acceptsExplicitEmptyCategoriesAndFailsButRequiresAllProjectionNames() {
        var policy = new ApiReportDurationProjectionPolicy();
        var projections = List.of(
                new ReportAuditSnapshot.DurationProjection("Tests", 1, 1, List.of()),
                new ReportAuditSnapshot.DurationProjection("Overview", 1, 1, List.of()),
                new ReportAuditSnapshot.DurationProjection("Categories", 0, 0, List.of()),
                new ReportAuditSnapshot.DurationProjection("Fails", 0, 0, List.of()));
        assertTrue(policy.validate(projections).valid());
        assertFalse(policy.validate(projections.subList(0, 3)).valid());
    }
}
