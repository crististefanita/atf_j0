package atf.reporting.model;
import atf.testkit.AtfUnitTest;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.mock.MockRoutePaths;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportExecutionModelTest {
    @Test
    void derivesAllStaticReportProjectionsFromOneExecutionTree() {
        var failed = new ReportExecutionModel.Validation(
                "Validate user",
                List.of(new AtfAssertion("HTTP status", "201 Created", "500 Server Error", false)),
                AtfReportStatus.FAIL);
        var request = new ReportExecutionModel.Request(
                "read user", "GET", MockRoutePaths.userById("1"), List.of(), List.of(),
                null, List.of(failed), AtfReportStatus.FAIL);
        var model = new ReportExecutionModel(List.of(
                new ReportExecutionModel.Scenario("test-id=1", "UsersTests", "read user",
                        List.of("api", "users"),
                        List.of(new ReportExecutionModel.Phase("TEST BODY", List.of(),
                                List.of(request), AtfReportStatus.FAIL)),
                        AtfReportStatus.FAIL),
                new ReportExecutionModel.Scenario("test-id=2", "PostsTests", "create post",
                        List.of("api", "posts"), List.of(), AtfReportStatus.PASS)));

        assertEquals(new ReportExecutionModel.Overview(2, 1, 1, 0), model.overview());
        assertEquals(List.of("api", "posts", "users"), model.categories().tags());
        assertEquals(1, model.failures().items().size());
        assertEquals("500 Server Error", model.failures().items().getFirst().actual());
    }

    @Test
    void keepsTheExecutionTreeAndDerivedFailuresImmutable() {
        var assertion = new AtfAssertion("status", "200", "503", false);
        var validation = new ReportExecutionModel.Validation("response", List.of(assertion),
                AtfReportStatus.FAIL);
        var model = new ReportExecutionModel(List.of(new ReportExecutionModel.Scenario(
                "test-id=1", "Tests", "failure", List.of("api"),
                List.of(new ReportExecutionModel.Phase("TEST BODY", List.of(),
                        List.of(validation), AtfReportStatus.FAIL)), AtfReportStatus.FAIL)));

        assertThrows(UnsupportedOperationException.class, () -> model.scenarios().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.scenarios().getFirst()
                .phases().getFirst().children().clear());
        assertThrows(UnsupportedOperationException.class, () -> model.failures().items().clear());
        assertTrue(model.failures().items().getFirst().actual().contains("503"));
    }
}
