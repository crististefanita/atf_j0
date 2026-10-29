package atf.ui.tests.extent.component.failurecontext;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.failurecontext.UiTimeoutReportProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
class UiTimeoutReportComponentTest {
    @ParameterizedTest(name = "Timeout diagnostics · fallback={0}, preview={1}")
    @CsvSource({"false,240", "true,240", "false,18", "true,18"})
    void nativeDiagnosticIsRedactedAndInspectableThroughTheFailureBacklink(boolean fallback, int limit) throws Exception {
        UiTimeoutReportProbe.verify(fallback, limit);
    }
}
