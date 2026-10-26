package atf.autoex.flow.cart;

import atf.autoex.business.cart.AutoExCartPort;
import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.page.CartPage;
import atf.autoex.support.checkout.AutoExOrderLine;

import java.util.Objects;

/** UI implementation of the cart port; the page owns selectors and instrumented operations. */
public final class AutoExCartUiFlow implements AutoExCartPort {
    private final CartPage page;
    private final Catalog product;

    public AutoExCartUiFlow(CartPage page, Catalog product) {
        this.page = Objects.requireNonNull(page, "page");
        this.product = Objects.requireNonNull(product, "product");
    }

    @Override public AutoExOrderLine observe() { return page.orderLine(product); }
    @Override public void proceedToCheckout() { page.proceedToCheckout(); }
}
