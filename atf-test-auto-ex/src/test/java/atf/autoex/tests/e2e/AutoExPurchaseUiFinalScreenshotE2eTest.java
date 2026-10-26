package atf.autoex.tests.e2e;

import atf.console.junit.AtfConsoleTestExtension;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.testkit.output.UiReportSubdirectory;
import atf.testkit.reporting.AtfReportPath;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.journey.AutoExPurchaseJourney;
import atf.autoex.tests.extension.AutoExE2eExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Same UI-only journey; success gets one final screenshot and failures retain a failure capture. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@AtfUiEvidence(flow = AtfUiEvidenceFlow.UNIFIED_FLOW,
        screenshots = AtfUiScreenshotPolicy.FINAL_AND_FAILURE)
@UiReportSubdirectory("auto-ex")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExE2eExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
@Order(3)
@DisplayName("Automation Exercise purchase — UI, final screenshot and failure evidence")
public class AutoExPurchaseUiFinalScreenshotE2eTest {
    @Test
    @DisplayName("customer completes the same demo purchase with a single terminal screenshot")
    void completesPurchaseWithOneFinalScreenshot(AutoExE2eRun run) {
        AutoExPurchaseJourney.execute(run);
    }
}
