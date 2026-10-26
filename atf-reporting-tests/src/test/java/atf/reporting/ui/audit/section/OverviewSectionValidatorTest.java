package atf.reporting.ui.audit.section;
import atf.testkit.AtfUnitTest;

import atf.reporting.audit.ReportAuditSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class OverviewSectionValidatorTest {
    private final OverviewSectionValidator validator = new OverviewSectionValidator();

    @Test
    void acceptsOverviewThatReconcilesWithTests() {
        var tests = new ReportAuditSnapshot.Tests(2, 3, 3, 6, 6,
                List.of("Users", "Posts"), List.of("GET users", "POST post", "GET post"));
        var overview = new ReportAuditSnapshot.Overview(9, 9, 2, 3, 3, 1, true, List.of());
        var result = validator.validate(overview, tests,
                new OverviewSectionValidator.Expected(List.of("users", "posts"), 6,
                        Set.of("Users", "Posts")));
        assertTrue(result.valid(), result::failureSummary);
    }

    @Test
    void rejectsOverviewWithDifferentScenarioTotals() {
        var tests = new ReportAuditSnapshot.Tests(1, 2, 1, 2, 2,
                List.of("Users"), List.of("GET users", "POST users"));
        var overview = new ReportAuditSnapshot.Overview(8, 9, 1, 1, 1, 1, true, List.of());
        var result = validator.validate(overview, tests,
                new OverviewSectionValidator.Expected(List.of(), 0, Set.of()));
        assertFalse(result.valid());
    }
}
