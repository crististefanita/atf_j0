package atf.ui.tests.extent.component.inspection.table;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.table.TableViewportProbe;
import org.junit.jupiter.api.Test;

/** Intent only; the neutral fixture and browser mechanics stay in module support. */
@AtfComponentTest
@ReportBrowserTest
class TableViewportBrowserComponentTest {
    @Test
    void everyDataFamilyUsesSystemCapForNaturalLongTallAndEmptyContent() {
        TableViewportProbe.verifyGeometryMatrix();
    }

    @Test
    void realReportsKeepSetupActionsAssertionsAndApiEvidenceBoundedInAllPhases() {
        TableViewportProbe.verifyPublishedReports();
    }

    @Test
    void missingViewportWrongCapClippingAndDoubleWrappingAreRejected() {
        TableViewportProbe.verifyNegativeCases();
    }
}
