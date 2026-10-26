package atf.shop.locators;

import atf.api.ui.dom.CssStringEscaper;

import java.util.Objects;

/** Builds the stable card locator used to select one exact configured product. */
public final class ShopProductSelector {
    public static final String PRODUCT_CARD = "a[data-test^='product-']";
    private static final String PRODUCT_NAME = "[data-test='product-name']";

    private ShopProductSelector() { }

    /**
     * Selects a product card whose product-name element is an exact text match.
     * The Playwright text-is selector prevents "Pliers" from selecting "Combination Pliers".
     */
    public static String exactProductCard(String productName) {
        String name = required(productName, "productName");
        return PRODUCT_CARD + ":has(" + PRODUCT_NAME + ":text-is('"
                + CssStringEscaper.escapeQuotedValue(name) + "'))";
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
