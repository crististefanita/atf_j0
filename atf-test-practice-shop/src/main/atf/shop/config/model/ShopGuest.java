package atf.shop.config.model;

import java.util.Objects;

/** Non-sensitive guest identity used by the demo checkout journey. */
public record ShopGuest(String email, String firstName, String lastName) {
    public ShopGuest {
        email = required(email, "email");
        firstName = required(firstName, "firstName");
        lastName = required(lastName, "lastName");
    }

    public String displayName() {
        return firstName + " " + lastName;
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
