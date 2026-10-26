package atf.shop.config.model;

import java.util.Locale;

/** Business choice, independent from UI/API evidence and capture policy. */
public enum ShopBillingStrategy {
    CONFIGURED_FIELDS, POSTAL_LOOKUP;

    public static ShopBillingStrategy from(String value) {
        return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
