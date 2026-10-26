package atf.shop.tests.e2e;

import atf.api.testkit.AtfE2eTest;
import atf.api.testkit.channel.AtfTestChannel;
import atf.api.testkit.channel.TestChannel;
import atf.shop.flow.ShopPurchaseFlow;
import atf.shop.tests.support.ShopE2eTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** One complete, guest-safe purchase proof; no account secret is required. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI_API)
@ShopE2eTestSupport.WithE2eHarness
@Execution(ExecutionMode.SAME_THREAD)
@Order(3)
@DisplayName("Guest purchase flow with UI and API evidence")
public class ShopPurchaseE2eTest {
    @Test
    @DisplayName("Configured guest can complete a demo purchase")
    void configuredGuestCanCompletePurchase(ShopE2eTestSupport.E2eHarness harness) {
        harness.run("Purchase flow",
                "the configured guest adds a product, completes billing and confirms the order", journey -> {
                    new ShopPurchaseFlow(journey.locators()).purchaseConfiguredProduct(
                            journey.page(), journey.actions(), journey.configuration(),
                            journey::checkpoint, journey.apiNetwork());
                    journey.completePurchase();
                });
    }
}
