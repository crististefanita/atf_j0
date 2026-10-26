package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExPurchaseJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/** Same UI-only journey; success gets one final screenshot and failures retain a failure capture. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI)
@AtfUiEvidence(flow = AtfUiEvidenceFlow.UNIFIED_FLOW,
        screenshots = AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
@Order(3)
@DisplayName("Automation Exercise purchase — UI, final screenshot and failure evidence")
public class AutoExPurchaseUiFinalScreenshotE2eTest {
    @Test
    @DisplayName("customer completes the same demo purchase with a single terminal screenshot")
    void completesPurchaseWithOneFinalScreenshot(AutoExE2eRun run) {
        AutoExPurchaseJourney.execute(run);
    }
}
