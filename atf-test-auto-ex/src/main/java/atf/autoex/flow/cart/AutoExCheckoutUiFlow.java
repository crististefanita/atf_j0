package atf.autoex.flow.cart;

import atf.autoex.business.checkout.AutoExCheckoutFlow;
import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.page.AccountPage;
import atf.autoex.page.CartPage;
import atf.autoex.support.checkout.AutoExOrderLine;
import java.util.Objects;

/** Page adapters observe the current customer/cart; the business gate owns permission to continue. */
public final class AutoExCheckoutUiFlow implements AutoExCheckoutFlow.Port {
    private final CartPage cart;
    private final AccountPage account;
    private final Catalog product;
    public AutoExCheckoutUiFlow(CartPage cart, AccountPage account, Catalog product) {
        this.cart = Objects.requireNonNull(cart); this.account = Objects.requireNonNull(account); this.product = Objects.requireNonNull(product);
    }
    @Override public String customerName() { return account.awaitLoggedInName(); }
    @Override public AutoExOrderLine observe() { return cart.orderLine(product); }
    @Override public void proceedToCheckout() { cart.proceedToCheckout(); }
}
