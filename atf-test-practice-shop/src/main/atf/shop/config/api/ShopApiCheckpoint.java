package atf.shop.config.api;

import java.util.Locale;

/**
 * Stable API evidence seams in the Practice Shop journey.
 *
 * <p>The enum is intentionally consumer-owned: the reporting library knows
 * how to render a checkpoint, while this module decides which business seam
 * owns an exchange.</p>
 */
public enum ShopApiCheckpoint {
    PRODUCT("product"),
    REGISTRATION("registration"),
    LOGIN("login"),
    CART("cart"),
    PAYMENT("payment"),
    ORDER("order");

    private final String key;

    ShopApiCheckpoint(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static ShopApiCheckpoint from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ShopApiCheckpoint checkpoint : values()) {
            if (checkpoint.key.equals(normalized) || checkpoint.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return checkpoint;
            }
        }
        throw new IllegalArgumentException("Unsupported Practice Shop API evidence checkpoint: " + value
                + ". Expected product, registration, login, cart, payment or order");
    }
}
