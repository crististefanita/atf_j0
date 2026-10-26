package atf.ui.tests.extent.component.inspection;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.disclosure.ReportBulkDisclosureProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ReportBulkDisclosureComponentTest {
    @Test
    void ownerControlsChangeTheEntireSemanticTreeWithoutChangingOtherOwners() {
        ReportBulkDisclosureProbe.verify();
    }
}
