package atf.reporting.ui.audit.input;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable input contract shared by generated-report and existing-report audits. */
public record ReportAuditInput(Path report, ReportAuditInputKind kind) {
    public ReportAuditInput {
        report = Objects.requireNonNull(report, "report").toAbsolutePath().normalize();
        kind = Objects.requireNonNull(kind, "kind");
    }

    public static ReportAuditInput apiGenerated(Path report) {
        return new ReportAuditInput(report, ReportAuditInputKind.API_GENERATED);
    }

    public static ReportAuditInput existing(Path report) {
        return new ReportAuditInput(report, ReportAuditInputKind.EXISTING);
    }
}
