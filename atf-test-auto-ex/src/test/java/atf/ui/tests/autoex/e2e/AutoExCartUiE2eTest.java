package atf.ui.tests.autoex.e2e;

import atf.api.console.junit.AtfConsoleTestExtension;
import atf.api.testkit.AtfConsoleMessages;
import atf.api.testkit.AtfE2eTest;
import atf.api.testkit.channel.AtfTestChannel;
import atf.api.testkit.channel.TestChannel;
import atf.api.testkit.output.UiReportSubdirectory;
import atf.api.testkit.reporting.AtfReportPath;
import atf.ui.autoex.runtime.AutoExE2eRun;
import atf.ui.autoex.support.cart.AutoExCartJourney;
import atf.ui.tests.autoex.extension.AutoExE2eExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Two independent business stories on the public store, each with an isolated browser session. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@UiReportSubdirectory("auto-ex")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExE2eExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Automation Exercise cart and customer session — UI")
public class AutoExCartUiE2eTest {
    @Test
    @DisplayName("adding the same product recalculates the cart and removal empties it")
    void recalculatesAndRemovesCartProduct(AutoExE2eRun run) {
        AutoExCartJourney.recalculatesAndRemovesProduct(run);
    }

    @Test
    @DisplayName("signing in preserves the guest cart and the disposable account is removed")
    void preservesGuestCartAfterLoginAndRemovesAccount(AutoExE2eRun run) {
        AutoExCartJourney.preservesGuestCartAfterLogin(run);
    }
}
