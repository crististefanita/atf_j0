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

/** Same purchase journey, augmented with redacted API exchanges from its browser session. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI_API)
@UiReportSubdirectory("auto-ex")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExE2eExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
@Order(2)
@DisplayName("Automation Exercise purchase — UI + API")
public class AutoExPurchaseUiApiE2eTest {
    @Test
    @DisplayName("customer completes the same demo purchase with UI and API evidence")
    void completesPurchaseUsingUiAndApiEvidence(AutoExE2eRun run) {
        AutoExPurchaseJourney.execute(run);
    }
}
