package atf.autoex.business.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Pure invoice contract: expected identity/amount never complete missing observations. */
public final class AutoExInvoiceValidator {
    private static final String INVOICE_FILE_NAME = "invoice.txt";
    private static final String CURRENCY_PREFIX = "Rs.";
    private static final Pattern CUSTOMER = Pattern.compile("(?m)^Hi\\h+([^,\\r\\n]+)");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)total purchase amount\\s+is\\s+(?:Rs\\.\\s*)?([0-9]+(?:\\.[0-9]+)?)");

    private AutoExInvoiceValidator() { }

    public static List<Check> validate(String customer, String expectedTotal, AutoExInvoiceSnapshot invoice) {
        var checks = new ArrayList<>(contentChecks(customer, expectedTotal, invoice));
        checks.add(new Check("Invoice download", "downloaded invoice file", invoice.fileName(), invoice.available()));
        return List.copyOf(checks);
    }

    public static List<Check> contentChecks(String customer, String expectedTotal, AutoExInvoiceSnapshot invoice) {
        String actualCustomer = extract(CUSTOMER, invoice.contents());
        String actualAmount = extract(AMOUNT, invoice.contents());
        String expectedAmount = expectedTotal.replace(CURRENCY_PREFIX, "").strip();
        boolean correctAmount = !actualAmount.isBlank()
                && new BigDecimal(expectedAmount).compareTo(new BigDecimal(actualAmount)) == 0;
        return List.of(new Check("Invoice file", INVOICE_FILE_NAME, invoice.fileName(),
                        INVOICE_FILE_NAME.equals(invoice.fileName())),
                new Check("Invoice customer", customer, actualCustomer, customer.equals(actualCustomer)),
                new Check("Invoice amount", expectedAmount, actualAmount, correctAmount));
    }

    private static String extract(Pattern pattern, String contents) {
        var match = pattern.matcher(contents);
        return match.find() ? match.group(1).strip() : "";
    }

    public record Check(String label, String expected, String actual, boolean passed) { }
}
