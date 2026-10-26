package atf.shop.pages;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;

/** Payment page object; the demo payment method is configuration, not a test literal. */
@ShopPageContract(screen = ShopScreen.PAYMENT, route = ShopUiRoutes.CHECKOUT,
        requiredControls = {ShopControl.PAYMENT_METHOD, ShopControl.PAYMENT_CHECK})
public final class ShopPaymentPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopPaymentPage(Page page, ShopLocators locators,
                                   PlaywrightActionSequence actions) {
        this.page = page;
        this.locators = locators;
        this.actions = actions;
    }

    public String chooseMethod(String paymentMethod) {
        actions.waitForVisible(locators.paymentMethod(page));
        actions.selectOption(locators.paymentMethod(page), paymentMethod,
                "configured demo payment method is selected");
        return actions.getText(locators.selectedPaymentMethod(page),
                "selected payment method is observed");
    }

    public String checkPayment() {
        actions.click(locators.paymentCheck(page), "configured payment method is checked");
        actions.waitForText(page, locators.paymentSuccess(page), "Payment was successful",
                "payment success confirmation is displayed");
        return actions.expectText(locators.paymentSuccess(page), "Payment was successful");
    }
}
