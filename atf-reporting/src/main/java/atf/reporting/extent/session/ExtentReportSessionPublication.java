package atf.reporting.extent.session;
import atf.reporting.publication.ReportPublicationCoordinator;
import atf.reporting.publication.ReportStagingWorkspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Owns only the private staging and atomic publication boundary of an Extent session. */
public final class ExtentReportSessionPublication implements AutoCloseable {
    private final Path finalReport;
    private final ReportStagingWorkspace workspace;
    private final Path stagedReport;

    private ExtentReportSessionPublication(Path finalReport, ReportStagingWorkspace workspace) {
        this.finalReport = finalReport;
        this.workspace = workspace;
        this.stagedReport = workspace.directory().resolve(finalReport.getFileName());
    }

    public static ExtentReportSessionPublication open(Path reportPath) {
        Path finalReport = Objects.requireNonNull(reportPath, "reportPath")
                .toAbsolutePath().normalize();
        try {
            if (finalReport.getParent() != null) Files.createDirectories(finalReport.getParent());
            ReportStagingWorkspace workspace = ReportStagingWorkspace.create(
                    finalReport.getParent(), finalReport);
            return new ExtentReportSessionPublication(finalReport, workspace);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot create report staging workspace", error);
        }
    }

    Path stagedReport() { return stagedReport; }

    void publish() throws IOException {
        ExtentReportFinalizer.finalizeReport(stagedReport);
        ReportPublicationCoordinator.publishAtomic(stagedReport, finalReport);
    }

    @Override
    public void close() {
        workspace.close();
    }
}
