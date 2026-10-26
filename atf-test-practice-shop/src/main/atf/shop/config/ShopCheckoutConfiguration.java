package atf.shop.config;

import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.model.ShopGuest;

import java.util.Objects;

/** Typed, YAML-driven purchase inputs; no checkout value is hidden in a flow. */
public record ShopCheckoutConfiguration(
        int quantity,
        ShopGuest guest,
        ShopBillingAddress billing,
        String paymentMethod) {

    public ShopCheckoutConfiguration {
        if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
        guest = Objects.requireNonNull(guest, "guest");
        billing = Objects.requireNonNull(billing, "billing");
        paymentMethod = required(paymentMethod, "paymentMethod");
    }

    static ShopCheckoutConfiguration defaults() {
        return new ShopCheckoutConfiguration(2,
                new ShopGuest("atf.practice.shop@example.test", "ATF", "Shop"),
                new ShopBillingAddress("Romania", "010101", "10", "Test Street",
                        "Bucharest", "Bucharest"),
                "Cash on Delivery");
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
