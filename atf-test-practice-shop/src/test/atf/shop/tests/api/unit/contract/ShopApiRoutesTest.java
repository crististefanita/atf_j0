package atf.shop.tests.api.unit.contract;

import atf.testkit.AtfUnitTest;
import atf.shop.api.ShopApiRoutes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ShopApiRoutesTest {
    @Test
    void dynamicRoutesUseTheSameCanonicalPathVocabularyAsTheFactory() {
        assertEquals("/products/product-1", ShopApiRoutes.product("product-1"));
        assertEquals("/carts/cart-1", ShopApiRoutes.cart("cart-1"));
        assertEquals("/carts/cart-1/product/quantity", ShopApiRoutes.updateCartQuantity("cart-1"));
        assertEquals("/carts/cart-1/product/product-1",
                ShopApiRoutes.removeCartProduct("cart-1", "product-1"));
    }

    @Test
    void dynamicRouteArgumentsFailFastInsteadOfProducingMalformedPaths() {
        assertThrows(IllegalArgumentException.class, () -> ShopApiRoutes.product(" "));
        assertThrows(IllegalArgumentException.class, () -> ShopApiRoutes.cart(null));
        assertThrows(IllegalArgumentException.class,
                () -> ShopApiRoutes.removeCartProduct("cart-1", ""));
    }
}
