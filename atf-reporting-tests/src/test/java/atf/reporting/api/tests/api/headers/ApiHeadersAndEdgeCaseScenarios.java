package atf.reporting.api.tests.api.headers;
import atf.core.http.request.ApiRequest;
import atf.reporting.api.acceptance.ApiAcceptancePlanBuilder;
import atf.reporting.api.acceptance.ApiAcceptancePublisher;

import atf.core.reporting.contract.AtfReportPorts;
import atf.reporting.api.execution.ApiModule;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.validation.contract.ResponseContract;

import java.util.List;

/** Extent projection for header, cache and rate-limit behavior. */
public final class ApiHeadersAndEdgeCaseScenarios {
    public static final String REPORT_OWNER = "ApiHeadersAndEdgeCasesTests";
    public static final String CATALOG_PAGE = "catalogPage";
    public static final String EXPORT = "export";
    public static final String NOT_MODIFIED = "notModified";
    public static final String CATALOG_PAGE_TITLE = ApiHeaderRoutes.catalogPageTitle();
    public static final String EXPORT_TITLE = ApiHeaderRoutes.exportTitle();
    public static final String NOT_MODIFIED_TITLE = ApiHeaderRoutes.notModifiedTitle();
    public static final List<String> REPORT_SCENARIO_TITLES = List.of(
            CATALOG_PAGE_TITLE, EXPORT_TITLE, NOT_MODIFIED_TITLE);

    private ApiHeadersAndEdgeCaseScenarios() { }

    public static void publishSelected(AtfReportPorts.Session session, ApiModule api,
                                       ApiScenarioLifecycleRunner lifecycle, String method) {
        switch (method) {
            case CATALOG_PAGE -> publishOne(session, api, lifecycle, CATALOG_PAGE_TITLE,
                    ApiHeaderRoutes.catalogPageTags(), ApiHeaderRoutes.catalogPageRequest(),
                    ApiHeaderRoutes.catalogPageContract());
            case EXPORT -> publishOne(session, api, lifecycle, EXPORT_TITLE,
                    ApiHeaderRoutes.exportTags(), ApiHeaderRoutes.exportRequest(),
                    ApiHeaderRoutes.exportContract());
            case NOT_MODIFIED -> publishOne(session, api, lifecycle, NOT_MODIFIED_TITLE,
                    ApiHeaderRoutes.notModifiedTags(), ApiHeaderRoutes.notModifiedRequest(),
                    ApiHeaderRoutes.notModifiedContract());
            default -> throw new IllegalArgumentException("Unknown header API scenario method: " + method);
        }
    }

    public static String scenarioTitle(String method) {
        return switch (method) {
            case CATALOG_PAGE -> CATALOG_PAGE_TITLE;
            case EXPORT -> EXPORT_TITLE;
            case NOT_MODIFIED -> NOT_MODIFIED_TITLE;
            default -> throw new IllegalArgumentException("Unknown header API scenario method: " + method);
        };
    }

    public static void publish(AtfReportPorts.Session session, ApiModule api,
                               ApiScenarioLifecycleRunner lifecycle) {
        publishOne(session, api, lifecycle, CATALOG_PAGE_TITLE,
                ApiHeaderRoutes.catalogPageTags(), ApiHeaderRoutes.catalogPageRequest(),
                ApiHeaderRoutes.catalogPageContract());
        publishOne(session, api, lifecycle, EXPORT_TITLE,
                ApiHeaderRoutes.exportTags(), ApiHeaderRoutes.exportRequest(),
                ApiHeaderRoutes.exportContract());
        publishOne(session, api, lifecycle, NOT_MODIFIED_TITLE,
                ApiHeaderRoutes.notModifiedTags(), ApiHeaderRoutes.notModifiedRequest(),
                ApiHeaderRoutes.notModifiedContract());
    }

    private static void publishOne(AtfReportPorts.Session session, ApiModule api,
                                   ApiScenarioLifecycleRunner lifecycle, String title,
                                   java.util.List<String> tags, ApiRequest request,
                                   ResponseContract contract) {
        var plan = ApiAcceptancePlanBuilder.scenario(REPORT_OWNER,
                ApiHeadersAndEdgeCaseScenarios.class.getPackageName(), title, tags);
        lifecycle.run(session, plan.body(phase -> {
            var execution = api.executor().execute(request);
            ApiAcceptancePublisher.publish(phase, title, execution, contract, false, null, request);
        }).build());
    }
}
