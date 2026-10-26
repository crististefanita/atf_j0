package atf.ui.tests.extent.component.support;
import atf.testkit.AtfComponentTest;
import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.reporting.ui.audit.input.ReportAuditInputKind;
import atf.ui.tests.extent.support.audit.ReportAuditInputs;
import atf.reporting.ui.contract.ReportUiContract;

import atf.ui.tests.extent.support.config.UiConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest
class ReportAuditInputsTest {
    @Test
    void resolvesConfiguredExistingReportWithoutChangingItsPath() throws Exception {
        Path root = Files.createTempDirectory(ReportUiContract.INPUT_SOURCE_TEMP_PREFIX);
        Path report = root.resolve("input.html");
        UiConfiguration configuration = UiConfiguration.defaults(root).withAuditInputReport(report);

        ReportAuditInput input = ReportAuditInputs.existing().resolve(configuration);

        assertEquals(report.toAbsolutePath().normalize(), input.report());
        assertEquals(ReportAuditInputKind.EXISTING, input.kind());
    }

    @Test
    void keepsGeneratedInputDistinctFromAnExistingInput() throws Exception {
        Path root = Files.createTempDirectory(ReportUiContract.INPUT_SOURCE_TEMP_PREFIX);
        Path report = root.resolve("generated.html");
        ReportAuditInput input = ReportAuditInputs.generated(report).resolve(UiConfiguration.defaults(root));

        assertEquals(report.toAbsolutePath().normalize(), input.report());
        assertEquals(ReportAuditInputKind.API_GENERATED, input.kind());
    }

    @Test
    void failsClearlyWhenExistingInputWasNotConfigured() throws Exception {
        Path root = Files.createTempDirectory(ReportUiContract.INPUT_SOURCE_TEMP_PREFIX);
        assertThrows(IllegalStateException.class,
                () -> ReportAuditInputs.existing().resolve(UiConfiguration.defaults(root)));
    }
}
