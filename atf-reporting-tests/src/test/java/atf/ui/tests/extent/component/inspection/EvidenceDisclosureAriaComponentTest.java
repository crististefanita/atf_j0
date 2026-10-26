package atf.ui.tests.extent.component.inspection;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.disclosure.EvidenceDisclosureAriaProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class EvidenceDisclosureAriaComponentTest {
    @Test
    void nativeJavaEvidenceKeepsAriaInSyncForMouseKeyboardAndReselection() {
        EvidenceDisclosureAriaProbe.verify();
    }
}
