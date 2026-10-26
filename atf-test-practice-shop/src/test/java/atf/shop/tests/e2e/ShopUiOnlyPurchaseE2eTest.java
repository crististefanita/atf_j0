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

/** Config-driven UI-only proof of the complete guest purchase journey. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ShopE2eTestSupport.WithE2eHarness
@Execution(ExecutionMode.SAME_THREAD)
@Order(4)
@DisplayName("Guest UI-only purchase flow")
public class ShopUiOnlyPurchaseE2eTest {
    @Test
    @DisplayName("Configured guest completes a purchase using UI evidence only")
    void configuredGuestCanCompletePurchaseWithUiEvidenceOnly(
            ShopE2eTestSupport.E2eHarness harness) {
        harness.run("UI-only purchase flow",
                "the configured guest completes product selection, billing and payment through the UI",
                journey -> {
                    new ShopPurchaseFlow(journey.locators()).purchaseConfiguredProduct(
                            journey.page(), journey.actions(), journey.configuration(), journey::checkpoint);
                    journey.completePurchase();
                });
    }
}
