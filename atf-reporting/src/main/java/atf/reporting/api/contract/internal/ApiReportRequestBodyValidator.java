package atf.reporting.api.contract.internal;
import atf.reporting.api.evidence.ReportBodyAvailability;

import atf.reporting.api.ReportHttpHeaders;
import atf.reporting.api.ReportJsonBodyPolicy;
import atf.reporting.api.contract.ApiReportEvidenceField;
import atf.reporting.api.contract.ApiReportRequestBodyMode;
import atf.reporting.api.contract.ApiReportRouteContract;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.audit.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/** Validates JSON write evidence and the explicit response JSON channel. */
public final class ApiReportRequestBodyValidator {
    public ValidationResult validate(ApiReportRouteContract expected, ApiReportContractInput input) {
        if (!ReportJsonBodyPolicy.appliesTo(expected.method())) {
            return ValidationResult.empty();
        }
        List<String> violations = new ArrayList<>();
        String body = input.requestBody().trim();
        boolean hasPayload = ApiReportContractValueSupport.hasPayload(body);
        boolean validJson = ApiReportContractValueSupport.isJson(body);
        String renderedHeaders = ApiReportContractValueSupport.value(input.requestFields(),
                ApiReportEvidenceField.REQUEST_HEADERS.key()).toLowerCase(java.util.Locale.ROOT);
        String jsonHeader = (ReportHttpHeaders.CONTENT_TYPE + "=application/json")
                .toLowerCase(java.util.Locale.ROOT);
        boolean jsonContentType = renderedHeaders.contains(jsonHeader);
        switch (ApiReportRequestBodyMode.from(expected.requestBodyMode())) {
            case JSON -> {
                if (!hasPayload) violations.add(expected.key() + " must expose a non-empty JSON request body");
                else if (!validJson) violations.add(expected.key() + " must expose a valid JSON request body");
                if (!renderedHeaders.isBlank() && !jsonContentType) {
                    violations.add(expected.key() + " must expose " + ReportHttpHeaders.CONTENT_TYPE
                            + " application/json");
                }
            }
            case WRONG_CONTENT_TYPE -> {
                if (!hasPayload || !validJson) violations.add(expected.key()
                        + " must expose a valid JSON body for the wrong-content-type outcome");
                if (!renderedHeaders.isBlank() && jsonContentType) {
                    violations.add(expected.key()
                            + " must expose a non-JSON Content-Type for the wrong-content-type outcome");
                }
            }
        }
        if (expected.expectedRequestBody() != null && validJson
                && !ApiReportContractValueSupport.sameJson(expected.expectedRequestBody(), body)) {
            violations.add(expected.key() + " request JSON differs from configured request-body");
        }
        boolean absentResponse = input.responseAvailability() ==
                ReportBodyAvailability.ABSENT
                && input.responseBody().isEmpty() && expected.responseBody().isEmpty();
        if (!absentResponse && (!ApiReportContractValueSupport.hasPayload(input.responseBody())
                || !ApiReportContractValueSupport.isJson(input.responseBody()))) {
            violations.add(expected.key() + " must expose a non-empty JSON response body");
        }
        return ValidationResult.ofViolations(violations);
    }
}
