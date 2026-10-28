package atf.shop.tests.component.catalog;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.locators.ShopProductSelector;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@ShopComponentTest
class ShopProductSelectorTest {
    @Test
    void selectsTheExactProductNameWithinTheProductCard() {
        assertEquals(
                "a[data-test^='product-']:has([data-test='product-name']:text-is('Pliers'))",
                ShopProductSelector.exactProductCard("Pliers"));
    }

    @Test
    void escapesDynamicProductNamesWithoutFallingBackToPartialText() {
        assertEquals(
                "a[data-test^='product-']:has([data-test='product-name']:text-is('Pliers\\' \\\\ Pro'))",
                ShopProductSelector.exactProductCard("Pliers' \\ Pro"));
    }

    @Test
    void rejectsBlankProductNamesBeforeBuildingASelector() {
        assertThrows(NullPointerException.class,
                () -> ShopProductSelector.exactProductCard(null));
        assertThrows(IllegalArgumentException.class,
                () -> ShopProductSelector.exactProductCard("  "));
    }
}
