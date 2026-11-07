package atf.ui.tests.extent.component.execution;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.execution.UiTerminalExchangeProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
@ReportBrowserTest
class UiTerminalExchangeReportComponentTest {
    @ParameterizedTest(name = "Nested terminal exchange · fallback={0}, preview={1}")
    @CsvSource({"false,240", "true,240", "false,18", "true,18"})
    void terminalDiagnosticsRemainAuditableThroughEveryUiPublicationPath(boolean fallback, int limit) throws Exception {
        UiTerminalExchangeProbe.verify(fallback, limit);
    }
}
