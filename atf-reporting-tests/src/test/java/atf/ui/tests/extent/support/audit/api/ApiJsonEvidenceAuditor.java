package atf.ui.tests.extent.support.audit.api;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.audit.ValidationResult;

import com.microsoft.playwright.Locator;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.section.extract.ApiJsonEvidenceExtractor;
import atf.reporting.ui.audit.section.JsonEvidenceValidator;

import java.util.List;

/**
 * Read-only browser audit for the two JSON payload disclosures of one API
 * request node. The caller owns navigation and disclosure clicks; this class
 * only evaluates the rendered evidence contract. A body-less HTTP exchange
 * is compliant only with explicit ABSENT capture metadata. Literal JSON
 * {@code null} is a captured payload; missing capture remains a failed audit.
 */
public final class ApiJsonEvidenceAuditor {
    private final ApiJsonEvidenceExtractor extractor;
    private final JsonEvidenceValidator validator = new JsonEvidenceValidator();

    public ApiJsonEvidenceAuditor() {
        this(new ReportUiLocators());
    }

    public ApiJsonEvidenceAuditor(ReportUiLocators locators) {
        this.extractor = new ApiJsonEvidenceExtractor(locators);
    }

    public Result inspect(Locator requestNode) {
        var snapshot = extractor.extract(requestNode);
        ValidationResult validation = validator.validate(snapshot);
        return new Result(snapshot.requestBody(), snapshot.responseBody(), validation.violations(),
                snapshot.requestAvailability(), snapshot.responseAvailability());
    }

    public record Result(String requestBody, String responseBody, List<String> violations,
                         ReportBodyAvailability requestAvailability,
                         ReportBodyAvailability responseAvailability) {
        public Result(String requestBody, String responseBody, List<String> violations) {
            this(requestBody, responseBody, violations,
                    new ReportAuditSnapshot.JsonEvidence(requestBody, responseBody, violations)
                            .requestAvailability(),
                    new ReportAuditSnapshot.JsonEvidence(requestBody, responseBody, violations)
                            .responseAvailability());
        }
        public Result {
            violations = violations == null ? List.of() : List.copyOf(violations);
        }

        public boolean compliant() {
            return new ReportAuditSnapshot.JsonEvidence(requestBody, responseBody, violations,
                    requestAvailability, responseAvailability).compliant();
        }

        public ValidationResult validation() {
            return ValidationResult.ofViolations(violations);
        }

        public boolean requestHasPayload() {
            return isJsonDocument(requestBody);
        }

        public boolean responseHasPayload() {
            return isJsonDocument(responseBody);
        }

        private static boolean isJsonDocument(String body) {
            return body != null && !body.isBlank();
        }

    }
}
