package atf.autoex.page;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.checkout.AutoExOrderLine;
import atf.autoex.ui.AutoExUiActions;
import atf.autoex.support.validation.AutoExUiText;

/** Cart rows and checkout transition. */
public final class CartPage extends AutoExPage {
    public CartPage(AutoExUiActions ui) { super(ui); }

    public void openFromHeader() { click(AutoExSelectors.CART_LINK, "cart page opens"); }

    public AutoExOrderLine orderLine(AutoExConfiguration.Catalog product) {
        return new AutoExOrderLine(productName(product), productPrice(product), quantity(product), total(product));
    }

    public String productName(AutoExConfiguration.Catalog product) {
        return text(row(product) + " " + AutoExSelectors.CART_PRODUCT_NAME, "cart product name is read");
    }

    public String productPrice(AutoExConfiguration.Catalog product) {
        return text(row(product) + " " + AutoExSelectors.CART_PRICE, "cart unit price is read");
    }

    public String quantity(AutoExConfiguration.Catalog product) {
        return text(row(product) + " " + AutoExSelectors.CART_QUANTITY, "cart quantity is read");
    }

    public String total(AutoExConfiguration.Catalog product) {
        return text(row(product) + " " + AutoExSelectors.CART_TOTAL, "cart line total is read");
    }

    public void proceedToCheckout() {
        ui.waitForVisible(AutoExSelectors.CHECKOUT);
        click(AutoExSelectors.CHECKOUT, "checkout begins from the cart");
    }

    public String removeProductAndReadEmptyCart(AutoExConfiguration.Catalog product) {
        click(row(product) + " " + AutoExSelectors.CART_REMOVE, "selected product is removed from the cart");
        ui.waitForText(AutoExSelectors.EMPTY_CART, AutoExUiText.CART_EMPTY.text(),
                "cart confirms that its last product was removed");
        return text(AutoExSelectors.EMPTY_CART, "empty-cart confirmation is read");
    }

    private static String row(AutoExConfiguration.Catalog product) {
        return AutoExSelectors.CART_PRODUCT_ROW_PREFIX + product.productId();
    }
}
