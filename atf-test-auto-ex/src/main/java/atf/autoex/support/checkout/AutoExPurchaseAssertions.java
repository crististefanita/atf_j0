package atf.autoex.support.checkout;

import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.config.AutoExConfiguration.Registration;
import atf.autoex.support.catalog.AutoExCatalogAssertions;
import atf.autoex.support.validation.AutoExStoreAssertions;
import atf.reporting.contract.AtfAssertion;

import java.util.ArrayList;
import java.util.List;

/** Projects configured expectations and UI snapshots using the existing purchase rules. */
public final class AutoExPurchaseAssertions {
    private AutoExPurchaseAssertions() { }

    public static List<AtfAssertion> product(Catalog expected, String details) {
        return List.of(new AtfAssertion("Product name", expected.productName(), details,
                        details.contains(expected.productName())),
                new AtfAssertion("Product price", expected.unitPrice(), details,
                        AutoExCatalogAssertions.containsExactLine(details, expected.unitPrice())),
                AutoExStoreAssertions.stockAvailability(details));
    }

    public static List<AtfAssertion> checkout(Catalog product, Registration registration,
                                             String delivery, String billing, AutoExOrderLine line,
                                             String observedTotal) {
        String expectedTotal = product.expectedTotal();
        var checks = new ArrayList<>(AutoExCheckoutAssertions.address("Delivery", registration, delivery));
        checks.addAll(AutoExCheckoutAssertions.address("Billing", registration, billing));
        checks.addAll(AutoExCheckoutAssertions.orderLine(product, expectedTotal, line));
        checks.add(new AtfAssertion("Order total", expectedTotal, observedTotal,
                expectedTotal.equals(observedTotal)));
        return List.copyOf(checks);
    }

    public static List<AtfAssertion> cleanup(String expectedLanding, String observedLanding, String deletion) {
        return List.of(new AtfAssertion("Post-cleanup landing page", expectedLanding, observedLanding,
                        expectedLanding.equals(observedLanding)),
                AutoExStoreAssertions.accountDeleted(deletion));
    }
}
