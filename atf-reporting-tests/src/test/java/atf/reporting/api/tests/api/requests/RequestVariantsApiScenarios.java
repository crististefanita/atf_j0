package atf.reporting.api.tests.api.requests;
import atf.core.lifecycle.ApiScenarioPlan;
import atf.reporting.api.acceptance.ApiAcceptancePlanBuilder;
import atf.reporting.api.acceptance.ApiAcceptancePublisher;

import atf.core.reporting.contract.AtfReportPorts;
import atf.reporting.api.execution.ApiModule;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;

import java.util.List;

/** Public Extent scenarios for request parameters, bodies and auth profiles. */
public final class RequestVariantsApiScenarios {
    public static final String REPORT_OWNER = "RequestVariantsApiTests";
    public static final String SEARCH = "search";
    public static final String PROFILE_UPDATE = "profileUpdate";
    public static final String SEARCH_TITLE = RequestVariantRoutes.searchTitle();
    public static final String INVALID_SEARCH_QUERY_TITLE = RequestVariantRoutes.invalidSearchQueryTitle();
    public static final String MISSING_PAGE_SEARCH_TITLE = RequestVariantRoutes.missingPageSearchTitle();
    public static final String EMPTY_PAGE_SEARCH_TITLE = RequestVariantRoutes.emptyPageSearchTitle();
    public static final String DUPLICATE_TAG_SEARCH_TITLE = RequestVariantRoutes.duplicateTagSearchTitle();
    public static final String BULK_IMPORT_TITLE = RequestVariantRoutes.bulkImportTitle();
    public static final String PROFILE_UPDATE_TITLE = RequestVariantRoutes.profileUpdateTitle();
    public static final String PREFERENCES_TITLE = RequestVariantRoutes.preferencesTitle();
    public static final List<String> REPORT_SCENARIO_TITLES = List.of(
            SEARCH_TITLE, INVALID_SEARCH_QUERY_TITLE, MISSING_PAGE_SEARCH_TITLE,
            EMPTY_PAGE_SEARCH_TITLE, DUPLICATE_TAG_SEARCH_TITLE, BULK_IMPORT_TITLE,
            PROFILE_UPDATE_TITLE, PREFERENCES_TITLE);

    private RequestVariantsApiScenarios() { }

    public static void publishSelected(AtfReportPorts.Session session, ApiModule api,
                                       ApiScenarioLifecycleRunner lifecycle, String method) {
        switch (method) {
            case SEARCH -> publishSearch(session, api, lifecycle);
            case PROFILE_UPDATE -> publishProfileUpdate(session, api, lifecycle);
            default -> throw new IllegalArgumentException("Unknown request-variant API scenario method: " + method);
        }
    }

    public static String scenarioTitle(String method) {
        return switch (method) {
            case SEARCH -> SEARCH_TITLE;
            case PROFILE_UPDATE -> PROFILE_UPDATE_TITLE;
            default -> throw new IllegalArgumentException("Unknown request-variant API scenario method: " + method);
        };
    }

    public static void publish(AtfReportPorts.Session session, ApiModule api,
                               ApiScenarioLifecycleRunner lifecycle) {
        publishSearch(session, api, lifecycle);

        run(lifecycle, session, plan(
                INVALID_SEARCH_QUERY_TITLE, RequestVariantRoutes.invalidSearchQueryTags()),
                phase -> {
                    var request = RequestVariantRoutes.invalidSearchQueryRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, INVALID_SEARCH_QUERY_TITLE, execution,
                            RequestVariantRoutes.invalidSearchQueryContract(), false, null, request);
                });

        run(lifecycle, session, plan(
                MISSING_PAGE_SEARCH_TITLE, RequestVariantRoutes.missingPageSearchTags()),
                phase -> {
                    var request = RequestVariantRoutes.missingPageSearchRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, MISSING_PAGE_SEARCH_TITLE, execution,
                            RequestVariantRoutes.missingPageSearchContract(), false, null, request);
                });

        run(lifecycle, session, plan(
                EMPTY_PAGE_SEARCH_TITLE, RequestVariantRoutes.emptyPageSearchTags()),
                phase -> {
                    var request = RequestVariantRoutes.emptyPageSearchRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, EMPTY_PAGE_SEARCH_TITLE, execution,
                            RequestVariantRoutes.emptyPageSearchContract(), false, null, request);
                });

        run(lifecycle, session, plan(
                DUPLICATE_TAG_SEARCH_TITLE, RequestVariantRoutes.duplicateTagSearchTags()),
                phase -> {
                    var request = RequestVariantRoutes.duplicateTagSearchRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, DUPLICATE_TAG_SEARCH_TITLE, execution,
                            RequestVariantRoutes.duplicateTagSearchContract(), false, null, request);
                });

        run(lifecycle, session, plan(
                BULK_IMPORT_TITLE, RequestVariantRoutes.bulkImportTags()),
                phase -> {
                    var request = RequestVariantRoutes.bulkImportRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, BULK_IMPORT_TITLE, execution,
                            RequestVariantRoutes.bulkImportContract(), false, null, request);
                });

        publishProfileUpdate(session, api, lifecycle);

        run(lifecycle, session, plan(
                PREFERENCES_TITLE, RequestVariantRoutes.preferencesTags()),
                phase -> {
                    var request = RequestVariantRoutes.preferencesRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, PREFERENCES_TITLE, execution,
                            RequestVariantRoutes.preferencesContract(), false, null, request);
                });
    }

    private static void publishSearch(AtfReportPorts.Session session, ApiModule api,
                                      ApiScenarioLifecycleRunner lifecycle) {
        run(lifecycle, session, plan(
                SEARCH_TITLE, RequestVariantRoutes.searchTags()),
                phase -> {
                    var request = RequestVariantRoutes.searchRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, SEARCH_TITLE, execution,
                            RequestVariantRoutes.searchContract(), false, null, request);
                });
    }

    private static void publishProfileUpdate(AtfReportPorts.Session session, ApiModule api,
                                             ApiScenarioLifecycleRunner lifecycle) {
        run(lifecycle, session, plan(
                PROFILE_UPDATE_TITLE, RequestVariantRoutes.profileUpdateTags()),
                phase -> {
                    var request = RequestVariantRoutes.profileUpdateRequest();
                    var execution = api.executor().execute(request);
                    ApiAcceptancePublisher.publish(phase, PROFILE_UPDATE_TITLE, execution,
                            RequestVariantRoutes.profileUpdateContract(), false, null, request);
                });
    }

    private static void run(ApiScenarioLifecycleRunner lifecycle, AtfReportPorts.Session session,
                            ApiAcceptancePlanBuilder plan,
                            ApiScenarioPlan.PhaseAction body) {
        lifecycle.run(session, plan.body(body).build());
    }

    private static ApiAcceptancePlanBuilder plan(String scenario, java.util.List<String> tags) {
        return ApiAcceptancePlanBuilder.scenario(REPORT_OWNER, RequestVariantsApiScenarios.class.getPackageName(),
                scenario, tags);
    }
}
