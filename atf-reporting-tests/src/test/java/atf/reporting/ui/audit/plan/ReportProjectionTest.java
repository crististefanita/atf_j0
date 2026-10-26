package atf.reporting.ui.audit.plan;
import atf.testkit.AtfUnitTest;

import atf.reporting.api.ReportScenarioSelection;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class ReportProjectionTest {
    @Test
    void createsTheExpectedShapeFromExactCoreSelections() {
        var projection = ReportProjection.from(List.of(
                new ReportScenarioSelection(getClass(), "getUsers", "users",
                        "UsersApiTests", "GET users"),
                new ReportScenarioSelection(getClass(), "createPost", "posts",
                        "PostsApiTests", "POST posts")));

        assertEquals(List.of("UsersApiTests", "PostsApiTests"),
                projection.owners().stream().map(ReportProjection.Owner::name).toList());
        assertEquals(List.of("GET users", "POST posts"), projection.scenarioTitles());
        assertEquals(2, projection.scenarioCount());
        assertEquals(ReportProjection.DEFAULT_TABS, projection.tabs());
        assertEquals(List.of(AtfReportStatus.PASS, AtfReportStatus.PASS),
                projection.expectedScenarioStatuses());
    }

    @Test
    void keepsExplicitWarningAndFailureExpectationsAlignedWithTitles() {
        var projection = ReportProjection.from(List.of(
                new ReportScenarioSelection(getClass(), "warning", "users", "UsersApiTests",
                        "GET users warning", List.of("GET users warning"),
                        List.of(AtfReportStatus.WARNING)),
                new ReportScenarioSelection(getClass(), "failure", "lifecycle", "LifecycleApiScenarios",
                        "Lifecycle failure", List.of("Lifecycle failure"),
                        List.of(AtfReportStatus.FAIL))));

        assertEquals(List.of(AtfReportStatus.WARNING, AtfReportStatus.FAIL),
                projection.expectedScenarioStatuses());
        assertEquals(1, projection.expectedStatusCount(AtfReportStatus.FAIL));
    }

    @Test
    void exposesAnExplicitEmptyProjectionForStaticReports() {
        var projection = ReportProjection.empty();

        assertEquals(0, projection.scenarioCount());
        assertEquals(List.of(), projection.tabs());
    }
}
