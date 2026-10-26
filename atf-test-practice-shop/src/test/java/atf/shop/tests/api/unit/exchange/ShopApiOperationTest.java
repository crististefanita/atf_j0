package atf.shop.tests.api.unit.exchange;

import atf.api.core.http.request.HttpMethod;
import atf.api.testkit.AtfUnitTest;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.ShopApiRoutes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class ShopApiOperationTest {
    @Test
    void resolvesStableIntentWithoutPuttingDynamicIdsIntoTheEnum() {
        var product = ShopApiOperation.resolve(HttpMethod.GET.value(),
                ShopApiRoutes.product("product-42"));
        var cart = ShopApiOperation.resolve(HttpMethod.POST.value(),
                ShopApiRoutes.cart("cart-7"));
        var update = ShopApiOperation.resolve(HttpMethod.PUT.value(),
                ShopApiRoutes.updateCartQuantity("cart-7"));
        var remove = ShopApiOperation.resolve(HttpMethod.DELETE.value(),
                ShopApiRoutes.removeCartProduct("cart-7", "product-42"));
        var order = ShopApiOperation.resolve(
                HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE);
        var registration = ShopApiOperation.resolve(
                HttpMethod.POST.value(), ShopApiRoutes.USER_REGISTER);

        assertEquals(ShopApiOperation.PRODUCT_DETAIL, product);
        assertEquals(ShopApiOperation.CART_ADD, cart);
        assertEquals(ShopApiOperation.CART_UPDATE, update);
        assertEquals(ShopApiOperation.CART_REMOVE, remove);
        assertEquals(ShopApiOperation.ORDER_CREATE, order);
        assertEquals(ShopApiOperation.USER_REGISTER, registration);
        assertEquals(ShopApiRoutes.PRODUCT_DETAIL_TEMPLATE, product.routeTemplate());
        assertEquals(ShopApiRoutes.CART_RESOURCE_TEMPLATE, cart.routeTemplate());
        assertEquals(ShopApiRoutes.CART_QUANTITY_TEMPLATE, update.routeTemplate());
        assertEquals(ShopApiRoutes.CART_PRODUCT_TEMPLATE, remove.routeTemplate());
    }

    @Test
    void reportTitleContainsProtocolRouteAndShortHumanIntent() {
        String title = ShopApiOperation.reportTitle(
                HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK);

        assertEquals("POST /payment/check — validate payment selection", title);
    }

    @Test
    void explicitInvocationIntentOverridesOnlyTheGenericOperationDescription() {
        assertEquals("POST /users/login — reject invalid credentials",
                ShopApiOperation.reportTitle(HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                        "reject invalid credentials"));
        assertEquals(ShopApiOperation.reportTitle(HttpMethod.POST.value(), ShopApiRoutes.LOGIN),
                ShopApiOperation.reportTitle(HttpMethod.POST.value(), ShopApiRoutes.LOGIN, " "));
    }

    @Test
    void classifiesObservedQueryAsSearchWithoutChangingItsCapturedMethod() {
        assertEquals(ShopApiOperation.SEARCH,
                ShopApiOperation.resolve(ShopApiOperation.OBSERVED_QUERY_METHOD,
                        ShopApiRoutes.PRODUCT_SEARCH));
        assertEquals("QUERY /products/search — find configured product",
                ShopApiOperation.reportTitle(ShopApiOperation.OBSERVED_QUERY_METHOD,
                        ShopApiRoutes.PRODUCT_SEARCH));
    }

    @Test
    void labelsOrderCreationAsTheBusinessMutation() {
        assertEquals("POST /invoices/guest — create order",
                ShopApiOperation.reportTitle(
                        HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE));
        assertEquals("POST /invoices — create order",
                ShopApiOperation.reportTitle(
                        HttpMethod.POST.value(), ShopApiRoutes.INVOICE_CREATE));
    }
}
