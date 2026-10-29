package atf.shop.support.validation;

import atf.reporting.contract.AtfAssertion;
import atf.shop.flow.ShopPurchaseFlow;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Business success means a terminal order confirmation with an invoice, not only a payment check. */
public final class ShopPurchaseConfirmation {
    private static final Pattern INVOICE = Pattern.compile("(?<![\\p{L}\\p{N}_-])INV-\\S+");
    private ShopPurchaseConfirmation() { }

    public static List<AtfAssertion> assertions(ShopPurchaseFlow.PurchaseResult result) {
        String confirmation = Objects.requireNonNull(result, "purchase result").confirmation();
        boolean confirmed = confirmation != null
                && confirmation.contains("Thanks for your order!")
                && !invoiceNumber(confirmation).isBlank();
        return List.of(new AtfAssertion("Order confirmation",
                "confirmation includes a successful order and invoice",
                confirmation == null ? "no confirmation was observed" : confirmation, confirmed));
    }

    /** Exactly one complete invoice token is observable; prefixes and ambiguous confirmations are rejected. */
    public static String invoiceNumber(String confirmation) {
        if (confirmation == null) return "";
        var tokens = INVOICE.matcher(confirmation);
        if (!tokens.find()) return "";
        String invoice = tokens.group().replaceFirst("[.,;:!?]+$", "");
        if (!invoice.matches("INV-[\\p{L}\\p{N}_-]+")) return "";
        return tokens.find() ? "" : invoice;
    }
}
