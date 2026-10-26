package atf.autoex.support.checkout;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.business.cart.AutoExCartResult;
import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.config.AutoExConfiguration.Registration;

import java.util.List;

/** Business expectations shared by the cart and checkout checkpoints. */
public final class AutoExCheckoutAssertions {
    private static final String SELECTED_CUSTOMER_TITLE = "Mr.";

    private AutoExCheckoutAssertions() { }

    public static List<AtfAssertion> orderLine(Catalog product, String expectedTotal, AutoExOrderLine actual) {
        return cart(AutoExCartResult.compare(expectedLine(product, expectedTotal), actual));
    }

    public static AutoExOrderLine expectedLine(Catalog product, String expectedTotal) {
        return new AutoExOrderLine(product.productName(), product.unitPrice(),
                Integer.toString(product.quantity()), expectedTotal);
    }

    public static List<AtfAssertion> cart(AutoExCartResult result) {
        return result.checks().stream().map(check -> new AtfAssertion(
                check.label(), check.expected(), check.actual(), check.passed())).toList();
    }

    public static List<AtfAssertion> address(String label, Registration profile, String renderedAddress) {
        List<String> lines = renderedAddress.lines().map(AutoExCheckoutAssertions::normalize).toList();
        String recipient = SELECTED_CUSTOMER_TITLE + " " + profile.displayName();
        String locality = "%s %s %s".formatted(profile.city(), profile.state(), profile.postalCode());
        return List.of(
                line(label, "recipient", recipient, lines),
                line(label, "company", profile.company(), lines),
                line(label, "address line 1", profile.address1(), lines),
                line(label, "address line 2", profile.address2(), lines),
                line(label, "city/state/postcode", locality, lines),
                line(label, "country", profile.country(), lines),
                line(label, "phone", profile.mobileNumber(), lines));
    }

    private static AtfAssertion line(String label, String field, String expected, List<String> lines) {
        return new AtfAssertion(label + " — " + field, expected, String.join(" | ", lines),
                lines.contains(normalize(expected)));
    }

    private static String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }
}
