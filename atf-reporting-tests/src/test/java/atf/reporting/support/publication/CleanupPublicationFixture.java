package atf.reporting.support.publication;

import atf.reporting.extent.session.ExtentReportTimestampReporter;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.theme.AtfReportTheme;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Raw vendor input for the real ATF publication pipeline, without a browser or business fixture. */
public final class CleanupPublicationFixture {
    private CleanupPublicationFixture() { }

    public static Path rawReport(Path report) throws IOException {
        Files.createDirectories(report.getParent());
        var spark = new ExtentReportTimestampReporter(report.toString());
        spark.config().setOfflineMode(true);
        spark.config().setTheme(Theme.DARK);
        spark.config().setCss(AtfReportTheme.sparkCss());
        spark.config().setDocumentTitle(AtfReportLabels.REPORT_TITLE);
        spark.config().setReportName(AtfReportLabels.REPORT_TITLE);
        var reports = new ExtentReports();
        reports.attachReporter(spark);
        reports.createTest("AssetCleanupContract").assignCategory("component", "cleanup")
                .createNode("Complete report survives an explicit cleanup failure")
                .createNode("TEST BODY")
                .createNode("THEN the report stays complete")
                .createNode("Publication evidence")
                .pass("Synthetic cleanup fixture; not a business execution");
        reports.flush();
        return report;
    }

    public static List<Path> temporaryOutputs(Path report) throws IOException {
        try (var entries = Files.list(report.getParent())) {
            return entries.filter(path -> path.getFileName().toString().endsWith(".tmp")).toList();
        }
    }
}
