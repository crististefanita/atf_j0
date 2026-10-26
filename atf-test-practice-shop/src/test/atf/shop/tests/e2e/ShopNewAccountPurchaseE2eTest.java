package atf.shop.tests.e2e;

import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.shop.flow.ShopPurchaseFlow;
import atf.shop.tests.support.ShopAuthenticatedAccountTestSupport;
import atf.shop.tests.support.ShopE2eTestSupport;
import atf.shop.tests.support.ShopTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Creates a synthetic account, proves login, then completes one authenticated purchase. */
@ShopTestSupport.AuthenticatedE2e(
        accountSetup = ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
        registrationProfile = ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
@ShopTestSupport.AllowsRegistrationPerRun
@AtfE2eTest
@AtfTestChannel(TestChannel.UI_API)
@ShopE2eTestSupport.WithE2eHarness
@Execution(ExecutionMode.SAME_THREAD)
@Order(1)
@DisplayName("New account authenticated purchase")
public class ShopNewAccountPurchaseE2eTest {
    @Test
    @DisplayName("A newly registered and signed-in customer completes a purchase")
    void newAccountCanCompletePurchase(ShopE2eTestSupport.E2eHarness harness) {
        harness.run("New-account purchase flow",
                "a synthetic account is registered and signed in before the complete purchase journey",
                ShopAuthenticatedAccountTestSupport::prepare, (journey, credentials) -> {
                    var purchase = new ShopPurchaseFlow(journey.locators())
                            .purchaseConfiguredProductAuthenticated(
                            journey.page(), journey.actions(), journey.configuration(),
                            journey::checkpoint, journey.apiNetwork());
                    journey.completePurchase(purchase);
                });
    }
}
