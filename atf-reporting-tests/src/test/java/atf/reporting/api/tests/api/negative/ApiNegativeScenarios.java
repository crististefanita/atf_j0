package atf.reporting.api.tests.api.negative;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.api.acceptance.ApiAcceptancePlanBuilder;
import atf.reporting.api.acceptance.ApiAcceptancePublisher;

import atf.core.http.response.ApiOperationLabel;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.core.reporting.contract.AtfReportPorts;
import atf.reporting.api.execution.ApiModule;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.validation.contract.ResponseContract;

import java.util.List;

/**
 * Extent projection of the negative wire matrix. Each request keeps its
 * modeled negative outcome visible while the response is validated deeply
 * against the mock error envelope.
 */
public final class ApiNegativeScenarios {
    public static final String REPORT_OWNER = "ApiNegativeMatrixTests";
    public static final String NEGATIVE_MATRIX = "negativeMatrix";
    public static final String NEGATIVE_MATRIX_TITLE = "Negative API wire contract matrix";
    public static final List<String> REPORT_SCENARIO_TITLES = List.of(NEGATIVE_MATRIX_TITLE);
    private static final String PACKAGE = "atf.reporting.api.tests.api.negative";

    private ApiNegativeScenarios() { }

    public static void publishSelected(AtfReportPorts.Session session, ApiModule api,
                                       ApiScenarioLifecycleRunner lifecycle, String method) {
        if (NEGATIVE_MATRIX.equals(method)) {
            publish(session, api, lifecycle);
            return;
        }
        throw new IllegalArgumentException("Unknown negative API scenario method: " + method);
    }

    public static String scenarioTitle(String method) {
        if (NEGATIVE_MATRIX.equals(method)) return NEGATIVE_MATRIX_TITLE;
        throw new IllegalArgumentException("Unknown negative API scenario method: " + method);
    }

    public static void publish(AtfReportPorts.Session session, ApiModule api,
                               ApiScenarioLifecycleRunner lifecycle) {
        lifecycle.run(session, ApiAcceptancePlanBuilder.scenario(
                        REPORT_OWNER, PACKAGE,
                        NEGATIVE_MATRIX_TITLE,
                        List.of("api", "negative", "error-contract", "mock-contract"))
                .body(phase -> {
                    publishOne(phase, api, ApiNegativeRoutes.invalidJson(),
                            400, "INVALID_JSON", "invalid JSON", "$.error.message");
                    publishOne(phase, api, ApiNegativeRoutes.missingBody(),
                            400, "BODY_REQUIRED", "missing body", "$.error.message");
                    publishOne(phase, api, ApiNegativeRoutes.wrongContentType(),
                            415, "UNSUPPORTED_MEDIA_TYPE", "wrong content type", "$.error.message");
                    publishOne(phase, api, ApiNegativeRoutes.missingTitle(),
                            422, "VALIDATION_FAILED", "missing required field", "$.error.fields[0].name");
                    publishOne(phase, api, ApiNegativeRoutes.conflict(),
                            409, "RESOURCE_CONFLICT", "conflict", "$.error.message");
                    publishOne(phase, api, ApiNegativeRoutes.forbiddenUpdate(),
                            403, "FORBIDDEN", "forbidden", "$.error.message");
                    publishOne(phase, api, ApiNegativeRoutes.unknownAuth(),
                            401, "UNKNOWN_AUTH_PROFILE", "unknown auth", "$.error.message");
                    publishOne(phase, api, ApiNegativeRoutes.serverError(),
                            500, "INTERNAL_ERROR", "server failure", "$.error.message");
                }).build());
    }

    private static void publishOne(AtfReportPorts.Phase phase, ApiModule api,
                                   ApiRequest request, int status, String code,
                                   String label, String detailPath) {
        var contract = new ResponseContract().status(status)
                .contentTypeContains(MediaTypes.APPLICATION_JSON)
                .jsonPathType("$.error", "object")
                .jsonPathEquals("$.error.code", code)
                .jsonPathRequired(detailPath);
        var execution = api.executor().execute(request);
        var decision = ApiAcceptancePublisher.publishNegativeDecision(
                phase,
                ApiOperationLabel.format(HttpMethod.valueOf(request.method()), request.path(), label),
                execution, contract, false, request);
        if (decision.status() == AtfReportStatus.FAIL) {
            throw new ApiScenarioLifecycleRunner.FailureSignal(
                    "Negative API contract failed for " + request.method() + " " + request.path());
        }
    }
}
