package atf.shop.pages;

import atf.api.ui.dom.HtmlElement;
import atf.api.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.routes.ShopUiRoutes;
import atf.shop.support.ShopCountryOption;
import atf.shop.support.ShopPageContract;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

import static atf.api.ui.dom.HtmlAttribute.VALUE;

/** Billing address page object with no raw selectors outside the locator owner. */
@ShopPageContract(screen = ShopScreen.BILLING, route = ShopUiRoutes.CHECKOUT,
        requiredControls = {ShopControl.COUNTRY, ShopControl.POSTAL_CODE,
                ShopControl.HOUSE_NUMBER, ShopControl.STREET, ShopControl.CITY,
                ShopControl.STATE, ShopControl.BILLING_PROCEED})
public final class ShopBillingPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopBillingPage(Page page, ShopLocators locators,
                                   PlaywrightActionSequence actions) {
        this.page = page;
        this.locators = locators;
        this.actions = actions;
    }

    public void fill(ShopBillingAddress address) {
        actions.waitForVisible(locators.country(page));
        actions.selectOption(locators.country(page), countrySelectionLabel(address.country()),
                "billing country is selected");
        actions.fill(locators.postalCode(page), address.postalCode(), "billing postal code is entered");
        actions.fill(locators.houseNumber(page), address.houseNumber(), "billing house number is entered");
        actions.fill(locators.street(page), address.street(), "billing street is entered");
        actions.fill(locators.city(page), address.city(), "billing city is entered");
        actions.fill(locators.state(page), address.state(), "billing state is entered");
    }

    /**
     * Follows the public checkout's own address lookup for UI-only journeys.
     * The shop, not the test, owns street/city/state after country, postal code
     * and house number are entered.
     */
    public ObservedAddress fillFromPostalLookup(ShopBillingAddress address) {
        actions.waitForVisible(locators.country(page));
        actions.selectOption(locators.country(page), countrySelectionLabel(address.country()),
                "billing country is selected");
        actions.fill(locators.postalCode(page), address.postalCode(), "billing postal code is entered");
        actions.fill(locators.houseNumber(page), address.houseNumber(), "billing house number is entered");
        page.waitForFunction("""
                selectors => selectors.every(selector => {
                    const field = document.querySelector(selector);
                    return field && field.value.trim().length > 0;
                })
                """, List.of(locators.street(page).selector(), locators.city(page).selector(),
                locators.state(page).selector()));

        ObservedAddress observed = new ObservedAddress(
                locators.street(page).locator().inputValue(),
                locators.city(page).locator().inputValue(),
                locators.state(page).locator().inputValue());
        actions.observation("Shop address lookup", "billing address form",
                "after country, postal code and house number, the shop fills street, city and state; "
                        + "do not type over those fields",
                "street=" + observed.street() + "; city=" + observed.city() + "; state=" + observed.state());
        return observed;
    }

    public void proceedToPayment() {
        actions.click(locators.billingProceed(page), "billing address proceeds to payment");
    }

    private String countrySelectionLabel(String configuredCountry) {
        Locator options = locators.country(page).locator().locator(HtmlElement.OPTION.selector());
        ShopCountryOption match = null;
        for (int index = 0; index < options.count(); index++) {
            Locator option = options.nth(index);
            var candidate = new ShopCountryOption(option.getAttribute(VALUE.attributeName()),
                    option.getAttribute("label"), option.innerText());
            if (!candidate.matches(configuredCountry)) continue;
            if (match != null) {
                throw new IllegalStateException("Configured country matches multiple checkout options: "
                        + configuredCountry);
            }
            match = candidate;
        }
        if (match == null) {
            throw new IllegalArgumentException("Configured country does not match a checkout option by name or code: "
                    + configuredCountry);
        }
        String label = match.selectionLabel();
        if (label.isBlank()) {
            throw new IllegalStateException("Matched checkout country has no selectable label: "
                    + configuredCountry);
        }
        return label;
    }

    public record ObservedAddress(String street, String city, String state) {
        public ObservedAddress {
            street = requiredObserved(street, "street");
            city = requiredObserved(city, "city");
            state = requiredObserved(state, "state");
        }

        private static String requiredObserved(String value, String field) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) {
                throw new IllegalStateException("Practice Shop postcode lookup left " + field + " blank");
            }
            return normalized;
        }
    }
}
