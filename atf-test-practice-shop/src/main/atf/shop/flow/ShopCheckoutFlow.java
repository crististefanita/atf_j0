package atf.shop.flow;

import atf.shop.pages.ShopCartPage;
import atf.shop.support.ShopPurchaseCheckpoint;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Coordinates checkout evidence gates and transitions without depending on Playwright.
 * Each port method represents a screen-level observation or business transition,
 * not an individual browser action.
 */
public final class ShopCheckoutFlow {
    public ShopPurchaseFlow.PurchaseResult complete(Port port,
                                                    Consumer<ShopPurchaseCheckpoint> publisher,
                                                    boolean authenticated) {
        Objects.requireNonNull(port, "port");
        Objects.requireNonNull(publisher, "publisher");

        ShopCartPage.CartLine cart = pass(port.observeCart(), publisher);
        port.proceedFromCart();

        pass(port.observeIdentity(authenticated), publisher);
        port.proceedToBilling(authenticated);

        pass(port.observeBilling(), publisher);
        port.proceedToPayment();

        String paymentMethod = pass(port.observePaymentMethod(), publisher);
        pass(port.checkPayment(), publisher);

        String confirmation = pass(port.confirmOrder(), publisher);
        return new ShopPurchaseFlow.PurchaseResult(
                cart.productName(), cart.quantity(), paymentMethod, confirmation);
    }

    private static <T> T pass(Observation<T> observation,
                              Consumer<ShopPurchaseCheckpoint> publisher) {
        Objects.requireNonNull(observation, "observation");
        ShopPurchaseCheckpoint checkpoint = observation.checkpoint();
        publisher.accept(checkpoint);
        if (!checkpoint.passed()) {
            throw new IllegalStateException("Page validation failed: " + checkpoint.title());
        }
        return observation.value();
    }

    public interface Port {
        Observation<ShopCartPage.CartLine> observeCart();

        void proceedFromCart();

        Observation<Void> observeIdentity(boolean authenticated);

        void proceedToBilling(boolean authenticated);

        Observation<Void> observeBilling();

        void proceedToPayment();

        Observation<String> observePaymentMethod();

        Observation<Void> checkPayment();

        Observation<String> confirmOrder();
    }

    public record Observation<T>(T value, ShopPurchaseCheckpoint checkpoint) {
        public Observation {
            Objects.requireNonNull(checkpoint, "checkpoint");
        }
    }
}
