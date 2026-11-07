package atf.ui.tests.extent.component.flow;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.flow.BodyOnlyUnifiedFlowProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
@ReportBrowserTest
class BodyOnlyUnifiedFlowComponentTest {
    @ParameterizedTest
    @CsvSource({"false,240,1280", "true,240,1280", "false,18,392", "true,18,392"})
    void oneBodyFlowKeepsItsTablesAndKeyboardDisclosure(boolean fallback, int limit, int width) throws Exception {
        BodyOnlyUnifiedFlowProbe.verify(fallback, limit, width);
    }
}
