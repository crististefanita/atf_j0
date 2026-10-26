package atf.shop.support.validation;

import atf.api.reporting.contract.AtfAssertion;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Objects;

/** Component validation slices for the non-home screens of the purchase journey. */
public final class ShopPurchaseScreenValidator {
    private final ShopComponentAssertions components;

    public ShopPurchaseScreenValidator(ShopLocators locators) {
        this.components = new ShopComponentAssertions(Objects.requireNonNull(locators, "locators"));
    }

    public List<AtfAssertion> product(Page page) {
        return concat(
                components.rendered(page, ShopControl.PRODUCT_NAME, ShopControl.PRODUCT_PRICE,
                        ShopControl.PRODUCT_DESCRIPTION, ShopControl.PRODUCT_QUANTITY,
                        ShopControl.ADD_TO_CART),
                components.interactive(page, ShopControl.PRODUCT_QUANTITY, ShopControl.ADD_TO_CART));
    }

    public List<AtfAssertion> cart(Page page) {
        return concat(
                components.rendered(page, ShopControl.CART_PRODUCT_TITLE,
                        ShopControl.CART_PRODUCT_QUANTITY, ShopControl.CART_PROCEED),
                components.interactive(page, ShopControl.CART_PRODUCT_QUANTITY,
                        ShopControl.CART_PROCEED));
    }

    public List<AtfAssertion> authenticatedCheckout(Page page) {
        return concat(
                List.of(components.rendered(page, ShopControl.AUTHENTICATED_PROCEED)),
                List.of(components.interactive(page, ShopControl.AUTHENTICATED_PROCEED)));
    }

    public List<AtfAssertion> guestCheckout(Page page) {
        return concat(
                components.rendered(page, ShopControl.GUEST_TAB, ShopControl.GUEST_EMAIL,
                        ShopControl.GUEST_FIRST_NAME, ShopControl.GUEST_LAST_NAME,
                        ShopControl.GUEST_SUBMIT),
                components.interactive(page, ShopControl.GUEST_EMAIL, ShopControl.GUEST_FIRST_NAME,
                        ShopControl.GUEST_LAST_NAME, ShopControl.GUEST_SUBMIT));
    }

    public List<AtfAssertion> billing(Page page) {
        return concat(
                components.rendered(page, ShopControl.COUNTRY, ShopControl.POSTAL_CODE,
                        ShopControl.HOUSE_NUMBER, ShopControl.STREET, ShopControl.CITY,
                        ShopControl.STATE, ShopControl.BILLING_PROCEED),
                components.interactive(page, ShopControl.COUNTRY, ShopControl.POSTAL_CODE,
                        ShopControl.HOUSE_NUMBER, ShopControl.STREET, ShopControl.CITY,
                        ShopControl.STATE, ShopControl.BILLING_PROCEED));
    }

    public List<AtfAssertion> payment(Page page) {
        return concat(
                components.rendered(page, ShopControl.PAYMENT_METHOD, ShopControl.PAYMENT_CHECK),
                components.interactive(page, ShopControl.PAYMENT_METHOD, ShopControl.PAYMENT_CHECK));
    }

    public List<AtfAssertion> paymentChecked(Page page) {
        return List.of(components.rendered(page, ShopControl.PAYMENT_SUCCESS));
    }

    public List<AtfAssertion> orderConfirmation(Page page) {
        return List.of(components.rendered(page, ShopControl.ORDER_CONFIRMATION));
    }

    @SafeVarargs
    private static List<AtfAssertion> concat(List<AtfAssertion>... groups) {
        return java.util.Arrays.stream(groups).flatMap(List::stream).toList();
    }
}
