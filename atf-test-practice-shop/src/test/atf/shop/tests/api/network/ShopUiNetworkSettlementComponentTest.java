package atf.shop.tests.api.network;

import atf.core.http.request.HttpMethod;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopUiNetworkSettlementFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Component proof for bounded consumer-owned UI network settlement. */
@ShopComponentTest
class ShopUiNetworkSettlementComponentTest {
    @Test
    void boundedReadBackMatchesTheShopQuantityAfterTheCursor() throws Exception {
        try (var inspector = ShopUiNetworkSettlementFixture.inspector();
             var scheduler = Executors.newSingleThreadScheduledExecutor()) {
            int beforeMutation = inspector.observationCursor();
            inspector.capture(ShopUiNetworkSettlementFixture.mutation(1, 1));
            scheduler.schedule(() -> inspector.capture(
                    ShopUiNetworkSettlementFixture.readBack(2, 1)),
                    20, TimeUnit.MILLISECONDS);

            var readBack = inspector.awaitAfter(beforeMutation, observation ->
                            ShopUiNetworkSettlementFixture.CART_PATH.equals(observation.path())
                                    && HttpMethod.GET.value().equals(observation.method())
                                    && ShopJsonEvidence.parse(observation.responseBody())
                                            .path("cart_items").path(0).path("quantity").asInt() == 1,
                    Duration.ofMillis(200));

            assertTrue(readBack.isPresent());
            assertEquals(2L, readBack.orElseThrow().sequence(), "The accepted cart read-back was captured after the cursor");
            assertEquals(ShopUiNetworkSettlementFixture.PRODUCT_ID,
                    ShopJsonEvidence.parse(readBack.orElseThrow().responseBody()).path("cart_items").path(0).path("product_id").asText(),
                    "The observed cart contains the fixture's independently configured product");
        }
    }

    @Test
    void expiredReadBackRemainsEmptyWithoutInventingCartState() {
        try (var inspector = ShopUiNetworkSettlementFixture.inspector()) {
            int beforeMutation = inspector.observationCursor();
            inspector.capture(ShopUiNetworkSettlementFixture.mutation(1, 1));

            var readBack = inspector.awaitAfter(beforeMutation, observation ->
                            ShopUiNetworkSettlementFixture.CART_PATH.equals(observation.path())
                                    && HttpMethod.GET.value().equals(observation.method()),
                    Duration.ofMillis(20));

            assertTrue(readBack.isEmpty());
            assertEquals(1, inspector.observations().size(), "The journal contains the supplied POST and no invented read-back");
            assertEquals(HttpMethod.POST.value(), inspector.observations().getFirst().method());
        }
    }

    @Test
    void readBackFromBeforeTheMutationCannotSatisfySettlement() {
        try (var inspector = ShopUiNetworkSettlementFixture.inspector()) {
            inspector.capture(ShopUiNetworkSettlementFixture.readBack(1, 1));
            int beforeMutation = inspector.observationCursor();
            inspector.capture(ShopUiNetworkSettlementFixture.mutation(2, 1));

            var premature = inspector.awaitAfter(beforeMutation,
                    observation -> ShopUiNetworkSettlementFixture.CART_PATH.equals(observation.path())
                            && HttpMethod.GET.value().equals(observation.method()), Duration.ofMillis(20));

            assertTrue(premature.isEmpty(), "a matching pre-action GET is stale evidence");
            inspector.capture(ShopUiNetworkSettlementFixture.readBack(3, 1));
            var current = inspector.awaitAfter(beforeMutation,
                    observation -> ShopUiNetworkSettlementFixture.CART_PATH.equals(observation.path())
                            && HttpMethod.GET.value().equals(observation.method()), Duration.ofMillis(20));
            assertEquals(3, current.orElseThrow().sequence());
        }
    }
}
