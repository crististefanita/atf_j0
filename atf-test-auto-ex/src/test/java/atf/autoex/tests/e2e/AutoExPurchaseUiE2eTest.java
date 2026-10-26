package atf.autoex.tests.e2e;

import atf.console.junit.AtfConsoleTestExtension;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
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

/** Complete purchase journey; verdicts and evidence come from the rendered UI. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@UiReportSubdirectory("auto-ex")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExE2eExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
@Order(1)
@DisplayName("Automation Exercise purchase — UI")
public class AutoExPurchaseUiE2eTest {
    @Test
    @DisplayName("customer registers during checkout, completes a demo order and removes the account")
    void completesPurchaseUsingUiEvidence(AutoExE2eRun run) {
        AutoExPurchaseJourney.execute(run);
    }
}
