package atf.shop.support.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Immutable pre-payment price and configured quantity, independent of the created order. */
public record ShopOrderExpectation(BigDecimal unitPrice, int quantity) {
    public ShopOrderExpectation {
        unitPrice = Objects.requireNonNull(unitPrice, "unitPrice").setScale(2, RoundingMode.UNNECESSARY);
        if (unitPrice.signum() <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("Order price and quantity must be positive");
        }
    }

    /** Shop exposes an ungrouped decimal in its unit-price element, separately from the currency symbol. */
    public static ShopOrderExpectation fromUi(String price, int quantity) {
        if (price == null || !price.strip().matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Shop unit price is not an ungrouped decimal amount");
        }
        return new ShopOrderExpectation(new BigDecimal(price.strip()), quantity);
    }

    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean matchesDisplayedUnitPrice(String amount) {
        return matchesDisplayed(amount, unitPrice);
    }

    public boolean matchesDisplayedTotal(String amount) {
        return matchesDisplayed(amount, total());
    }

    private static boolean matchesDisplayed(String amount, BigDecimal expected) {
        if (amount == null || !amount.strip().matches("\\$[0-9]+(?:\\.[0-9]{1,2})?")) return false;
        return new BigDecimal(amount.strip().substring(1)).compareTo(expected) == 0;
    }
}
