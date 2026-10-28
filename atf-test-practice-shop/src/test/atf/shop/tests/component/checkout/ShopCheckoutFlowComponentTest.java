package atf.shop.tests.component.checkout;

import atf.reporting.contract.AtfAssertion;
import atf.shop.flow.ShopCheckoutFlow;
import atf.shop.flow.ShopPurchaseFlow;
import atf.shop.pages.ShopCartPage;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopCheckoutFlowComponentTest {
    private static final ShopCartPage.CartLine CART_LINE =
            new ShopCartPage.CartLine("Combination Pliers", "2");
    private static final String PAYMENT_METHOD = "Wire Transfer";
    private static final String ORDER_CONFIRMATION = "Thanks for your order! Your order number is INV-42";

    @Test
    void publishesEachPassingScreenBeforeItsTransitionAndConfirmsOrderAfterPayment() {
        var events = new ArrayList<String>();
        var port = new CheckoutPort(events);
        List<String> expectedTitles = List.of(
                ShopPurchaseTraceStep.CART.title(),
                ShopPurchaseTraceStep.GUEST_CHECKOUT.title(),
                ShopPurchaseTraceStep.BILLING.title(),
                ShopPurchaseTraceStep.PAYMENT_METHOD.title(),
                ShopPurchaseTraceStep.PAYMENT_CHECK.title(),
                ShopPurchaseTraceStep.ORDER_CONFIRMATION.title());

        ShopPurchaseFlow.PurchaseResult result = new ShopCheckoutFlow().complete(
                port, checkpoint -> events.add("publish " + checkpoint.title()), false);

        assertEquals(new ShopPurchaseFlow.PurchaseResult(
                CART_LINE.productName(), CART_LINE.quantity(), PAYMENT_METHOD, ORDER_CONFIRMATION), result);
        assertEquals(List.of(
                "observe cart", "publish " + expectedTitles.get(0), "proceed from cart",
                "observe guest identity", "publish " + expectedTitles.get(1), "proceed to billing as guest",
                "observe billing", "publish " + expectedTitles.get(2), "proceed to payment",
                "observe payment method", "publish " + expectedTitles.get(3),
                "check payment", "publish " + expectedTitles.get(4),
                "confirm order", "publish " + expectedTitles.get(5)), events);
        assertTrue(expectedTitles.indexOf(ShopPurchaseTraceStep.PAYMENT_CHECK.title())
                < expectedTitles.indexOf(ShopPurchaseTraceStep.ORDER_CONFIRMATION.title()));
        assertTrue(result.confirmation().contains("INV-42"));
    }

    @Test
    void failedPaymentGateIsPublishedAndPreventsOrderConfirmation() {
        var events = new ArrayList<String>();
        var port = new CheckoutPort(events);
        port.failedObservation = ShopPurchaseTraceStep.PAYMENT_CHECK;

        var failure = assertThrows(IllegalStateException.class,
                () -> new ShopCheckoutFlow().complete(port,
                        checkpoint -> events.add("publish " + checkpoint.title()), false));

        assertEquals("Page validation failed: " + ShopPurchaseTraceStep.PAYMENT_CHECK.title(),
                failure.getMessage());
        assertTrue(events.contains("publish " + ShopPurchaseTraceStep.PAYMENT_CHECK.title()));
        assertFalse(events.contains("confirm order"));
    }

    @Test
    void authenticatedCheckoutUsesItsOwnObservationAndTransition() {
        var events = new ArrayList<String>();

        new ShopCheckoutFlow().complete(new CheckoutPort(events),
                checkpoint -> events.add("publish " + checkpoint.title()), true);

        assertTrue(events.contains("observe authenticated identity"));
        assertTrue(events.contains("publish " + ShopPurchaseTraceStep.AUTHENTICATED_CHECKOUT.title()));
        assertTrue(events.contains("proceed to billing as authenticated"));
        assertFalse(events.contains("observe guest identity"));
        assertFalse(events.contains("proceed to billing as guest"));
    }

    @Test
    void publicationFailureStopsBeforeTheFollowingBrowserTransition() {
        var events = new ArrayList<String>();
        var port = new CheckoutPort(events);
        var reportingFailure = new IllegalStateException("report sink failed");

        var thrown = assertThrows(IllegalStateException.class,
                () -> new ShopCheckoutFlow().complete(port, checkpoint -> {
                    events.add("publish " + checkpoint.title());
                    throw reportingFailure;
                }, true));

        assertEquals(reportingFailure, thrown);
        assertEquals(List.of("observe cart", "publish " + ShopPurchaseTraceStep.CART.title()), events);
    }

    private static ShopPurchaseCheckpoint checkpoint(ShopPurchaseTraceStep step, boolean passed) {
        return new ShopPurchaseCheckpoint(step.keyword(), step.title(),
                List.of(new AtfAssertion("screen", "ready", passed ? "ready" : "invalid", passed)));
    }

    private static final class CheckoutPort implements ShopCheckoutFlow.Port {
        private final List<String> events;
        private ShopPurchaseTraceStep failedObservation;

        private CheckoutPort(List<String> events) {
            this.events = events;
        }

        @Override public ShopCheckoutFlow.Observation<ShopCartPage.CartLine> observeCart() {
            events.add("observe cart");
            return observation(CART_LINE, ShopPurchaseTraceStep.CART);
        }
        @Override public void proceedFromCart() { events.add("proceed from cart"); }
        @Override public ShopCheckoutFlow.Observation<Void> observeIdentity(boolean authenticated) {
            events.add(authenticated ? "observe authenticated identity" : "observe guest identity");
            return observation(null, authenticated
                    ? ShopPurchaseTraceStep.AUTHENTICATED_CHECKOUT : ShopPurchaseTraceStep.GUEST_CHECKOUT);
        }
        @Override public void proceedToBilling(boolean authenticated) {
            events.add(authenticated ? "proceed to billing as authenticated" : "proceed to billing as guest");
        }
        @Override public ShopCheckoutFlow.Observation<Void> observeBilling() {
            events.add("observe billing");
            return observation(null, ShopPurchaseTraceStep.BILLING);
        }
        @Override public void proceedToPayment() { events.add("proceed to payment"); }
        @Override public ShopCheckoutFlow.Observation<String> observePaymentMethod() {
            events.add("observe payment method");
            return observation(PAYMENT_METHOD, ShopPurchaseTraceStep.PAYMENT_METHOD);
        }
        @Override public ShopCheckoutFlow.Observation<Void> checkPayment() {
            events.add("check payment");
            return observation(null, ShopPurchaseTraceStep.PAYMENT_CHECK);
        }
        @Override public ShopCheckoutFlow.Observation<String> confirmOrder() {
            events.add("confirm order");
            return observation(ORDER_CONFIRMATION, ShopPurchaseTraceStep.ORDER_CONFIRMATION);
        }

        private <T> ShopCheckoutFlow.Observation<T> observation(T value, ShopPurchaseTraceStep step) {
            boolean passed = step != failedObservation;
            return new ShopCheckoutFlow.Observation<>(value, checkpoint(step, passed));
        }
    }
}
