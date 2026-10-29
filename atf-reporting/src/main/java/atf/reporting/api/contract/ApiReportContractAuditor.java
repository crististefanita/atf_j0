package atf.reporting.api.contract;

import atf.reporting.api.contract.internal.ApiReportContractResponseJsonValidator;
import atf.reporting.api.contract.internal.ApiReportExecutionEvidenceValidator;
import atf.reporting.api.contract.internal.ApiReportRequestBodyValidator;
import atf.reporting.api.contract.internal.ApiReportRequestMetadataValidator;
import atf.reporting.api.contract.internal.ApiReportResponseMetadataValidator;
import atf.reporting.api.contract.internal.ApiReportRouteResolver;
import atf.reporting.audit.ValidationResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure contract engine for an API report node.
 *
 * <p>The auditor knows neither Playwright nor the mock module. A catalog
 * adapter supplies expectations, while a DOM or static-HTML adapter supplies
 * {@link ApiReportContractInput}. This makes the same rules usable as unit,
 * component and integration checks.</p>
 */
public final class ApiReportContractAuditor {
    private final ApiReportRouteResolver routeResolver;
    private final ApiReportRequestBodyValidator requestBodyValidator = new ApiReportRequestBodyValidator();
    private final ApiReportRequestMetadataValidator requestMetadataValidator = new ApiReportRequestMetadataValidator();
    private final ApiReportResponseMetadataValidator responseMetadataValidator = new ApiReportResponseMetadataValidator();
    private final ApiReportExecutionEvidenceValidator executionEvidenceValidator = new ApiReportExecutionEvidenceValidator();
    private final ApiReportContractResponseJsonValidator responseJsonValidator = new ApiReportContractResponseJsonValidator();

    public ApiReportContractAuditor(ApiReportContractCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        routeResolver = new ApiReportRouteResolver(catalog.routes());
    }

    public Result inspect(ApiReportContractInput input) {
        Objects.requireNonNull(input, "input");
        ApiReportRouteResolver.Resolution resolution = routeResolver.resolve(
                input.requestTitle(), input.requestUrl(), input.actualStatus());
        ValidationResult validation = resolution.validation()
                .merge(ValidationResult.ofViolations(input.evidenceViolations()));
        if (resolution.route().isEmpty()) {
            return new Result(input.requestTitle(), Optional.empty(), validation.violations());
        }

        ApiReportRouteContract expected = resolution.route().orElseThrow();
        validation = validation.merge(requestBodyValidator.validate(expected, input));
        if (!input.requestFields().isEmpty()) {
            validation = validation.merge(requestMetadataValidator.validateMetadata(expected, input));
        }
        if (!input.responseFields().isEmpty()) {
            validation = validation.merge(responseMetadataValidator.validate(expected, input));
        }
        if (!input.executionFields().isEmpty()) {
            validation = validation.merge(executionEvidenceValidator.validate(expected, input));
        }
        validation = validation.merge(responseJsonValidator.validate(expected, input));
        return new Result(input.requestTitle(), Optional.of(expected), validation.violations());
    }

    public record Result(String requestTitle, Optional<ApiReportRouteContract> route,
                         List<String> violations) {
        public Result {
            requestTitle = requestTitle == null ? "" : requestTitle;
            route = route == null ? Optional.empty() : route;
            violations = violations == null ? List.of() : List.copyOf(violations);
        }

        public boolean compliant() {
            return violations.isEmpty();
        }

        public ValidationResult validation() {
            return ValidationResult.ofViolations(violations);
        }
    }
}
