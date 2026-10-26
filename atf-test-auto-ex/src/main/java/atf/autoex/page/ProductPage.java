package atf.autoex.page;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.ui.AutoExUiActions;
import atf.autoex.support.validation.AutoExUiText;

/** Product detail and add-to-cart actions. */
public final class ProductPage extends AutoExPage {
    public ProductPage(AutoExUiActions ui) { super(ui); }

    public String details() {
        return text(AutoExSelectors.PRODUCT_INFORMATION,
                "product name, price and availability are read");
    }

    public String addConfirmation() {
        ui.waitForText(AutoExSelectors.CART_ADD_CONFIRMATION, AutoExUiText.CART_ADDED.text(),
                "the successful add-to-cart confirmation is visible");
        return text(AutoExSelectors.CART_ADD_CONFIRMATION, "cart confirms that the selected product was added");
    }

    public void addQuantityToCart(int quantity) {
        fill(AutoExSelectors.QUANTITY, Integer.toString(quantity), "requested quantity is entered");
        click(AutoExSelectors.ADD_TO_CART, "product is added to the cart");
    }

    public CartPage viewCartFromConfirmation() {
        clickLast(AutoExSelectors.VIEW_CART, "cart opens from the added-product confirmation");
        return new CartPage(ui);
    }
}
