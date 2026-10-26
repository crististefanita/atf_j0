package atf.shop.support.validation;

import atf.reporting.contract.AtfAssertion;
import atf.shop.flow.ShopPurchaseFlow;

import java.util.List;
import java.util.Objects;

/** Business success means a terminal order confirmation with an invoice, not only a payment check. */
public final class ShopPurchaseConfirmation {
    private ShopPurchaseConfirmation() { }

    public static List<AtfAssertion> assertions(ShopPurchaseFlow.PurchaseResult result) {
        String confirmation = Objects.requireNonNull(result, "purchase result").confirmation();
        boolean confirmed = confirmation != null
                && confirmation.contains("Thanks for your order!")
                && confirmation.contains("INV-");
        return List.of(new AtfAssertion("Order confirmation",
                "confirmation includes a successful order and invoice",
                confirmation == null ? "no confirmation was observed" : confirmation, confirmed));
    }
}
