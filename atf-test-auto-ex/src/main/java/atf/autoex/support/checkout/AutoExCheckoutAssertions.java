package atf.autoex.support.checkout;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.config.AutoExConfiguration.Registration;

import java.util.List;

/** Business expectations shared by the cart and checkout checkpoints. */
public final class AutoExCheckoutAssertions {
    private static final String SELECTED_CUSTOMER_TITLE = "Mr.";

    private AutoExCheckoutAssertions() { }

    public static List<AtfAssertion> orderLine(Catalog product, String expectedTotal, AutoExOrderLine actual) {
        return List.of(
                equal("Product", product.productName(), actual.productName()),
                equal("Unit price", product.unitPrice(), actual.unitPrice()),
                equal("Quantity", Integer.toString(product.quantity()), actual.quantity()),
                equal("Line total", expectedTotal, actual.total()));
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

    private static AtfAssertion equal(String field, String expected, String actual) {
        return new AtfAssertion(field, expected, actual, expected.equals(actual));
    }

    private static String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }
}
