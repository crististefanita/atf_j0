package atf.reporting.api.tests.api.users;
import atf.core.lifecycle.ApiScenarioPlan;
import atf.reporting.api.acceptance.ApiAcceptancePlanBuilder;
import atf.reporting.api.acceptance.ApiAcceptancePublisher;

import atf.core.http.response.ApiOperationLabel;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;
import atf.core.http.request.MediaTypes;

import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.api.execution.ApiModule;
import atf.core.json.JsonPayloads;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.reporting.api.execution.model.UserRequest;
import atf.reporting.api.tests.api.data.ApiFixtureData;
import atf.validation.contract.ContractValidators;
import atf.validation.contract.ResponseContract;

import java.util.List;

/** User-resource scenarios that appear in the public acceptance artifact. */
public final class UsersApiScenarios {
    public static final String REPORT_OWNER = "UsersApiTests";
    public static final String GET_COLLECTION = "getCollection";
    public static final String GET_BY_ID = "getById";
    public static final String CREATE_UNAUTHENTICATED = "createUnauthenticated";
    public static final String GET_COLLECTION_WARNING = "getCollectionWarning";
    public static final String GET_COLLECTION_TITLE = ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.USERS,
            "collection contract");
    public static final String GET_BY_ID_TITLE = ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.USER_BY_ID,
            "item contract");
    public static final String CREATE_UNAUTHENTICATED_TITLE = ApiOperationLabel.format(HttpMethod.POST, MockRoutePaths.USERS,
            "missing authentication");
    public static final String GET_COLLECTION_WARNING_TITLE = ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.USERS,
            "soft contract warning");
    public static final List<String> REPORT_SCENARIO_TITLES = List.of(
            GET_COLLECTION_TITLE, GET_BY_ID_TITLE, CREATE_UNAUTHENTICATED_TITLE, GET_COLLECTION_WARNING_TITLE);

    private UsersApiScenarios() { }

    public static void publish(AtfReportPorts.Session session, ApiModule api,
                               ApiScenarioLifecycleRunner lifecycle) {
        publishCollection(session, api, lifecycle);
        publishItem(session, api, lifecycle);
        publishUnauthenticatedCreate(session, api, lifecycle);
        publishCollectionWarning(session, api, lifecycle);
    }

    public static void publishSelected(AtfReportPorts.Session session, ApiModule api,
                                       ApiScenarioLifecycleRunner lifecycle, String method) {
        switch (method) {
            case GET_COLLECTION -> publishCollection(session, api, lifecycle);
            case GET_BY_ID -> publishItem(session, api, lifecycle);
            case CREATE_UNAUTHENTICATED -> publishUnauthenticatedCreate(session, api, lifecycle);
            case GET_COLLECTION_WARNING -> publishCollectionWarning(session, api, lifecycle);
            default -> throw new IllegalArgumentException("Unknown users API scenario method: " + method);
        }
    }

    public static String scenarioTitle(String method) {
        return switch (method) {
            case GET_COLLECTION -> GET_COLLECTION_TITLE;
            case GET_BY_ID -> GET_BY_ID_TITLE;
            case CREATE_UNAUTHENTICATED -> CREATE_UNAUTHENTICATED_TITLE;
            case GET_COLLECTION_WARNING -> GET_COLLECTION_WARNING_TITLE;
            default -> throw new IllegalArgumentException("Unknown users API scenario method: " + method);
        };
    }

    public static AtfReportStatus expectedStatus(String method) {
        return GET_COLLECTION_WARNING.equals(method) ? AtfReportStatus.WARNING : AtfReportStatus.PASS;
    }

    private static void publishCollection(AtfReportPorts.Session session, ApiModule api,
                                          ApiScenarioLifecycleRunner lifecycle) {
        run(lifecycle, session, plan(
                GET_COLLECTION_TITLE,
                List.of("api", "users", "contract", "smoke")),
                phase -> ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.USERS, null),
                        api.users().getCollection(), ContractValidators.userCollection(), false, null));
    }

    private static void publishItem(AtfReportPorts.Session session, ApiModule api,
                                    ApiScenarioLifecycleRunner lifecycle) {
        run(lifecycle, session, plan(
                GET_BY_ID_TITLE,
                List.of("api", "users", "contract")),
                phase -> ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.userById("1"), null), api.users().get(1),
                        ContractValidators.userItem(), false, null));
    }

    private static void publishUnauthenticatedCreate(AtfReportPorts.Session session, ApiModule api,
                                                      ApiScenarioLifecycleRunner lifecycle) {
        run(lifecycle, session, plan(
                CREATE_UNAUTHENTICATED_TITLE,
                List.of("api", "users", "negative", "auth")),
                phase -> {
                    UserRequest payload = ApiFixtureData.unauthenticatedUserRequest();
                    ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.POST, MockRoutePaths.USERS, null), api.users().create(payload),
                            new ResponseContract().status(401).contentTypeContains(MediaTypes.APPLICATION_JSON)
                                    .bodyContains("Authentication required"), false,
                            JsonPayloads.write(payload));
                });
    }

    private static void publishCollectionWarning(AtfReportPorts.Session session, ApiModule api,
                                                 ApiScenarioLifecycleRunner lifecycle) {
        run(lifecycle, session, plan(
                GET_COLLECTION_WARNING_TITLE,
                List.of("api", "users", "reporting", "soft-assert")),
                phase -> ApiAcceptancePublisher.publish(phase, ApiOperationLabel.format(HttpMethod.GET, MockRoutePaths.USERS, null),
                        api.users().getCollection(),
                        new ResponseContract().status(201).contentTypeContains(MediaTypes.APPLICATION_JSON), true, null));
    }

    private static void run(ApiScenarioLifecycleRunner lifecycle, AtfReportPorts.Session session,
                            ApiAcceptancePlanBuilder plan,
                            ApiScenarioPlan.PhaseAction body) {
        lifecycle.run(session, plan.body(body).build());
    }

    private static ApiAcceptancePlanBuilder plan(String scenario, List<String> tags) {
        return ApiAcceptancePlanBuilder.scenario("UsersApiTests", UsersApiScenarios.class.getPackageName(),
                scenario, tags);
    }
}
