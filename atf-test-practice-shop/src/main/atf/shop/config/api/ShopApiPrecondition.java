package atf.shop.config.api;

import java.util.Locale;

/** Selects the API state required before the first screen is exercised. */
public enum ShopApiPrecondition {
    ANONYMOUS,
    AUTHENTICATED;

    public static ShopApiPrecondition from(String value) {
        if (value == null || value.isBlank()) return AUTHENTICATED;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unsupported Practice Shop API precondition: " + value
                    + ". Expected anonymous or authenticated", error);
        }
    }
}
