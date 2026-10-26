package atf.shop.config.model;

import java.util.Objects;

/** Deterministic billing input for the public demo checkout. */
public record ShopBillingAddress(String country, String postalCode,
                                         String houseNumber, String street,
                                         String city, String state) {
    public ShopBillingAddress {
        country = required(country, "country");
        postalCode = required(postalCode, "postalCode");
        houseNumber = required(houseNumber, "houseNumber");
        street = required(street, "street");
        city = required(city, "city");
        state = required(state, "state");
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
