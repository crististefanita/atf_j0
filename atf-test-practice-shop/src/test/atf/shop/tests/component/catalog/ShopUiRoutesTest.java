package atf.shop.tests.component.catalog;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.routes.ShopUiRoutes;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@ShopComponentTest
class ShopUiRoutesTest {
    @Test
    void buildsOnlyRelativeUiRoutes() {
        assertEquals("/product/1", ShopUiRoutes.product("1"));
        assertEquals("/categories/pliers", ShopUiRoutes.category("pliers"));
        assertEquals("/checkout", ShopUiRoutes.CHECKOUT);
    }

    @Test
    void rejectsMissingDynamicRouteSegments() {
        assertThrows(IllegalArgumentException.class, () -> ShopUiRoutes.product(" "));
        assertThrows(IllegalArgumentException.class, () -> ShopUiRoutes.category(null));
    }
}
