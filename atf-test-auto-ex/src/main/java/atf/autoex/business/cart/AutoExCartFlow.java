package atf.autoex.business.cart;

import atf.autoex.support.checkout.AutoExOrderLine;

import java.util.Objects;
import java.util.function.Consumer;

/** Checkout cannot start after a failed observation, validation or evidence publication. */
public final class AutoExCartFlow {
    private AutoExCartFlow() { }

    public static AutoExCartResult review(AutoExCartPort cart, AutoExOrderLine expected,
                                         Consumer<AutoExCartResult> observer) {
        Objects.requireNonNull(cart, "cart");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(expected, "expected");
        AutoExCartResult result = AutoExCartResult.compare(expected, cart.observe());
        if (!result.passed()) {
            var failure = new AutoExCartValidationException(result);
            try { observer.accept(result); }
            catch (RuntimeException | AssertionError publicationFailure) { failure.initCause(publicationFailure); }
            throw failure;
        }
        observer.accept(result);
        return result;
    }

    public static AutoExCartResult checkout(AutoExCartPort cart, AutoExOrderLine expected,
                                           Consumer<AutoExCartResult> observer) {
        AutoExCartResult result = review(cart, expected, observer);
        cart.proceedToCheckout();
        return result;
    }
}
