package atf.reporting.api.tests.api.negative;
import atf.testkit.AtfIntegrationTest;

import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiOperationLabel;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpMethod;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.api.tests.support.junit.ApiResourceTestBase;
import atf.validation.contract.ResponseContract;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Explicit wire/error matrix: status, JSON shape and request-body semantics. */
@AtfModuleTest(requires = {AtfModule.API, AtfModule.REPORTING})
@AtfIntegrationTest
class ApiNegativeMatrixTests extends ApiResourceTestBase {
    @Test
    void validatesBadPayloadsAuthFailuresConflictAndServerFailure() {
        var cases = List.of(
                new Case(ApiNegativeRoutes.invalidJson(), 400, "INVALID_JSON", "invalid JSON", "$.error.message"),
                new Case(ApiNegativeRoutes.missingBody(), 400, "BODY_REQUIRED", "missing body", "$.error.message"),
                new Case(ApiNegativeRoutes.wrongContentType(), 415, "UNSUPPORTED_MEDIA_TYPE", "wrong content type", "$.error.message"),
                new Case(ApiNegativeRoutes.missingTitle(), 422, "VALIDATION_FAILED", "missing required field", "$.error.fields[0].name"),
                new Case(ApiNegativeRoutes.conflict(), 409, "RESOURCE_CONFLICT", "conflict", "$.error.message"),
                new Case(ApiNegativeRoutes.forbiddenUpdate(), 403, "FORBIDDEN", "forbidden", "$.error.message"),
                new Case(ApiNegativeRoutes.unknownAuth(), 401, "UNKNOWN_AUTH_PROFILE", "unknown auth", "$.error.message"),
                new Case(ApiNegativeRoutes.serverError(), 500, "INTERNAL_ERROR", "server failure", "$.error.message"));

        assertAll(cases.stream().map(this::validate).toList());
    }

    private org.junit.jupiter.api.function.Executable validate(Case testCase) {
        return () -> {
            ApiRequest request = testCase.request();
            ApiExecutionResult execution = fixture().api().executor().execute(request);
            var decision = validateNegative(ApiOperationLabel.format(HttpMethod.valueOf(request.method()), request.path(), testCase.label()),
                    execution, new ResponseContract().status(testCase.status())
                            .jsonPathType("$.error", "object")
                            .jsonPathEquals("$.error.code", testCase.code())
                            .jsonPathRequired(testCase.detailPath()));
            assertEquals(testCase.status(), execution.response().status());
            assertEquals(AtfReportStatus.PASS, decision.status());
        };
    }

    private record Case(ApiRequest request, int status, String code, String label, String detailPath) { }
}
