package atf.reporting.evidence.markup;

import atf.reporting.contract.AtfReportStatus;

import java.util.Objects;

/** Renderer-neutral input for one collapsible evidence section. */
public record ReportEvidenceDisclosure(
        String kind,
        String title,
        String body,
        String evidenceType,
        AtfReportStatus status) {

    public ReportEvidenceDisclosure {
        kind = Objects.requireNonNullElse(kind, "");
        title = Objects.requireNonNullElse(title, "");
        body = Objects.requireNonNullElse(body, "");
        evidenceType = normalize(evidenceType);
    }

    public static ReportEvidenceDisclosure standard(String kind, String title, String body) {
        return new ReportEvidenceDisclosure(kind, title, body, null, null);
    }

    public static ReportEvidenceDisclosure withEvidenceStatus(String kind, String title,
                                                               String body, String evidenceType,
                                                               AtfReportStatus status) {
        return new ReportEvidenceDisclosure(kind, title, body, evidenceType, status);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
