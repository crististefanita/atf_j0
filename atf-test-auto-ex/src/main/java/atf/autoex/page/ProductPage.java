package atf.autoex.page;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.ui.AutoExUiActions;
import atf.autoex.support.validation.AutoExUiText;
import com.microsoft.playwright.TimeoutError;

import java.util.Optional;

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

    /** Waits after a successful transport response but keeps a UI timeout reportable at the checkpoint. */
    public Optional<String> addConfirmationAfterResponse() {
        try {
            return Optional.of(addConfirmation());
        } catch (RuntimeException timeout) {
            if (isPlaywrightTimeout(timeout)) return Optional.empty();
            throw timeout;
        }
    }

    /** Reads the modal without waiting when transport already failed or produced no settled response. */
    public Optional<String> addConfirmationIfVisible() {
        if (!visible(AutoExSelectors.CART_ADD_CONFIRMATION)) return Optional.empty();
        return Optional.of(text(AutoExSelectors.CART_ADD_CONFIRMATION,
                "cart confirmation state is read after the observed mutation"));
    }

    public void addQuantityToCart(int quantity) {
        fill(AutoExSelectors.QUANTITY, Integer.toString(quantity), "requested quantity is entered");
        click(AutoExSelectors.ADD_TO_CART, "product is added to the cart");
    }

    private static boolean isPlaywrightTimeout(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof TimeoutError) return true;
        }
        return false;
    }

    public CartPage viewCartFromConfirmation() {
        clickLast(AutoExSelectors.VIEW_CART, "cart opens from the added-product confirmation");
        return new CartPage(ui);
    }
}
