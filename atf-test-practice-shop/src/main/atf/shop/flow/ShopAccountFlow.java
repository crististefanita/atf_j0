package atf.shop.flow;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.model.ShopCredentials;
import atf.shop.locators.ShopLocators;
import atf.shop.pages.account.ShopAccountPage;
import atf.shop.pages.account.ShopLoginPage;
import com.microsoft.playwright.Page;

/** Reusable authentication flow shared by each external E2E scenario. */
public final class ShopAccountFlow {
    private final ShopLocators locators;

    public ShopAccountFlow(ShopLocators locators) {
        this.locators = locators;
    }

    public void loginAndAssertAccount(Page page, PlaywrightActionSequence actions,
                                      ShopConfiguration configuration,
                                      ShopCredentials credentials) {
        ShopLoginPage login = new ShopLoginPage(page, locators, actions);
        login.open(configuration);
        login.login(credentials);
        new ShopAccountPage(page, locators, actions).assertLoaded(credentials);
    }
}


