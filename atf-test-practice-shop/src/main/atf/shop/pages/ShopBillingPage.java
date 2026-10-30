package atf.shop.pages;

import atf.ui.dom.HtmlElement;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.routes.ShopUiRoutes;
import atf.shop.support.ShopCountryOption;
import atf.shop.support.ShopPageContract;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import atf.shop.support.billing.ShopPostalLookup;
import java.net.URI;

import static atf.ui.dom.HtmlAttribute.VALUE;

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

    public void fill(ShopBillingAddress address, URI lookupEndpoint) {
        actions.waitForVisible(locators.country(page));
        ShopCountryOption country=countrySelection(address.country());
        try(var lookup=new ShopPostalLookup(page,lookupEndpoint,country.value(),address.postalCode(),address.houseNumber())) {
            applyLookupInputs(address,country,lookup);
            actions.awaitVisible(locators.country(page),budget->lookup.settleInputs(()->
                    !locators.postcodeLookupLoading(page).locator().isVisible()
                            && !locators.postcodeLookupError(page).locator().isVisible(),budget));
            actions.information("Postcode lookup settled","billing address form",
                    "configured fields are entered once after pending application autofill has settled",lookup.summary());
            actions.fill(locators.street(page),address.street(),"billing street is entered");
            actions.fill(locators.city(page),address.city(),"billing city is entered");
            actions.fill(locators.state(page),address.state(),"billing state is entered");
        }
    }

    /**
     * Follows the public checkout's own address lookup for UI-only journeys.
     * The shop, not the test, owns street/city/state after country, postal code
     * and house number are entered.
     */
    public ObservedAddress fillFromPostalLookup(ShopBillingAddress address, URI lookupEndpoint) {
        actions.waitForVisible(locators.country(page));
        ShopCountryOption country = countrySelection(address.country());
        try (var lookup = new ShopPostalLookup(page, lookupEndpoint, country.value(), address.postalCode(), address.houseNumber())) {
            applyLookupInputs(address,country,lookup);
            var confirmed = new java.util.concurrent.atomic.AtomicReference<ShopPostalLookup.Address>();
            actions.awaitVisible(locators.country(page), budget -> confirmed.set(lookup.await(() -> observedLookupAddress(budget), budget)));
            var value = confirmed.get();
            ObservedAddress observed = new ObservedAddress(value.street(), value.city(), value.state());
            actions.observation("Shop address lookup", "billing address form",
                    "the current input-owned lookup completed and its street, city and state match the UI; "
                            + "do not type over those fields",
                    "street=" + observed.street() + "; city=" + observed.city() + "; state=" + observed.state());
            return observed;
        }
    }

    private void applyLookupInputs(ShopBillingAddress address,ShopCountryOption country,ShopPostalLookup lookup) {
        actions.selectOption(locators.country(page),country.selectionLabel(),"billing country is selected");
        actions.fill(locators.postalCode(page),address.postalCode(),"billing postal code is entered");
        actions.fill(locators.houseNumber(page),address.houseNumber(),"billing house number is entered");
        lookup.inputsApplied();
    }
    private ShopPostalLookup.Address observedLookupAddress(PlaywrightActionSequence.ObservationBudget budget) {
        if(locators.postcodeLookupLoading(page).locator().isVisible()) return null;
        if(locators.postcodeLookupError(page).locator().isVisible()) throw new IllegalStateException("Application rejected postcode lookup");
        return new ShopPostalLookup.Address(
                locators.street(page).locator().inputValue(new Locator.InputValueOptions().setTimeout(budget.remainingMilliseconds())).trim(),
                locators.city(page).locator().inputValue(new Locator.InputValueOptions().setTimeout(budget.remainingMilliseconds())).trim(),
                locators.state(page).locator().inputValue(new Locator.InputValueOptions().setTimeout(budget.remainingMilliseconds())).trim());
    }

    public void proceedToPayment() {
        actions.click(locators.billingProceed(page), "billing address proceeds to payment");
    }

    private ShopCountryOption countrySelection(String configuredCountry) {
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
        return match;
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
