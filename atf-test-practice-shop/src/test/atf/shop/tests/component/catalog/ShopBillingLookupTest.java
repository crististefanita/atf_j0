package atf.shop.tests.component.catalog;

import atf.testkit.AtfComponentTest;
import atf.shop.pages.ShopBillingPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest
class ShopBillingLookupTest {
    @Test
    void lookupEvidenceRequiresEveryApplicationGeneratedAddressField() {
        var address = new ShopBillingPage.ObservedAddress("Wayne Lock", "Cartwrightmouth", "Alaska");

        assertEquals("Wayne Lock", address.street());
        assertEquals("Cartwrightmouth", address.city());
        assertEquals("Alaska", address.state());
    }

    @Test
    void blankLookupResultFailsInsteadOfProducingAnUnauditableBillingCheckpoint() {
        assertThrows(IllegalStateException.class,
                () -> new ShopBillingPage.ObservedAddress("Wayne Lock", "  ", "Alaska"));
    }
}
