package atf.reporting.api.tests.api.flows;
import atf.reporting.api.acceptance.ApiAcceptancePlanBuilder;
import atf.reporting.api.acceptance.ApiAcceptancePublisher;

import atf.core.http.response.ApiOperationLabel;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;

import atf.core.reporting.contract.AtfReportPorts;
import atf.reporting.api.execution.ApiModule;
import atf.core.json.JsonPayloads;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;

import java.util.List;

/** Public Extent projection for stateful multi-request API flows. */
public final class ApiRequestFlowScenarios {
    public static final String REPORT_OWNER = "ApiRequestFlowsTests";
    public static final String PROFILE_UPDATE = "profileUpdate";
    public static final String PROFILE_UPDATE_TITLE = ApiRequestFlowFixtures.PROFILE_UPDATE_TITLE;
    public static final List<String> REPORT_SCENARIO_TITLES = List.of(PROFILE_UPDATE_TITLE);

    private ApiRequestFlowScenarios() { }

    public static void publishSelected(AtfReportPorts.Session session, ApiModule api,
                                       ApiScenarioLifecycleRunner lifecycle, String method) {
        if (PROFILE_UPDATE.equals(method)) {
            publishProfileUpdate(session, api, lifecycle);
            return;
        }
        throw new IllegalArgumentException("Unknown API flow scenario method: " + method);
    }

    public static String scenarioTitle(String method) {
        if (PROFILE_UPDATE.equals(method)) return PROFILE_UPDATE_TITLE;
        throw new IllegalArgumentException("Unknown API flow scenario method: " + method);
    }

    public static void publish(AtfReportPorts.Session session, ApiModule api,
                               ApiScenarioLifecycleRunner lifecycle) {
        lifecycle.run(session, ApiAcceptancePlanBuilder.scenario(
                        REPORT_OWNER, ApiRequestFlowScenarios.class.getPackageName(),
                        ApiRequestFlowFixtures.POST_CREATE_TITLE,
                        List.of("api", "flow", "post", "read-after-write"))
                .body(phase -> {
                    var created = api.posts().create(ApiRequestFlowFixtures.CREATED_POST);
                    String createdId = ApiFlowAssertions.resourceId(created, HttpMethod.POST);
                    ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.POST, MockRoutePaths.POSTS, null), created,
                            ApiRequestFlowFixtures.createdPostMutationContract(), false,
                            JsonPayloads.write(ApiRequestFlowFixtures.CREATED_POST));
                    var collection = api.posts().getCollection();
                    ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.POSTS, "collection correlation"),
                            collection, ApiRequestFlowFixtures.createdPostCollectionContract(createdId), false, null);
                    var readBack = api.posts().get(Integer.parseInt(createdId));
                    ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.GET,
                            MockRoutePaths.postById(createdId), null), readBack,
                            ApiRequestFlowFixtures.createdPostReadModelContract(createdId), false, null, null,
                            request -> ApiFlowAssertions.postReadBack(created, collection, readBack, request));
                }).build());

        publishProfileUpdate(session, api, lifecycle);
    }

    private static void publishProfileUpdate(AtfReportPorts.Session session, ApiModule api,
                                             ApiScenarioLifecycleRunner lifecycle) {
        lifecycle.run(session, ApiAcceptancePlanBuilder.scenario(
                        REPORT_OWNER, ApiRequestFlowScenarios.class.getPackageName(),
                        ApiRequestFlowFixtures.PROFILE_UPDATE_TITLE,
                        List.of("api", "flow", "put", "read-after-write"))
                .body(phase -> {
                    var updated = api.executor().execute(ApiRequestFlowFixtures.profileUpdateRequest());
                    ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.PUT, MockRoutePaths.profileById("1"), null), updated,
                            ApiRequestFlowFixtures.updatedProfileMutationContract(), false,
                            ApiRequestFlowFixtures.profileUpdateJson());
                    var readBack = api.executor().execute(ApiRequestFlowFixtures.profileReadRequest());
                    ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.profileById("1"), null), readBack,
                            ApiRequestFlowFixtures.profileReadModelContract(), false, null, null,
                            request -> ApiFlowAssertions.profileReadBack(updated, readBack, request));
                }).build());
    }

}
