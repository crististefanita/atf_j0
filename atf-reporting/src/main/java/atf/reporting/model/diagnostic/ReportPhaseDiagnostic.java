package atf.reporting.model.diagnostic;

import atf.reporting.model.ReportField;
import atf.reporting.contract.AtfReportStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Explicit execution diagnostics, distinct from internal phase bookkeeping and HTTP contracts. */
public record ReportPhaseDiagnostic(String phase, Kind kind, String message, AtfReportStatus status) {
    public ReportPhaseDiagnostic(String phase, Kind kind, String message) {
        this(phase, kind, message, AtfReportStatus.INFO);
    }

    public ReportPhaseDiagnostic {
        phase = phase == null ? "" : phase;
        Objects.requireNonNull(kind, "kind");
        message = message == null ? "" : message;
        status = AtfReportStatus.orInfo(status);
    }

    /** Delimited compatibility seam for phase details emitted by existing lifecycle adapters. */
    public static List<ReportPhaseDiagnostic> from(String phase, List<ReportField> details) {
        return from(phase, details, AtfReportStatus.INFO);
    }

    public static List<ReportPhaseDiagnostic> from(String phase, List<ReportField> details, AtfReportStatus status) {
        return details.stream().filter(Objects::nonNull)
                .flatMap(field -> Kind.forLabel(field.name()).stream()
                        .map(kind -> new ReportPhaseDiagnostic(phase, kind, field.value(), status)))
                .toList();
    }

    public enum Kind {
        EXECUTION_FAILURE("Failure cause"),
        JUNIT_FAILURE("JUnit failure"),
        CONTRACT_ASSOCIATION("API contract association");

        private final String label;

        Kind(String label) { this.label = label; }

        public String label() { return label; }

        private static Optional<Kind> forLabel(String label) {
            String normalized = label == null ? "" : label.trim();
            return Arrays.stream(values()).filter(kind -> kind.label.equalsIgnoreCase(normalized)).findFirst();
        }
    }
}
