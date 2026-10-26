package atf.autoex.support.checkout;

import java.util.Objects;

/** Values read from the same product row in cart and checkout. */
public record AutoExOrderLine(String productName, String unitPrice, String quantity, String total) {
    public AutoExOrderLine {
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(total, "total");
    }
}
