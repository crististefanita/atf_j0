package atf.reporting.model;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.projection.ReportCategoryProjector;
import atf.reporting.model.projection.ReportExecutionProjections;
import atf.reporting.model.projection.ReportFailureProjector;
import atf.reporting.model.projection.ReportOverviewProjector;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit boundary for the renderer-neutral execution projections. */
@AtfUnitTest
class ReportExecutionProjectionTest {
    @Test
    void overviewProjectionCountsOnlyScenarioVerdicts() {
        var scenarios = List.of(
                scenario("pass", AtfReportStatus.PASS, List.of("users")),
                scenario("fail", AtfReportStatus.FAIL, List.of("users", "contract")),
                scenario("skip", AtfReportStatus.SKIP, List.of("contract")));

        var overview = ReportOverviewProjector.project(scenarios);

        assertEquals(new ReportExecutionModel.Overview(3, 1, 1, 1), overview);
    }

    @Test
    void categoryProjectionIsSortedAndDeduplicatedBeforeRendering() {
        var scenarios = List.of(
                scenario("one", AtfReportStatus.PASS, List.of("z", "api")),
                scenario("two", AtfReportStatus.PASS, List.of("api", "contract")));

        assertEquals(List.of("api", "contract", "z"),
                ReportCategoryProjector.project(scenarios).tags());
    }

    @Test
    void failureProjectionTraversesRequestsWithoutMutatingTheSnapshot() {
        var assertion = new AtfAssertion("HTTP status", "200", "500", false);
        var validation = new ReportExecutionModel.Validation(
                "Validate response", List.of(assertion), AtfReportStatus.FAIL);
        var request = new ReportExecutionModel.Request(
                "read user", "GET", "/users/1", List.of(), List.of(), null,
                List.of(validation), AtfReportStatus.FAIL);
        var phase = new ReportExecutionModel.Phase(
                "TEST BODY", List.of(), List.of(request), AtfReportStatus.FAIL);
        var scenario = new ReportExecutionModel.Scenario(
                "scenario-1", "UsersTests", "api", "read user",
                List.of("api"), List.of(phase), AtfReportStatus.FAIL);

        var failures = ReportFailureProjector.project(List.of(scenario));

        assertAll(
                () -> assertEquals(1, failures.items().size()),
                () -> assertEquals("scenario-1", failures.items().get(0).scenarioId()),
                () -> assertEquals("HTTP status", failures.items().get(0).assertionName()),
                () -> assertEquals("500", failures.items().get(0).actual()),
                () -> assertTrue(scenario.phases().get(0).children().contains(request)));
    }

    @Test
    void aggregateProjectionKeepsAllSecondaryViewsOnTheSamePopulation() {
        var assertion = new AtfAssertion("HTTP status", "200", "500", false);
        var validation = new ReportExecutionModel.Validation(
                "Validate response", List.of(assertion), AtfReportStatus.FAIL);
        var request = new ReportExecutionModel.Request(
                "read user", "GET", "/users/1", List.of(), List.of(), null,
                List.of(validation), AtfReportStatus.FAIL);
        var phase = new ReportExecutionModel.Phase(
                "TEST BODY", List.of(), List.of(request), AtfReportStatus.FAIL);
        var scenarios = List.of(
                scenario("pass", AtfReportStatus.PASS, List.of("api")),
                new ReportExecutionModel.Scenario(
                        "fail", "ProjectionTests", "api", "fail",
                        List.of("contract"), List.of(phase), AtfReportStatus.FAIL));

        var projections = ReportExecutionProjections.project(scenarios);

        assertAll(
                () -> assertEquals(new ReportExecutionModel.Overview(2, 1, 1, 0),
                        projections.overview()),
                () -> assertEquals(List.of("api", "contract"), projections.categories().tags()),
                () -> assertEquals(1, projections.failures().items().size()),
                () -> assertEquals("500", projections.failures().items().getFirst().actual()));
    }

    @Test
    void modelExposesTheAggregateWithoutChangingCompatibilityAccessors() {
        var model = new ReportExecutionModel(List.of(
                scenario("pass", AtfReportStatus.PASS, List.of("api"))));

        assertAll(
                () -> assertEquals(model.projections().overview(), model.overview()),
                () -> assertEquals(model.projections().categories(), model.categories()),
                () -> assertEquals(model.projections().failures(), model.failures()));
    }

    private static ReportExecutionModel.Scenario scenario(String id,
                                                            AtfReportStatus status,
                                                            List<String> tags) {
        return new ReportExecutionModel.Scenario(
                id, "ProjectionTests", "api", id, tags, List.of(), status);
    }
}
