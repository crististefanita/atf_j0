package atf.ui.tests.extent.component.audit;

import atf.reporting.ui.audit.input.ReportAuditInputKind;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.audit.ReportAuditInputs;
import atf.ui.tests.extent.support.config.UiConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest
class ReportAuditInputsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void explicitConfiguredReportTakesPrecedenceOverGeneratedFallback() throws Exception {
        Path report = Files.writeString(temporaryDirectory.resolve("selected-report.html"),
                "<!doctype html><html></html>");
        UiConfiguration configuration = UiConfiguration.defaults(temporaryDirectory)
                .withAuditInputReport(report);

        var input = ReportAuditInputs.configuredOrGenerated(getClass(), configuration);

        assertEquals(report.toAbsolutePath().normalize(), input.report());
        assertEquals(ReportAuditInputKind.EXISTING, input.kind());
    }
}
