package atf.reporting.api.contract.internal;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.api.contract.ApiReportContractInput;

import atf.reporting.api.contract.ApiReportRouteContract;
import atf.reporting.audit.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Compares response JSON as a tree, independent of whitespace and field order. */
public final class ApiReportContractResponseJsonValidator {
    public ValidationResult validate(ApiReportRouteContract expected,
                                     ApiReportContractInput input) {
        String expectedDocument = expected.responseBody();
        String actualDocument = input.responseBody().trim();
        if (input.responseAvailability() == ReportBodyAvailability.ABSENT) {
            return expectedDocument.isEmpty() && input.responseBody().isEmpty()
                    ? ValidationResult.empty() : ValidationResult.ofViolations(List.of(
                    expected.key() + " confirmed absent response contradicts configured or rendered payload"));
        }
        if (input.responseAvailability() != ReportBodyAvailability.AVAILABLE) {
            return ValidationResult.ofViolations(List.of(expected.key() + " response body was not captured"));
        }
        if (expectedDocument.isEmpty()) {
            return ValidationResult.ofViolations(List.of(expected.key()
                    + " expected no response body but rendered a captured payload"));
        }
        if (actualDocument.isBlank()) {
            return ValidationResult.ofViolations(List.of(expected.key()
                    + " rendered an empty response JSON document"));
        }
        try {
            JsonNode expectedTree = ApiReportContractValueSupport.JSON.readTree(expectedDocument);
            JsonNode actualTree = ApiReportContractValueSupport.JSON.readTree(actualDocument);
            if (!expectedTree.equals(actualTree)) {
                return ValidationResult.ofViolations(List.of(expected.key()
                        + " response JSON differs from configured catalog body [expected="
                        + ApiReportContractValueSupport.compact(expectedTree) + ", actual="
                        + ApiReportContractValueSupport.compact(actualTree) + "]"));
            }
            return ValidationResult.empty();
        } catch (Exception error) {
            return ValidationResult.ofViolations(List.of(expected.key()
                    + " response JSON cannot be compared: " + error.getMessage()));
        }
    }
}
