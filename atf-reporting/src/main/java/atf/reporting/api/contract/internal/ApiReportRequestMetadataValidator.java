package atf.reporting.api.contract.internal;

import atf.reporting.api.ReportQueryParameter;
import atf.reporting.api.ReportQueryParameters;
import atf.reporting.api.contract.ApiReportEvidenceField;
import atf.reporting.api.contract.ApiReportRouteContract;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.audit.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Validates auth profile, query parameters and request headers. */
public final class ApiReportRequestMetadataValidator {
    public ValidationResult validate(ApiReportRouteContract expected, ApiReportContractInput input) {
        return authentication(expected, input)
                .merge(validateQuery(expected, ApiReportContractValueSupport.queryFromUrl(input.requestUrl())))
                .merge(headers(expected, input));
    }

    /** Query validity is already checked by route resolution, even without rendered metadata. */
    public ValidationResult validateMetadata(ApiReportRouteContract expected, ApiReportContractInput input) {
        return authentication(expected, input).merge(headers(expected, input));
    }

    private ValidationResult authentication(ApiReportRouteContract expected, ApiReportContractInput input) {
        List<String> violations = new ArrayList<>();
        String actualProfile = ApiReportContractValueSupport.value(input.requestFields(),
                ApiReportEvidenceField.AUTH_PROFILE.key()).trim();
        if (!ApiReportRouteContract.ANY_AUTH_PROFILE.equals(expected.authProfile())
                && !expected.authProfile().equals(actualProfile)) {
            violations.add(expected.key() + " expected AUTH PROFILE " + expected.authProfile()
                    + " but report rendered " + (actualProfile.isBlank() ? "<missing>" : actualProfile));
        }
        return ValidationResult.ofViolations(violations);
    }

    public static ValidationResult validateQuery(ApiReportRouteContract expected, ReportQueryParameters query) {
        List<String> violations = new ArrayList<>();
        if (!query.containsAll(expected.queryParameters())) {
            String queryNames = expected.queryParameters().values().stream()
                    .map(ReportQueryParameter::name).distinct()
                    .collect(Collectors.joining(","));
            violations.add(expected.key() + " expected query " + queryNames + " parameters "
                    + expected.queryParameters().display() + " but report rendered " + query.display());
        }
        return ValidationResult.ofViolations(violations);
    }

    private ValidationResult headers(ApiReportRouteContract expected, ApiReportContractInput input) {
        List<String> violations = new ArrayList<>();
        Map<String, String> actualHeaders = ApiReportContractValueSupport.parseMap(
                input.requestFields().get(ApiReportEvidenceField.REQUEST_HEADERS.key()));
        expected.requiredHeaders().forEach((key, value) -> {
            String actual = ApiReportContractValueSupport.caseInsensitiveValue(actualHeaders, key);
            if (!value.equals(actual)) {
                violations.add(expected.key() + " expected request header " + key + "=" + value
                        + " but report rendered " + (actual == null ? "<missing>" : actual));
            }
        });
        return ValidationResult.ofViolations(violations);
    }
}
