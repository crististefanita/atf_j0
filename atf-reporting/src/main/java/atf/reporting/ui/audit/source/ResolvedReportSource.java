package atf.reporting.ui.audit.source;

import atf.reporting.artifact.ReportArtifactIdentity;
import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.reporting.ui.audit.plan.UiExtentReportSource;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable, fingerprinted report input resolved before browser lifecycle. */
public record ResolvedReportSource(UiExtentReportSource source,
                                   ReportAuditInput input,
                                   ReportArtifactIdentity identity) {
    public ResolvedReportSource {
        source = Objects.requireNonNull(source, "source");
        input = Objects.requireNonNull(input, "input");
        identity = Objects.requireNonNull(identity, "identity");
        Path inputPath = input.report().toAbsolutePath().normalize();
        if (!inputPath.equals(identity.path())) {
            throw new IllegalArgumentException("Report identity path differs from input path");
        }
    }

    public boolean readOnly() {
        return true;
    }
}
