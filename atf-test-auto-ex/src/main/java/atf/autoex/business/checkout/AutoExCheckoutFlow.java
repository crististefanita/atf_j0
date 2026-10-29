package atf.autoex.business.checkout;

import atf.autoex.business.cart.AutoExCartPort;
import atf.autoex.business.cart.AutoExCartResult;
import atf.autoex.support.checkout.AutoExOrderLine;
import java.util.Objects;
import java.util.function.Consumer;

/** A single checkout activation follows the owned identity, current cart and successful publication. */
public final class AutoExCheckoutFlow {
    public interface Port extends AutoExCartPort { String customerName(); }
    private AutoExCheckoutFlow() { }

    public static AutoExCheckoutAccess enter(Port port, String expectedCustomer, AutoExOrderLine expectedCart,
                                             Consumer<AutoExCheckoutAccess> observer) {
        Objects.requireNonNull(port, "port"); Objects.requireNonNull(observer, "observer");
        if (expectedCustomer == null || expectedCustomer.isBlank()) throw new IllegalArgumentException("owned customer is required");
        Objects.requireNonNull(expectedCart, "expectedCart");
        var result = new AutoExCheckoutAccess(expectedCustomer, port.customerName(), AutoExCartResult.compare(expectedCart, port.observe()));
        if (!result.passed()) {
            var failure = new AutoExCheckoutValidationException(result);
            try { observer.accept(result); }
            catch (RuntimeException | Error publication) { if (publication != failure) failure.addSuppressed(publication); }
            throw failure;
        }
        observer.accept(result);
        port.proceedToCheckout();
        return result;
    }
}
