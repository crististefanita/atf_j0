package atf.ui.tests.extent.component.inspection;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.viewport.ReportApiViewportProbe;
import org.junit.jupiter.api.Test;

/** Browser proof for the fallback projection of short and long API tables. */
@AtfComponentTest
class ReportApiViewportParityComponentTest {
    @Test
    void extentShortRequestAndRetryTablesKeepNaturalHeightAtBothViewportWidths() {
        ReportApiViewportProbe.verifyExtentShortTables();
    }

    @Test
    void fallbackRequestAndRetryTablesStayBoundedAtBothViewportWidths() {
        ReportApiViewportProbe.verifyFallbackTables();
    }
}
