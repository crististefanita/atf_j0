package atf.shop.tests.component.configuration;

import atf.api.testkit.AtfComponentTest;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.ShopCheckoutConfiguration;
import atf.shop.config.model.ShopGuest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest
class ShopCheckoutConfigurationTest {
    @Test
    void checkoutInputsRemainTypedAndIndependentFromTheFlow() {
        var checkout = new ShopCheckoutConfiguration(3,
                new ShopGuest("guest@example.test", "Ada", "Lovelace"),
                new ShopBillingAddress("Romania", "010101", "10", "Test Street",
                        "Bucharest", "Bucharest"),
                "Cash on Delivery");

        assertEquals(3, checkout.quantity());
        assertEquals("guest@example.test", checkout.guest().email());
        assertEquals("Romania", checkout.billing().country());
        assertEquals("Cash on Delivery", checkout.paymentMethod());
    }

    @Test
    void invalidCheckoutValuesFailBeforeTheBrowserStarts() {
        assertThrows(IllegalArgumentException.class, () -> new ShopCheckoutConfiguration(0,
                new ShopGuest("guest@example.test", "Ada", "Lovelace"),
                new ShopBillingAddress("Romania", "010101", "10", "Test Street",
                        "Bucharest", "Bucharest"),
                "Cash on Delivery"));
    }
}
