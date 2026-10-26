package atf.reporting.ui.audit.section;
import atf.testkit.AtfUnitTest;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.ui.audit.plan.ReportProjection;
import atf.reporting.ui.audit.projection.ReportProjectionValidator;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportProjectionValidatorTest {
    private static final ReportProjection PROJECTION = new ReportProjection(
            List.of(new ReportProjection.Owner("UsersApiTests", "USERS"),
                    new ReportProjection.Owner("PostsApiTests", "POSTS")),
            List.of("GET users", "POST posts"), 2, 2, 2,
            List.of("Request details", "Response details"),
            List.of("Tests", "Overview", "Categories", "Fails"));

    @Test
    void acceptsAnExactProjectionBeforeBrowserInspection() {
        var snapshot = new ReportAuditSnapshot.Tests(2, 2, 2, 2, 2,
                List.of("UsersApiTests", "PostsApiTests"), List.of("GET users", "POST posts"));

        assertTrue(new ReportProjectionValidator().validate(PROJECTION, snapshot).valid());
    }

    @Test
    void rejectsExtraOwnersAndScenarioTitles() {
        var snapshot = new ReportAuditSnapshot.Tests(3, 3, 3, 3, 3,
                List.of("UsersApiTests", "PostsApiTests", "LifecycleApiScenarios"),
                List.of("GET users", "POST posts", "GET lifecycle"));

        assertFalse(new ReportProjectionValidator().validate(PROJECTION, snapshot).valid());
    }

    @Test
    void rejectsAStatusProjectionMismatchEvenWhenTheShapeMatches() {
        var expected = new ReportProjection(
                List.of(new ReportProjection.Owner("UsersApiTests", "USERS")),
                List.of("GET users"), 1, 1, 1,
                List.of("Request details"), List.of("Tests", "Fails"),
                List.of(AtfReportStatus.WARNING));
        var observed = new ReportAuditSnapshot.Tests(1, 1, 1, 1, 1,
                List.of("UsersApiTests"), List.of("GET users"), List.of(AtfReportStatus.PASS));

        assertFalse(new ReportProjectionValidator().validate(expected, observed).valid());
    }
}
