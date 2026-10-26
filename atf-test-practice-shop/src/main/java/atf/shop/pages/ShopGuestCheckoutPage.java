package atf.shop.pages;

import atf.api.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.model.ShopGuest;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;

/** Guest identity page object; entered values are intentionally redacted by the action facade. */
@ShopPageContract(screen = ShopScreen.GUEST_CHECKOUT, route = ShopUiRoutes.CHECKOUT,
        requiredControls = {ShopControl.GUEST_EMAIL, ShopControl.GUEST_FIRST_NAME,
                ShopControl.GUEST_LAST_NAME, ShopControl.GUEST_SUBMIT})
public final class ShopGuestCheckoutPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopGuestCheckoutPage(Page page, ShopLocators locators,
                                         PlaywrightActionSequence actions) {
        this.page = page;
        this.locators = locators;
        this.actions = actions;
    }

    public void continueAsGuest(ShopGuest guest) {
        fillIdentity(guest);
        submitIdentity();
    }

    /** Fills the form without submitting it, allowing value validation at this boundary. */
    public void fillIdentity(ShopGuest guest) {
        actions.click(locators.guestTab(page), "guest checkout is selected");
        actions.fill(locators.guestEmail(page), guest.email(), "guest email is entered securely");
        actions.fill(locators.guestFirstName(page), guest.firstName(), "guest first name is entered");
        actions.fill(locators.guestLastName(page), guest.lastName(), "guest last name is entered");
    }

    /** Submits the already validated identity and waits for the checkout transition. */
    public void submitIdentity() {
        actions.click(locators.guestSubmit(page), "guest checkout identity is submitted");
        actions.waitForText(page, locators.pageBody(page), "Continuing as guest",
                "guest checkout identity is displayed");
    }

    public void proceedToBilling() {
        actions.click(locators.guestProceed(page), "guest checkout proceeds to billing");
    }
}
