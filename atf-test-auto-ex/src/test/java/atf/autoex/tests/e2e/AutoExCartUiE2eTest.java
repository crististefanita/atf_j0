package atf.autoex.tests.e2e;

import atf.console.junit.AtfConsoleTestExtension;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.output.UiReportSubdirectory;
import atf.testkit.reporting.AtfReportPath;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.cart.AutoExCartJourney;
import atf.autoex.tests.extension.AutoExE2eExtension;
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
