package atf.shop.pages.account;

import atf.core.http.request.HttpMethod;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.model.ShopRegistrationData;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import java.net.URI;
import java.util.List;

/** Registration page object; account creation is explicit and never implicit in a purchase test. */
@ShopPageContract(screen = ShopScreen.REGISTRATION, route = ShopUiRoutes.AUTH_REGISTER,
        requiredControls = {ShopControl.REGISTER_FIRST_NAME, ShopControl.REGISTER_LAST_NAME,
                ShopControl.REGISTER_DOB, ShopControl.REGISTER_COUNTRY,
                ShopControl.REGISTER_POSTAL_CODE, ShopControl.REGISTER_HOUSE_NUMBER,
                ShopControl.REGISTER_STREET, ShopControl.REGISTER_CITY,
                ShopControl.REGISTER_STATE, ShopControl.REGISTER_PHONE,
                ShopControl.REGISTER_EMAIL, ShopControl.REGISTER_PASSWORD,
                ShopControl.REGISTER_SUBMIT}, bodyMarker = "Customer registration")
public final class ShopRegistrationPage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopRegistrationPage(Page page, ShopLocators locators,
                                        PlaywrightActionSequence actions) {
        this.page = page;
        this.locators = locators;
        this.actions = actions;
    }

    public void open(URI registrationUrl) {
        actions.navigate(page, registrationUrl, "registration page is loaded");
        actions.waitForText(page, locators.pageBody(page), "Customer registration",
                "registration form is displayed");
    }

    public void fill(ShopRegistrationData data) {
        actions.fill(locators.registerFirstName(page), data.firstName(), "registration first name is entered");
        actions.fill(locators.registerLastName(page), data.lastName(), "registration last name is entered");
        actions.fill(locators.registerDob(page), data.dateOfBirth(), "registration date of birth is entered");
        actions.waitForVisible(locators.registerCountry(page));
        actions.selectOption(locators.registerCountry(page), data.country(), "registration country is selected");
        actions.fill(locators.registerPostalCode(page), data.postalCode(), "registration postal code is entered");
        actions.fill(locators.registerHouseNumber(page), data.houseNumber(), "registration house number is entered");
        page.waitForFunction("""
                selectors => selectors.every(selector => {
                    const field = document.querySelector(selector);
                    return field && field.value.trim().length > 0;
                })
                """, List.of(locators.registerStreet(page).selector(),
                locators.registerCity(page).selector(), locators.registerState(page).selector()));
        actions.observation("Address lookup", "registration address fields",
                "the shop fills street, city and state after country, postal code and house number are entered",
                "street, city and state were populated before configured values were applied");
        actions.fill(locators.registerStreet(page), data.street(), "registration street is entered");
        actions.fill(locators.registerCity(page), data.city(), "registration city is entered");
        actions.fill(locators.registerState(page), data.state(), "registration state is entered");
        actions.fill(locators.registerPhone(page), data.phone(), "registration phone is entered");
        actions.fill(locators.registerEmail(page), data.email(), "registration email is entered");
        actions.fill(locators.registerPassword(page), data.password(), "registration password is entered securely");
    }

    public void submit() {
        actions.click(locators.registerSubmit(page), "registration form is submitted");
    }

    /**
     * Submits once and waits for the API response from the configured host.
     * There is deliberately no retry: a timeout leaves the external outcome
     * uncertain and the caller must stop rather than replay account creation.
     */
    public int submitAndAwaitApiResponse(URI apiEndpoint) {
        URI endpoint = java.util.Objects.requireNonNull(apiEndpoint, "apiEndpoint");
        var response = page.waitForResponse(candidate -> isApiPost(candidate, endpoint),
                () -> actions.click(locators.registerSubmit(page), "registration form is submitted once"));
        int status = response.status();
        if (status < 200 || status >= 300) {
            throw new AssertionError("Practice Shop registration response was not successful; HTTP " + status);
        }
        // The registration response can arrive before the browser finishes the
        // form navigation. Do not let setup evidence capture a transient
        // document whose execution context is about to be replaced.
        page.waitForLoadState(LoadState.LOAD);
        return status;
    }

    static boolean isApiPost(com.microsoft.playwright.Response response, URI apiEndpoint) {
        if (response == null || response.request() == null || apiEndpoint == null) return false;
        if (!HttpMethod.POST.value().equalsIgnoreCase(response.request().method())) return false;
        try {
            return apiEndpoint.getHost().equalsIgnoreCase(URI.create(response.url()).getHost());
        } catch (RuntimeException malformedUrl) {
            return false;
        }
    }
}
