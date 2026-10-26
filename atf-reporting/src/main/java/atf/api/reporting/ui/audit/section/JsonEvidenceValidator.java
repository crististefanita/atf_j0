package atf.api.reporting.ui.audit.section;
import atf.api.reporting.api.evidence.ReportBodyAvailability;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.audit.ReportAuditChecks;
import atf.api.reporting.audit.ValidationResult;
import atf.api.reporting.audit.ReportAuditSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure JSON evidence contract; it has no browser, locator or renderer dependency. */
public final class JsonEvidenceValidator {
    private static final ObjectMapper JSON = new ObjectMapper();

    public ValidationResult validate(ReportAuditSnapshot.JsonEvidence snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<AtfAssertion> assertions = new ArrayList<>();
        assertions.add(ReportAuditChecks.check("Request JSON", "valid JSON document or confirmed absent body",
                snapshot.requestAvailability() + ": " + snapshot.requestBody(),
                validBody(snapshot.requestBody(), snapshot.requestAvailability())));
        assertions.add(ReportAuditChecks.check("Response JSON", "valid JSON document or confirmed absent body",
                snapshot.responseAvailability() + ": " + snapshot.responseBody(),
                validBody(snapshot.responseBody(), snapshot.responseAvailability())));
        assertions.add(ReportAuditChecks.check("Extraction violations", "0",
                snapshot.violations().toString(), snapshot.violations().isEmpty()));
        return ValidationResult.of(assertions);
    }

    private static boolean validBody(String value, ReportBodyAvailability availability) {
        if (availability == ReportBodyAvailability.ABSENT) return value.isEmpty();
        if (availability != ReportBodyAvailability.AVAILABLE) return false;
        if (value == null || value.isBlank()) return false;
        try {
            return JSON.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(value) != null;
        } catch (JsonProcessingException error) {
            return false;
        }
    }
}
