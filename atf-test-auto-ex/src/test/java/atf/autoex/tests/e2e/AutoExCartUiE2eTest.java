package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.cart.AutoExCartJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Two independent business stories on the public store, each with an isolated browser session. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI)

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
