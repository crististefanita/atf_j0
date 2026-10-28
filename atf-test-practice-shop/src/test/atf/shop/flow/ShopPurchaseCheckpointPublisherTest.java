package atf.shop.flow;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.shop.config.ShopScreen;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.validation.ShopPageValidationResult;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ShopComponentTest
class ShopPurchaseCheckpointPublisherTest {
    @Test
    void buildsCheckpointFromScreenValidationAndAdditionalAssertions() {
        var publisher = new ShopPurchaseCheckpointPublisher(ignored -> { });
        var screen = new AtfAssertion("screen", "ready", "ready", true);
        var domain = new AtfAssertion("product", "configured", "configured", true);

        ShopPurchaseCheckpoint checkpoint = publisher.checkpoint(
                AtfGherkinKeyword.GIVEN, "cart is ready",
                new ShopPageValidationResult(ShopScreen.CART, List.of(screen)),
                List.of(domain), List.of());

        assertEquals(AtfGherkinKeyword.GIVEN, checkpoint.keyword());
        assertEquals("cart is ready", checkpoint.title());
        assertEquals(List.of(screen, domain), List.copyOf(checkpoint.assertions()));
        assertTrue(checkpoint.passed());
    }

    @Test
    void catalogPublisherPublishesFailureBeforeClosingItsGate() {
        var published = new ArrayList<ShopPurchaseCheckpoint>();
        var publisher = new ShopPurchaseCheckpointPublisher(published::add);
        ShopPurchaseCheckpoint failed = failedCart();

        var thrown = assertThrows(IllegalStateException.class, () -> publisher.publish(failed));

        assertEquals("Page validation failed: " + ShopPurchaseTraceStep.CART.title(), thrown.getMessage());
        assertEquals(List.of(failed), published);
        assertSame(failed, published.getFirst());
    }

    @Test
    void coordinatorPublisherDoesNotDuplicateTheCheckoutGate() {
        var published = new ArrayList<ShopPurchaseCheckpoint>();
        var publisher = new ShopPurchaseCheckpointPublisher(published::add);
        ShopPurchaseCheckpoint failed = failedCart();

        publisher.report(failed);

        assertEquals(List.of(failed), published);
        assertFalse(published.getFirst().passed());
    }

    @Test
    void sinkFailureRemainsPrimaryAndIsNotReplacedByTheGate() {
        var reportingFailure = new IllegalStateException("checkpoint sink failed");
        var publisher = new ShopPurchaseCheckpointPublisher(checkpoint -> { throw reportingFailure; });

        assertSame(reportingFailure,
                assertThrows(IllegalStateException.class, () -> publisher.publish(failedCart())));
    }

    private static ShopPurchaseCheckpoint failedCart() {
        return new ShopPurchaseCheckpoint(ShopPurchaseTraceStep.CART.keyword(),
                ShopPurchaseTraceStep.CART.title(),
                List.of(new AtfAssertion("cart", "ready", "missing", false)));
    }
}
