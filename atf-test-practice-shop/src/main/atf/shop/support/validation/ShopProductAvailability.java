package atf.shop.support.validation;

import atf.reporting.contract.AtfAssertion;
import atf.shop.locators.ShopLocators;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Page;

import static atf.reporting.audit.ReportAuditChecks.check;

/** One immutable purchase precondition shared by screen evidence and the final mutation guard. */
public record ShopProductAvailability(boolean outOfStock, boolean quantityEnabled, boolean cartEnabled) {
    public static ShopProductAvailability observe(Page page, ShopLocators locators) {
        return new ShopProductAvailability(locators.productOutOfStock(page).locator().isVisible(),
                enabled(locators.productQuantity(page)), enabled(locators.addToCart(page)));
    }

    public boolean purchasable() { return !outOfStock && quantityEnabled && cartEnabled; }

    public AtfAssertion assertion() {
        return check("Product availability", "in stock; quantity and add-to-cart enabled", observed(), purchasable());
    }

    public void requirePurchasable() {
        if (!purchasable()) throw new IllegalStateException("Selected Practice Shop product cannot be purchased: " + observed());
    }

    private String observed() {
        return (outOfStock ? "out of stock" : "no out-of-stock marker")
                + "; quantity enabled=" + quantityEnabled + "; add-to-cart enabled=" + cartEnabled;
    }

    private static boolean enabled(AtfLocator control) {
        return control.locator().count() == 1 && control.locator().isEnabled();
    }
}
