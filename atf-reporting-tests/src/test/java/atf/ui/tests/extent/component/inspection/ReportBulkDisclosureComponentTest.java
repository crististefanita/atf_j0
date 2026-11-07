package atf.ui.tests.extent.component.inspection;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.disclosure.ReportBulkDisclosureProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
@ReportBrowserTest
class ReportBulkDisclosureComponentTest {
    @Test
    void ownerControlsChangeTheEntireSemanticTreeWithoutChangingOtherOwners() {
        ReportBulkDisclosureProbe.verify();
    }
}
