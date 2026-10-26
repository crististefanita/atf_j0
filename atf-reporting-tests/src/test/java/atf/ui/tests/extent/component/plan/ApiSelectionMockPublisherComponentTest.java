package atf.ui.tests.extent.component.plan;

import atf.core.http.request.HttpMethod;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.testkit.setup.api.ApiTestRef;
import atf.mock.MockRoutePaths;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.reporting.api.tests.api.posts.PostsApiScenarios;
import atf.ui.tests.extent.support.api.ApiReportAuditSetup;
import atf.ui.tests.extent.support.api.ApiReportSetupDeclarations;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;

import static atf.core.http.response.JsonBodyPolicy.validDocument;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves exact API selection through the reporting-owned mock and final publisher. */
@AtfComponentTest
class ApiSelectionMockPublisherComponentTest {
    private static final String SELECTED_TITLE = PostsApiScenarios.CREATE_POST_TITLE;
    private static final String UNSELECTED_TITLE = PostsApiScenarios.GET_COLLECTION_TITLE;

    @Test
    void exactPostSelectionRunsAgainstMockAndPublishesOnlyItsSemanticEvidence() throws Exception {
        UiConfiguration configuration = ReportingTestResources.uiConfiguration();
        Instant startedAt = Instant.now();
        var selection = ApiReportAuditSetup.selection(
                ApiReportSetupDeclarations.ExactPostCreate.class, configuration, null, null);

        ApiReportAuditSetup.Result result = new ApiReportAuditSetup()
                .generate(ApiReportSetupDeclarations.ExactPostCreate.class, configuration, selection);

        assertTrue(Files.isRegularFile(result.report()),
                "the real Extent publisher must create the API artifact");
        var route = result.mock().routes().stream()
                .filter(candidate -> candidate.method().equals(HttpMethod.POST.value()))
                .filter(candidate -> candidate.path().equals(MockRoutePaths.POSTS))
                .findFirst().orElseThrow();
        var artifact = ReportArtifactInspector.inspectFresh(result.report(), startedAt);
        String html = Files.readString(result.report());

        assertAll(
                () -> assertEquals(java.util.List.of("posts"), result.families()),
                () -> assertEquals(1, result.selectedTests().size()),
                () -> assertEquals(SELECTED_TITLE,
                        result.selectedTests().getFirst().scenarioTitle()),
                () -> assertEquals(java.util.List.of(SELECTED_TITLE),
                        result.manifest().expectedProjection().scenarioTitles()),
                () -> assertEquals(java.util.List.of(AtfReportStatus.PASS),
                        result.manifest().expectedProjection().expectedScenarioStatuses()),
                () -> assertEquals(201, route.status()),
                () -> assertTrue(validDocument(route.body()),
                        "the selected POST response must come from its JSON mock route"),
                () -> assertTrue(html.contains(SELECTED_TITLE),
                        "the chosen exact API test must reach the published report"),
                () -> assertFalse(html.contains(UNSELECTED_TITLE),
                        "unselected methods from the same family must not leak into the report"),
                () -> assertTrue(html.contains("REQUEST JSON")),
                () -> assertTrue(html.contains("RESPONSE JSON")),
                () -> assertTrue(html.contains("Execution evidence")),
                () -> assertTrue(html.contains("ATF created post"),
                        "published response evidence must reflect the configured mock"),
                () -> assertTrue(artifact.isCompliant(),
                        () -> "published artifact violations: " + artifact.violations()),
                () -> assertTrue(artifact.satisfies("fresh artifact")));
    }

    @Test
    void unknownExactApiReferenceFailsBeforeMockOrPublisherIsStarted() {
        assertThrows(IllegalArgumentException.class,
                () -> ApiReportAuditSetup.selection(InvalidExactSelection.class,
                        ReportingTestResources.uiConfiguration(), null, null));
    }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = PostsApiScenarios.class, method = "notAnApiTest")
    })
    private static final class InvalidExactSelection { }
}
