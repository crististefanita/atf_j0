package atf.reporting.api.contract.internal;

import atf.reporting.api.contract.ApiReportEvidenceField;
import atf.reporting.api.contract.ApiReportRouteContract;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.audit.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/** Validates execution evidence against the request card identity. */
public final class ApiReportExecutionEvidenceValidator {
    public ValidationResult validate(ApiReportRouteContract expected, ApiReportContractInput input) {
        List<String> violations = new ArrayList<>();
        if (ApiReportEvidenceField.RESPONSE_STATUS.valuesIn(input.executionFields()).stream()
                .anyMatch(status -> !String.valueOf(input.actualStatus()).equals(status))) {
            violations.add(expected.key() + " execution evidence response.status differs from report status");
        }
        if (!input.requestUrl().isBlank()
                && ApiReportEvidenceField.RESPONSE_URL.valuesIn(input.executionFields()).stream()
                .anyMatch(url -> !input.requestUrl().equals(url))) {
            violations.add(expected.key() + " execution evidence response.url differs from request URL");
        }
        return ValidationResult.ofViolations(violations);
    }
}
