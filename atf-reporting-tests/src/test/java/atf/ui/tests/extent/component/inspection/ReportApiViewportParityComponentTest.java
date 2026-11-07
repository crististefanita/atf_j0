package atf.ui.tests.extent.component.inspection;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.viewport.ReportApiViewportProbe;
import org.junit.jupiter.api.Test;

/** Browser proof for the fallback projection of short and long API tables. */
@AtfComponentTest
@ReportBrowserTest
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
