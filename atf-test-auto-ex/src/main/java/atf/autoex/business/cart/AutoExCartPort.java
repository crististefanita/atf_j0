package atf.autoex.business.cart;

import atf.autoex.support.checkout.AutoExOrderLine;

/** Observed cart and the guarded business transition, independent of browser and evidence. */
public interface AutoExCartPort {
    AutoExOrderLine observe();
    void proceedToCheckout();
}
