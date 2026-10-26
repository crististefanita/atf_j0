package atf.ui.tests.extent.support.uireport.contract;
import atf.reporting.artifact.ReportArtifactIdentity;

import java.nio.file.Path;
import java.util.Objects;

/** Read-only, fingerprinted input for a UI report contract audit. */
public record UiReportInput(Path report, ReportArtifactIdentity identity) {
    public UiReportInput {
        report = Objects.requireNonNull(report, "report").toAbsolutePath().normalize();
        identity = Objects.requireNonNull(identity, "identity");
        if (!report.equals(identity.path())) {
            throw new IllegalArgumentException("UI report identity path differs from input path");
        }
    }
}
