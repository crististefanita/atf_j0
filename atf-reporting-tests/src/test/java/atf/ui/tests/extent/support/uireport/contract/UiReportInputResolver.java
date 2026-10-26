package atf.ui.tests.extent.support.uireport.contract;
import atf.reporting.ui.contract.UiReportContractSpec;
import atf.reporting.artifact.ReportArtifactIdentity;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.ui.tests.extent.support.config.UiConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves a previous UI report without falling back to a generated report. */
public final class UiReportInputResolver {
    private UiReportInputResolver() { }

    public static UiReportInput resolve(Class<?> owner, UiReportContractSpec spec,
                                        UiConfiguration configuration) {
        if (owner == null) throw new NullPointerException("owner");
        if (spec == null) throw new NullPointerException("spec");
        String configured = System.getProperty(spec.reportProperty());
        if ((configured == null || configured.isBlank())
                && "atf.ui.report".equals(spec.reportProperty())
                && configuration != null) {
            configured = configuration.auditInputReport().map(Path::toString).orElse(null);
        }
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing static UI Extent report property -D"
                    + spec.reportProperty());
        }
        Path report = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isRegularFile(report)) {
            throw new IllegalArgumentException("Static UI Extent report does not exist: " + report);
        }
        var inspection = ReportArtifactInspector.inspect(report);
        if (!inspection.readError().isEmpty() || !inspection.satisfies("html exists")
                || !inspection.satisfies("doctype")) {
            throw new IllegalArgumentException("Static UI Extent report is not readable HTML: "
                    + inspection.violations());
        }
        try {
            if (Files.size(report) == 0L) {
                throw new IllegalArgumentException("Static UI Extent report is empty: " + report);
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot read static UI Extent report: " + report, error);
        }
        return new UiReportInput(report, ReportArtifactIdentity.read(report));
    }
}
