package atf.shop.tests.e2e;

import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.shop.flow.ShopPurchaseFlow;
import atf.shop.tests.support.ShopE2eTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/** One consolidated UI-only business proposition, action table and final screenshot. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@AtfUiEvidence(flow = AtfUiEvidenceFlow.UNIFIED_FLOW)
@ShopE2eTestSupport.WithE2eHarness
@Execution(ExecutionMode.SAME_THREAD)
@Order(5)
@DisplayName("Guest UI-only single-flow purchase")
public class ShopUiOnlySingleFlowPurchaseE2eTest {
    @Test
    @DisplayName("Configured guest completes one auditable UI purchase flow")
    void configuredGuestCompletesOneUiOnlyFlow(
            ShopE2eTestSupport.E2eHarness harness) {
        harness.run("UI-only purchase — one flow",
                "the configured guest completes checkout using browser actions only", journey -> {
                    var result = new ShopPurchaseFlow(journey.locators())
                            .purchaseConfiguredProduct(journey.page(), journey.actions(),
                                    journey.configuration(), journey::checkpoint);
                    journey.completePurchase(result);
                });
    }
}
