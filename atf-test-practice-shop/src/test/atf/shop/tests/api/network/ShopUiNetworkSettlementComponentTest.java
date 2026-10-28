package atf.shop.tests.api.network;

import atf.core.http.request.HttpMethod;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopUiNetworkSettlementFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Component proof for bounded consumer-owned UI network settlement. */
@ShopComponentTest
class ShopUiNetworkSettlementComponentTest {
    @Test
    void consecutiveMutationResponsesCollapseOnlyInThePresentationStream() {
        try (var inspector = ShopUiNetworkSettlementFixture.inspector()) {
            var first = ShopUiNetworkSettlementFixture.mutation(1, 1);
            var retry = ShopUiNetworkSettlementFixture.mutation(2, 1);
            var readBack = ShopUiNetworkSettlementFixture.readBack(3, 1);

            var capturedFirst = inspector.capture(first).orElseThrow();
            inspector.capture(retry);
            var capturedReadBack = inspector.capture(readBack).orElseThrow();

            assertEquals(3, inspector.observations().size(),
                    "raw browser order remains lossless for audit");
            assertEquals(2, inspector.presentationObservations().size(),
                    "only consecutive identical signatures are compacted");
            assertEquals(2, inspector.signatureCounts().get(capturedFirst.requestSignature()));
            assertEquals(capturedReadBack, inspector.presentationObservations().get(1));
        }
    }

    @Test
    void boundedReadBackCanArriveAfterMutationWithoutReplayingTheMutation() throws Exception {
        try (var inspector = ShopUiNetworkSettlementFixture.inspector();
             var scheduler = Executors.newSingleThreadScheduledExecutor()) {
            AtomicInteger mutationAttempts = new AtomicInteger(1);
            int beforeMutation = inspector.observationCursor();
            inspector.capture(ShopUiNetworkSettlementFixture.mutation(1, 1));
            scheduler.schedule(() -> inspector.capture(
                    ShopUiNetworkSettlementFixture.readBack(2, 1)),
                    20, TimeUnit.MILLISECONDS);

            Optional<?> readBack = inspector.awaitAfter(beforeMutation, observation ->
                            ShopUiNetworkSettlementFixture.CART_PATH.equals(observation.path())
                                    && HttpMethod.GET.value().equals(observation.method())
                                    && ShopJsonEvidence.parse(observation.responseBody())
                                            .path("cart_items").path(0).path("quantity").asInt() == 1,
                    Duration.ofMillis(200));

            assertTrue(readBack.isPresent());
            assertEquals(1, mutationAttempts.get(),
                    "settlement waits for read-back and never replays a browser mutation");
        }
    }

    @Test
    void expiredReadBackIsEmptyAndCannotBecomeFalseGreenThroughReplay() {
        try (var inspector = ShopUiNetworkSettlementFixture.inspector()) {
            AtomicInteger mutationAttempts = new AtomicInteger(1);
            int beforeMutation = inspector.observationCursor();
            inspector.capture(ShopUiNetworkSettlementFixture.mutation(1, 1));

            Optional<?> readBack = inspector.awaitAfter(beforeMutation, observation ->
                            ShopUiNetworkSettlementFixture.CART_PATH.equals(observation.path())
                                    && HttpMethod.GET.value().equals(observation.method()),
                    Duration.ofMillis(20));

            assertTrue(readBack.isEmpty());
            assertEquals(1, mutationAttempts.get(),
                    "timeout is observable; no implicit mutation replay is allowed");
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
