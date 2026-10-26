package atf.shop.pages;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;
import java.net.URI;

/** Cart page object: product identity and quantity are checked before checkout. */
@ShopPageContract(screen = ShopScreen.CART, route = ShopUiRoutes.CHECKOUT,
        requiredControls = {ShopControl.CART_PRODUCT_TITLE,
                ShopControl.CART_PRODUCT_QUANTITY, ShopControl.CART_PROCEED})
public final class ShopCartPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopCartPage(Page page, ShopLocators locators,
                                PlaywrightActionSequence actions) {
        this.page = page;
        this.locators = locators;
        this.actions = actions;
    }

    public void open(URI cartUrl) {
        actions.navigate(page, cartUrl, "cart page is loaded");
        actions.waitForText(page, locators.pageBody(page), "CART", "cart step is displayed");
    }

    public CartLine assertConfiguredLine(String expectedProductName, int expectedQuantity) {
        String productName = actions.expectText(locators.cartProductTitle(page), expectedProductName);
        String quantity = actions.expectValue(locators.cartProductQuantity(page),
                Integer.toString(expectedQuantity));
        return new CartLine(productName, quantity);
    }

    public void proceed() {
        actions.click(locators.cartProceed(page), "cart proceeds to checkout identity");
    }

    public record CartLine(String productName, String quantity) {
    }
}
