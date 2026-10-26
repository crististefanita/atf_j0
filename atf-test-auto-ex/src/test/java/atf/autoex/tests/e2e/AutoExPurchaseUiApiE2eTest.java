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
