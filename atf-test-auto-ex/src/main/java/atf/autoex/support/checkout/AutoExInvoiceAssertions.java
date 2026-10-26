package atf.autoex.support.checkout;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.business.invoice.AutoExInvoiceSnapshot;
import atf.autoex.business.invoice.AutoExInvoiceValidator;

import java.util.List;

/** The downloaded invoice must identify this customer and the amount of this order. */
public final class AutoExInvoiceAssertions {
    private AutoExInvoiceAssertions() { }

    public static List<AtfAssertion> validate(String customer, String expectedTotal,
                                              String fileName, String contents) {
        return reporting(AutoExInvoiceValidator.contentChecks(customer, expectedTotal,
                new AutoExInvoiceSnapshot(fileName, contents, true)));
    }

    public static List<AtfAssertion> validate(String customer, String expectedTotal, AutoExInvoiceSnapshot invoice) {
        return reporting(AutoExInvoiceValidator.validate(customer, expectedTotal, invoice));
    }

    private static List<AtfAssertion> reporting(List<AutoExInvoiceValidator.Check> checks) {
        return checks.stream()
                .map(check -> new AtfAssertion(check.label(), check.expected(), check.actual(), check.passed())).toList();
    }
}
