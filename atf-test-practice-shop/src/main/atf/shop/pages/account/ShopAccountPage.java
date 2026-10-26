package atf.shop.pages.account;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.model.ShopCredentials;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;

/** Account page object with semantic identity validation. */
@ShopPageContract(screen = ShopScreen.ACCOUNT, route = ShopUiRoutes.ACCOUNT,
        requiredControls = {ShopControl.ACCOUNT_MARKER}, bodyMarker = "My account")
public final class ShopAccountPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopAccountPage(Page page, ShopLocators locators,
                                   PlaywrightActionSequence actions) {
        this.page = page; this.locators = locators; this.actions = actions;
    }

    public void assertLoaded(ShopCredentials credentials) {
        actions.waitForVisible(locators.accountHeading(page));
        actions.waitForText(page, locators.accountHeading(page), "My account",
                "signed-in account heading is displayed");
        actions.expectText(locators.accountHeading(page), "My account");
    }
}


