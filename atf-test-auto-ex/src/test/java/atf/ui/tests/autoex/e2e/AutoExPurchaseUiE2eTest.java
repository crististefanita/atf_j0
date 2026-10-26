package atf.ui.tests.autoex.e2e;

import atf.api.console.junit.AtfConsoleTestExtension;
import atf.api.testkit.AtfConsoleMessages;
import atf.api.testkit.AtfE2eTest;
import atf.api.testkit.channel.AtfTestChannel;
import atf.api.testkit.channel.TestChannel;
import atf.api.testkit.output.UiReportSubdirectory;
import atf.api.testkit.reporting.AtfReportPath;
import atf.ui.autoex.runtime.AutoExE2eRun;
import atf.ui.autoex.support.journey.AutoExPurchaseJourney;
import atf.ui.tests.autoex.extension.AutoExE2eExtension;
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
