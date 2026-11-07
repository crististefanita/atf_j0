package atf.ui.tests.extent.component.flow;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.flow.PhaseUnifiedFlowProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
@ReportBrowserTest
class PhaseUnifiedFlowComponentTest {
    @ParameterizedTest
    @CsvSource({"false,240,1280", "true,240,1280", "false,18,392", "true,18,392"})
    void phaseLocalEvidenceRetainsContextAndCollapsibleAssertions(boolean fallback, int limit, int width) throws Exception {
        PhaseUnifiedFlowProbe.verify(fallback, limit, width);
    }
}
