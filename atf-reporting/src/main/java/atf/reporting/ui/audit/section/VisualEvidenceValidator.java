package atf.reporting.ui.audit.section;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.audit.ReportAuditSnapshot;

import java.util.List;
import java.util.Objects;

/** Pure contract for legacy visual evidence and actionable screenshot facts. */
public final class VisualEvidenceValidator {
    public ValidationResult validate(ReportAuditSnapshot.VisualEvidence snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        boolean actionContract = snapshot.actionSequenceCount() == 0
                || (snapshot.screenshotLinkCount() > 0 && snapshot.responsiveActionTable()
                && snapshot.observedTextIsStructured());
        return ValidationResult.of(List.of(
                ReportAuditChecks.check("Validation tables", "> 0",
                        String.valueOf(snapshot.validationCount()), snapshot.validationCount() > 0),
                ReportAuditChecks.check("Collapsible evidence", "> 0",
                        String.valueOf(snapshot.disclosureCount()), snapshot.disclosureCount() > 0),
                ReportAuditChecks.check("Screenshot anchors", "one canonical link per visual state",
                        snapshot.screenshotLinkCount() + " / " + snapshot.canonicalScreenshotLinks(),
                        snapshot.canonicalScreenshotLinks() == snapshot.screenshotLinkCount()),
                ReportAuditChecks.check("Nested screenshot images", "0",
                        String.valueOf(snapshot.nestedScreenshotImages()), snapshot.nestedScreenshotImages() == 0),
                ReportAuditChecks.check("Action table contract", "structured responsive table",
                        snapshot.observedActionText(), actionContract)));
    }

    public ValidationResult validateScreenshot(ReportAuditSnapshot.ScreenshotEvidence snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return ValidationResult.of(List.of(
                ReportAuditChecks.check("Screenshot payload", "valid PNG data URI in href",
                        String.valueOf(snapshot.validPngPayload()), snapshot.validPngPayload()),
                ReportAuditChecks.check("New-page target", "_blank", snapshot.target(),
                        "_blank".equals(snapshot.target())),
                ReportAuditChecks.check("Safe opener policy", "rel contains noopener", snapshot.rel(),
                        snapshot.rel().contains("noopener")),
                ReportAuditChecks.check("Lightbox contract", "data-featherlight=image",
                        snapshot.featherlight(), "image".equals(snapshot.featherlight())),
                ReportAuditChecks.check("Duplicate payload policy", "one unique payload per visual state",
                        String.valueOf(snapshot.distinctPayloadCount()), snapshot.distinctPayloadCount() == 1),
                ReportAuditChecks.check("Accessible marker", "📷 base64 img", snapshot.markerText(),
                        snapshot.markerText().contains("📷 base64 img")),
                ReportAuditChecks.check("Vertical alignment", "centered; transform none; overflow visible",
                        snapshot.geometry(), snapshot.centeredVertically()),
                ReportAuditChecks.check("Image navigation", "Featherlight overlay shows data:image/png;base64,...",
                        String.valueOf(snapshot.openedBase64Image()), snapshot.openedBase64Image()),
                ReportAuditChecks.check("Inline image cardinality", "0 nested images",
                        String.valueOf(snapshot.inlineImageCount()), snapshot.inlineImageCount() == 0)));
    }
}
