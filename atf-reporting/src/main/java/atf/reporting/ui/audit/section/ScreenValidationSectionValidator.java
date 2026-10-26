package atf.reporting.ui.audit.section;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.audit.ReportAuditSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure contract for the three compact UI validation evidence zones. */
public final class ScreenValidationSectionValidator {
    public ValidationResult validate(ReportAuditSnapshot.ScreenValidation snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<AtfAssertion> assertions = new ArrayList<>();
        assertions.add(ReportAuditChecks.check("UI validation node", "1",
                String.valueOf(snapshot.sectionCount()), snapshot.sectionCount() == 1));
        assertions.add(ReportAuditChecks.check("Evidence zones", "action + observations + assertions",
                snapshot.actionSequenceCount() + " + " + snapshot.observationCount() + " + "
                        + snapshot.assertionTableCount(), snapshot.actionSequenceCount() == 1
                        && snapshot.observationCount() == 1 && snapshot.assertionTableCount() == 1));
        assertions.add(ReportAuditChecks.check("Sibling rows", "3",
                String.valueOf(snapshot.zoneRowCount()), snapshot.zonesAreDirectSiblings()
                        && snapshot.zoneRowCount() == 3));
        assertions.add(ReportAuditChecks.check("Vertical composition", "ordered and gap-free",
                String.valueOf(snapshot.zonesStacked()), snapshot.zonesStacked()
                        && snapshot.noEmptyRows() && snapshot.noDecorativeSeparators()));
        assertions.add(ReportAuditChecks.check("Inline screenshot", "> 0 canonical links",
                snapshot.canonicalScreenshotLinkCount() + "/" + snapshot.screenshotLinkCount(),
                snapshot.inlineScreenshot()));
        return ValidationResult.of(assertions).merge(
                ValidationResult.ofViolations(snapshot.violations()));
    }
}
