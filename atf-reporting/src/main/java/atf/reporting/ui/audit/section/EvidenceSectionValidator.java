package atf.reporting.ui.audit.section;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.audit.ReportAuditSnapshot;

import java.util.List;
import java.util.Objects;

/** Pure contract for request/response/execution evidence and JSON payloads. */
public final class EvidenceSectionValidator {
    private final JsonEvidenceValidator jsonValidator = new JsonEvidenceValidator();

    public ValidationResult validate(ReportAuditSnapshot.RequestEvidence snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ReportAuditSnapshot.JsonEvidence json = snapshot.jsonEvidence();
        ValidationResult structure = ValidationResult.of(List.of(
                ReportAuditChecks.check("Evidence tables", "request + response + execution",
                        snapshot.requestTableCount() + " + " + snapshot.responseTableCount()
                                + " + " + snapshot.executionTableCount(),
                        snapshot.tablesVisible() && snapshot.requestTableCount() > 0
                                && snapshot.responseTableCount() > 0 && snapshot.executionTableCount() > 0),
                ReportAuditChecks.check("JSON evidence present",
                        "captured JSON or confirmed absent body on each side",
                        json.requestAvailability() + " / " + json.responseAvailability(),
                        json.compliant())));
        return structure.merge(jsonValidator.validate(json));
    }
}
