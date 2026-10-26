package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.tests.extension.AutoExNetworkDocuments;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.cart.AutoExCartJourney;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Same cart business stories as UI-only, with first-party browser exchanges attached. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI_API)
@AutoExNetworkDocuments({"/"})
@DisplayName("Automation Exercise cart and customer session — UI + API")
public class AutoExCartUiApiE2eTest {
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
