package atf.ui.tests.extent.component.plan;

import atf.core.testing.api.ApiScenarioSelection;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.testkit.setup.api.ApiTestRef;
import atf.mock.AtfMockConfiguration;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.audit.plan.ReportProjection;
import atf.testkit.AtfComponentTest;
import atf.reporting.api.tests.api.lifecycle.LifecycleApiScenarios;
import atf.reporting.api.tests.api.posts.PostsApiScenarios;
import atf.reporting.api.tests.api.users.UsersApiScenarios;
import atf.ui.tests.extent.support.api.ApiReportAuditSetup;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.data.UiTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiScenarioManifestTest {
    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = UsersApiScenarios.class,
                    method = UsersApiScenarios.GET_COLLECTION)
    })
    private static final class OnePositiveSelection { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = UsersApiScenarios.class,
                    method = UsersApiScenarios.GET_COLLECTION_WARNING),
            @ApiTestRef(owner = PostsApiScenarios.class,
                    method = PostsApiScenarios.CREATE_POST)
    })
    private static final class MixedWarningAndWriteSelection { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = LifecycleApiScenarios.class,
                    method = LifecycleApiScenarios.LIFECYCLE_MATRIX)
    })
    private static final class MultiOutcomeSelection { }

    private static final List<AtfReportStatus> ALL_REPORT_STATUSES = List.of(
            AtfReportStatus.PASS, AtfReportStatus.SKIP,
            AtfReportStatus.FAIL, AtfReportStatus.WARNING);

    @Test
    void derivesOwnersAndWriteContractFromSelectedFamiliesAndMock() {
        ApiScenarioManifest manifest = ApiScenarioManifest.from(
                java.util.List.of(UiTestData.USERS_FAMILY, UiTestData.POSTS_FAMILY),
                AtfMockConfiguration.defaults());

        assertEquals(java.util.List.of(UiTestData.USERS_FAMILY, UiTestData.POSTS_FAMILY),
                manifest.families());
        assertEquals(java.util.List.of(UiTestData.USERS_OWNER, UiTestData.POSTS_OWNER),
                manifest.expectedOwnerNames());
        assertTrue(manifest.writeRouteCount() >= 2);
        assertTrue(manifest.writeRoutesHaveJsonResponses());
        assertTrue(manifest.routes().stream().anyMatch(route ->
                route.identity().equals(PostsApiScenarios.CREATE_ROUTE_IDENTITY)));
    }

    @Test
    void rejectsUnknownFamilyInsteadOfLearningExpectedOwnersFromDom() {
        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ApiScenarioManifest.from(java.util.List.of("not-a-family"),
                        AtfMockConfiguration.defaults()));
        assertTrue(error.getMessage().contains("Unknown API family"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configuredSelections")
    void exactConfiguredSelectionsDefineTheExpectedReportProjection(SelectionCase selectionCase) {
        UiConfiguration fallback = UiConfiguration.defaults(Path.of("target", "selection-component"));
        List<String> families = ApiReportAuditSetup.families(selectionCase.declaration(), fallback);
        List<ApiScenarioSelection> selectedTests =
                ApiReportAuditSetup.selectedTests(selectionCase.declaration());

        ApiScenarioManifest manifest = ApiScenarioManifest.from(families,
                ReportingTestResources.mockConfiguration(), selectedTests);
        ReportProjection projection = manifest.expectedProjection();

        assertEquals(selectionCase.families(), families,
                "the exact annotation must override a broader configured collection");
        assertEquals(selectionCase.ownerNames(), manifest.expectedOwnerNames());
        assertEquals(selectionCase.ownerNames(),
                projection.owners().stream().map(ReportProjection.Owner::name).toList());
        assertEquals(selectionCase.scenarioTitles(), projection.scenarioTitles(),
                "unselected methods must not leak into the expected report");
        assertEquals(selectionCase.scenarioStatuses(), projection.expectedScenarioStatuses(),
                "every selected scenario title must retain its expected outcome");
        assertEquals(selectionCase.scenarioTitles().size(), projection.scenarioCount());
        assertTrue(projection.matches(observation(selectionCase.ownerNames(),
                selectionCase.scenarioTitles(), selectionCase.scenarioStatuses())));

        assertFalse(projection.matches(observation(
                        append(selectionCase.ownerNames(), "UnselectedApiTests"),
                        append(selectionCase.scenarioTitles(), "unselected scenario"),
                        append(selectionCase.scenarioStatuses(), AtfReportStatus.PASS))),
                "an otherwise valid report with an unselected scenario must fail closed");

        List<AtfReportStatus> changedStatuses = selectionCase.scenarioStatuses().stream()
                .map(status -> status == AtfReportStatus.PASS
                        ? AtfReportStatus.FAIL : AtfReportStatus.PASS)
                .toList();
        assertFalse(projection.matches(observation(selectionCase.ownerNames(),
                        selectionCase.scenarioTitles(), changedStatuses)),
                "a status drift must fail even when the selected scenario shape matches");
    }

    @Test
    void typedSelectionPreservesTheCompleteReportOutcomeVocabularyInOrder() {
        ApiScenarioSelection selection = new ApiScenarioSelection(
                UsersApiScenarios.class, UsersApiScenarios.GET_COLLECTION,
                UiTestData.USERS_FAMILY, UsersApiScenarios.REPORT_OWNER,
                UsersApiScenarios.GET_COLLECTION_TITLE,
                List.of("positive branch", "skipped branch", "failed branch", "warning branch"),
                List.of(atf.core.reporting.contract.AtfReportStatus.PASS,
                        atf.core.reporting.contract.AtfReportStatus.SKIP,
                        atf.core.reporting.contract.AtfReportStatus.FAIL,
                        atf.core.reporting.contract.AtfReportStatus.WARNING));

        ReportProjection projection = ApiScenarioManifest.from(
                List.of(UiTestData.USERS_FAMILY), ReportingTestResources.mockConfiguration(),
                List.of(selection)).expectedProjection();

        assertEquals(ALL_REPORT_STATUSES, projection.expectedScenarioStatuses());
        for (AtfReportStatus status : ALL_REPORT_STATUSES) {
            assertEquals(1, projection.expectedStatusCount(status));
        }
    }

    private static Stream<SelectionCase> configuredSelections() {
        return Stream.of(
                new SelectionCase("one exact positive API method", OnePositiveSelection.class,
                        List.of(UiTestData.USERS_FAMILY), List.of(UiTestData.USERS_OWNER),
                        List.of(UsersApiScenarios.GET_COLLECTION_TITLE),
                        List.of(AtfReportStatus.PASS)),
                new SelectionCase("mixed warning and POST methods", MixedWarningAndWriteSelection.class,
                        List.of(UiTestData.USERS_FAMILY, UiTestData.POSTS_FAMILY),
                        List.of(UiTestData.USERS_OWNER, UiTestData.POSTS_OWNER),
                        List.of(UsersApiScenarios.GET_COLLECTION_WARNING_TITLE,
                                PostsApiScenarios.CREATE_POST_TITLE),
                        List.of(AtfReportStatus.WARNING, AtfReportStatus.PASS)),
                new SelectionCase("one method expands to multiple lifecycle outcomes",
                        MultiOutcomeSelection.class, List.of(UiTestData.LIFECYCLE_FAMILY),
                        List.of(UiTestData.LIFECYCLE_OWNER),
                        LifecycleApiScenarios.SELECTED_REPORT_SCENARIO_TITLES,
                        List.of(AtfReportStatus.FAIL, AtfReportStatus.FAIL,
                                AtfReportStatus.FAIL)));
    }

    private static ReportAuditSnapshot.Tests observation(List<String> ownerNames,
                                                         List<String> scenarioTitles,
                                                         List<AtfReportStatus> statuses) {
        int scenarioCount = scenarioTitles.size();
        return new ReportAuditSnapshot.Tests(ownerNames.size(), scenarioCount, scenarioCount,
                scenarioCount, scenarioCount, ownerNames, scenarioTitles, statuses);
    }

    private static <T> List<T> append(List<T> values, T value) {
        return Stream.concat(values.stream(), Stream.of(value)).toList();
    }

    private record SelectionCase(String name, Class<?> declaration,
                                 List<String> families, List<String> ownerNames,
                                 List<String> scenarioTitles,
                                 List<AtfReportStatus> scenarioStatuses) {
        private SelectionCase {
            families = List.copyOf(families);
            ownerNames = List.copyOf(ownerNames);
            scenarioTitles = List.copyOf(scenarioTitles);
            scenarioStatuses = List.copyOf(scenarioStatuses);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
