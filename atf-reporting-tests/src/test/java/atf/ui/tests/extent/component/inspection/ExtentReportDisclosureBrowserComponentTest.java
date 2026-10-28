package atf.ui.tests.extent.component.inspection;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.disclosure.ExtentReportDisclosureProbe;
import org.junit.jupiter.api.Test;

/** Browser component intents; report fixtures and browser mechanics belong to support. */
@AtfComponentTest
class ExtentReportDisclosureBrowserComponentTest {
    @Test
    void duplicateScreenshotOpensItsCanonicalImageFromACollapsedScenario() {
        ExtentReportDisclosureProbe.verifyReferencedScreenshot();
    }

    @Test
    void customUiValidationTitleIsResolvedByRoleAndActionEvidence() {
        ExtentReportDisclosureProbe.verifyCustomValidationTitleLocator();
    }

    @Test
    void uiCheckpointHasNoSyntheticHttpTableWhileNestedApiEvidenceRemainsAuditable() {
        ExtentReportDisclosureProbe.verifyUiCheckpointEvidence();
    }

    @Test
    void emptyTestBodyDoesNotAdvertiseAnExpandControl() {
        ExtentReportDisclosureProbe.verifyEmptyPhaseDoesNotAdvertiseExpansion();
    }

    @Test
    void closingRequestHidesItsEvidenceAndValidationForMouseAndKeyboard() {
        ExtentReportDisclosureProbe.verifyRequestSubtreeForMouseAndKeyboard();
    }
}
