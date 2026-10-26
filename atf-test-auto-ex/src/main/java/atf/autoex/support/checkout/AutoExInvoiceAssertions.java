package atf.autoex.support.checkout;

import atf.reporting.contract.AtfAssertion;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/** The downloaded invoice must identify this customer and the amount of this order. */
public final class AutoExInvoiceAssertions {
    private static final String INVOICE_FILE_NAME = "invoice.txt";
    private static final String CURRENCY_PREFIX = "Rs.";
    private static final Pattern CUSTOMER = Pattern.compile("(?m)^Hi\\h+([^,\\r\\n]+)");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)total purchase amount\\s+is\\s+(?:Rs\\.\\s*)?([0-9]+(?:\\.[0-9]+)?)");

    private AutoExInvoiceAssertions() { }

    public static List<AtfAssertion> validate(String customer, String expectedTotal,
                                              String fileName, String contents) {
        String actualCustomer = extract(CUSTOMER, contents);
        String actualAmount = extract(AMOUNT, contents);
        String expectedAmount = expectedTotal.replace(CURRENCY_PREFIX, "").strip();
        boolean correctAmount = !actualAmount.isBlank()
                && new BigDecimal(expectedAmount).compareTo(new BigDecimal(actualAmount)) == 0;
        return List.of(
                new AtfAssertion("Invoice file", INVOICE_FILE_NAME, fileName,
                        INVOICE_FILE_NAME.equals(fileName)),
                new AtfAssertion("Invoice customer", customer, actualCustomer, customer.equals(actualCustomer)),
                new AtfAssertion("Invoice amount", expectedAmount, actualAmount, correctAmount));
    }

    private static String extract(Pattern pattern, String contents) {
        var match = pattern.matcher(contents);
        return match.find() ? match.group(1).strip() : "";
    }
}
