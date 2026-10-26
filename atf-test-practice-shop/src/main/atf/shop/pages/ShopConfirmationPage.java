package atf.shop.pages;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;

/** Final payment confirmation boundary, kept separate from payment input. */
@ShopPageContract(screen = ShopScreen.CONFIRMATION, route = ShopUiRoutes.CHECKOUT,
        requiredControls = {ShopControl.ORDER_CONFIRMATION}, bodyMarker = "Thanks for your order!")
public final class ShopConfirmationPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopConfirmationPage(Page page, ShopLocators locators,
                                        PlaywrightActionSequence actions) {
        this.page = page; this.locators = locators; this.actions = actions;
    }

    public String confirmOrder() {
        actions.click(locators.confirmOrder(page), "demo order is submitted");
        actions.waitForText(page, locators.orderConfirmation(page), "Thanks for your order!",
                "completed order confirmation is displayed");
        return actions.expectText(locators.orderConfirmation(page), "Thanks for your order!");
    }
}
