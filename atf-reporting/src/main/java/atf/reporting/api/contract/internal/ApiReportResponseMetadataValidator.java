package atf.reporting.api.contract.internal;

import atf.reporting.api.ReportHttpHeaders;
import atf.reporting.api.contract.ApiReportEvidenceField;
import atf.reporting.api.contract.ApiReportRouteContract;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.audit.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Validates response status, content type and response headers. */
public final class ApiReportResponseMetadataValidator {
    public ValidationResult validate(ApiReportRouteContract expected, ApiReportContractInput input) {
        List<String> violations = new ArrayList<>();
        Map<String, String> actualHeaders = ApiReportContractValueSupport.parseMap(
                input.responseFields().get(ApiReportEvidenceField.RESPONSE_HEADERS.key()));
        expected.responseHeaders().entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase(ReportHttpHeaders.CONTENT_TYPE))
                .forEach(entry -> {
                    String actual = ApiReportContractValueSupport.caseInsensitiveValue(actualHeaders, entry.getKey());
                    if (!entry.getValue().equals(actual)) {
                        violations.add(expected.key() + " expected response header " + entry.getKey()
                                + "=" + entry.getValue() + " but report rendered "
                                + (actual == null ? "<missing>" : actual));
                    }
                });
        List<String> contentTypes = ApiReportEvidenceField.CONTENT_TYPE.valuesIn(input.responseFields());
        String expectedContentType = ApiReportContractValueSupport.caseInsensitiveValue(
                expected.responseHeaders(), ReportHttpHeaders.CONTENT_TYPE);
        if (expectedContentType != null && (contentTypes.isEmpty()
                || contentTypes.stream().anyMatch(actual -> !expectedContentType.equals(actual)))) {
            violations.add(expected.key() + " expected CONTENT-TYPE " + expectedContentType
                    + " but report rendered " + (contentTypes.isEmpty() ? "<missing>" : contentTypes));
        }
        String renderedStatus = ApiReportContractValueSupport.value(input.responseFields(),
                ApiReportEvidenceField.HTTP_STATUS.key());
        if (!renderedStatus.isBlank() && !String.valueOf(input.actualStatus()).equals(renderedStatus)) {
            violations.add(expected.key() + " response evidence status differs from execution status");
        }
        return ValidationResult.ofViolations(violations);
    }
}
