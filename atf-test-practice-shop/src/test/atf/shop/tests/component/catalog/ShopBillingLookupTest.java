package atf.shop.tests.component.catalog;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.pages.ShopBillingPage;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@ShopComponentTest
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
