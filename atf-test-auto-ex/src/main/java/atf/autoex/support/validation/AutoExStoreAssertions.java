package atf.autoex.support.validation;

import atf.reporting.contract.AtfAssertion;

import java.util.Optional;

import static atf.autoex.support.validation.AutoExUiText.ACCOUNT_CREATED;
import static atf.autoex.support.validation.AutoExUiText.ACCOUNT_DELETED;
import static atf.autoex.support.validation.AutoExUiText.CART_ADDED;
import static atf.autoex.support.validation.AutoExUiText.CART_ADDED_DETAIL;
import static atf.autoex.support.validation.AutoExUiText.CART_EMPTY;
import static atf.autoex.support.validation.AutoExUiText.IN_STOCK;
import static atf.autoex.support.validation.AutoExUiText.ORDER_CONFIRMED;
import static atf.autoex.support.validation.AutoExUiText.ORDER_PLACED;
import static atf.autoex.support.validation.AutoExUiText.PAYMENT_PAGE;
import static atf.autoex.support.validation.AutoExUiText.STORE_BANNER;

/** Pure, site-specific verdicts shared by pages and independent business journeys. */
public final class AutoExStoreAssertions {
    private static final String MISSING_OBSERVATION = "<missing>";

    private AutoExStoreAssertions() { }

    public static AtfAssertion storeAvailability(String observed) {
        return contains("Store availability", STORE_BANNER, observed);
    }

    public static AtfAssertion accountCreated(String observed) {
        return contains("Account creation", ACCOUNT_CREATED, observed);
    }

    public static AtfAssertion accountDeleted(String observed) {
        return contains("Account cleanup", ACCOUNT_DELETED, observed);
    }

    public static AtfAssertion cartAddition(String observed) {
        String expected = "%s; %s".formatted(CART_ADDED.text(), CART_ADDED_DETAIL.text());
        boolean passed = observed != null && observed.contains(CART_ADDED.text())
                && observed.contains(CART_ADDED_DETAIL.text());
        return assertion("Add-to-cart confirmation", expected, observed, passed);
    }

    public static AtfAssertion cartMutationConfirmation(boolean mutationSucceeded,
                                                         Optional<String> observed) {
        Optional<String> confirmation = observed == null ? Optional.empty() : observed;
        if (mutationSucceeded) return cartAddition(confirmation.orElse(null));
        String expected = "no success confirmation after a rejected or unobserved cart request";
        String actual = confirmation.orElse("confirmation absent");
        return assertion("Add-to-cart confirmation", expected, actual, confirmation.isEmpty());
    }

    public static AtfAssertion emptyCart(String observed) {
        return contains("Empty cart", CART_EMPTY, observed);
    }

    public static AtfAssertion stockAvailability(String observed) {
        return contains("Availability", IN_STOCK, observed);
    }

    public static AtfAssertion paymentPage(String observed) {
        return assertion("Payment page", PAYMENT_PAGE.text(), observed, PAYMENT_PAGE.text().equals(observed));
    }

    public static AtfAssertion orderPlaced(String observed) {
        return assertion("Order status", ORDER_PLACED.text() + " (case-insensitive)", observed,
                ORDER_PLACED.text().equalsIgnoreCase(observed));
    }

    public static AtfAssertion orderConfirmation(String observed) {
        return contains("Order confirmation", ORDER_CONFIRMED, observed);
    }

    private static AtfAssertion contains(String name, AutoExUiText expected, String observed) {
        return assertion(name, expected.text(), observed,
                observed != null && observed.contains(expected.text()));
    }

    private static AtfAssertion assertion(String name, String expected, String observed, boolean passed) {
        return new AtfAssertion(name, expected, observed == null ? MISSING_OBSERVATION : observed, passed);
    }
}
