package atf.shop.tests.api.unit.exchange;

import atf.shop.support.validation.ShopOrderExpectation;

import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.testkit.AtfUnitTest;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopApiQueryParameter;
import atf.shop.api.contract.ShopProductSort;
import atf.shop.support.api.ShopApiAssertions;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ShopApiAssertionsTest {
    @Test
    void nearMissProductIdentityFailsPathAssertions() {
        var search = exchange(ShopApiOperation.SEARCH.wireValue(), ShopApiRoutes.PRODUCT_SEARCH,
                ShopApiHttpStatus.OK,
                "{\"q\":\"Pliers\"}",
                "{\"data\":[{\"id\":\"p-1\",\"name\":\"Pliers\"}]}");
        var detail = exchange(HttpMethod.GET.value(), ShopApiRoutes.product("p-1"),
                ShopApiHttpStatus.OK, "",
                "{\"id\":\"p-1\",\"name\":\"Pliers near miss\"}");

        var assertions = ShopApiAssertions.product(search, detail, "Pliers", "p-1");

        assertTrue(assertions.stream().anyMatch(a -> a.name().contains("catalog search") && a.passed()));
        assertFalse(assertions.stream().anyMatch(a -> a.name().contains("product identity") && a.passed()));
    }

    @Test
    void catalogQueryUsesSharedUriDecoderForPlusAndPercentEncodedValues() {
        var search = new ShopApiNetworkJournal.Exchange(
                HttpMethod.GET.value(), ShopApiRoutes.PRODUCT_SEARCH,
                ShopApiScenarioFixture.API_ENDPOINT + ShopApiRoutes.PRODUCT_SEARCH
                        + "?q=Pliers+Pro&unused=a%2Bb",
                ShopApiHttpStatus.OK.code(), "",
                "{\"data\":[{\"id\":\"p-1\",\"name\":\"Pliers Pro\"}]}",
                Map.of(), HttpHeaderProfiles.JSON_CONTENT);
        var detail = exchange(HttpMethod.GET.value(), ShopApiRoutes.product("p-1"),
                ShopApiHttpStatus.OK, "", "{\"id\":\"p-1\",\"name\":\"Pliers Pro\"}");

        var assertions = ShopApiAssertions.product(search, detail, "Pliers Pro", "p-1");

        assertTrue(assertions.stream().filter(a -> a.name().equals("API request query · catalog search"))
                .findFirst().orElseThrow().passed());
    }

    @Test
    void unauthorizedCatalogSearchFailsThePreconditionInsteadOfPassingByShape() {
        var search = exchange(ShopApiOperation.SEARCH.wireValue(), ShopApiRoutes.PRODUCT_SEARCH,
                ShopApiHttpStatus.UNAUTHORIZED,
                "", "{\"message\":\"Unauthorized\"}");
        var detail = exchange(HttpMethod.GET.value(), ShopApiRoutes.product("p-1"),
                ShopApiHttpStatus.OK, "", "{\"id\":\"p-1\",\"name\":\"Pliers\"}");

        var assertions = ShopApiAssertions.product(search, detail, "Pliers", "p-1");

        assertFalse(assertions.stream().anyMatch(a ->
                a.name().equals("API precondition · catalog search") && a.passed()));
    }

    @Test
    void loginDoesNotPassWhenTheObservedJsonTransportIsInvalid() {
        var login = exchange(HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                ShopApiHttpStatus.OK,
                "{\"email\":\"shop@example.test\",\"password\":\"secret\"}",
                "{}",
                Map.of(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON,
                        HttpHeader.CONTENT_LENGTH.value(), "0"));

        var assertions = ShopApiAssertions.login(login);

        assertFalse(assertions.stream().anyMatch(a ->
                a.name().equals("API login response JSON") && a.passed()));
    }

    @Test
    void orderRequiresServerInvoiceToMatchTheVisibleOrderConfirmation() {
        var order = exchange(HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE,
                ShopApiHttpStatus.OK, "{}",
                "{\"id\":\"order-1\",\"invoice_number\":\"INV-2026000007\",\"total\":24.02}");

        var passing = ShopApiAssertions.order(order,
                "Thanks for your order! Your invoice number is INV-2026000007.", ShopOrderExpectation.fromUi("12.01", 2));
        var mismatched = ShopApiAssertions.order(order,
                "Thanks for your order! Your invoice number is INV-OTHER.", ShopOrderExpectation.fromUi("12.01", 2));

        assertTrue(passing.stream().allMatch(a -> a.passed()));
        assertFalse(mismatched.stream().filter(a -> a.name().equals("UI/API order correlation"))
                .findFirst().orElseThrow().passed());
    }

    @Test
    void liveCatalogAssertionRequiresFilteredCategoryAndAscendingPrices() {
        String categoryId = "pliers-category";
        var ascending = categoryExchange(categoryId,
                "[{\"id\":\"a\",\"price\":9.17,\"category\":{\"id\":\"pliers-category\"}},"
                        + "{\"id\":\"b\",\"price\":14.15,\"category\":{\"id\":\"pliers-category\"}}]");
        var wrongOrder = categoryExchange(categoryId,
                "[{\"id\":\"b\",\"price\":14.15,\"category\":{\"id\":\"pliers-category\"}},"
                        + "{\"id\":\"a\",\"price\":9.17,\"category\":{\"id\":\"pliers-category\"}}]");
        var wrongCategory = categoryExchange(categoryId,
                "[{\"id\":\"a\",\"price\":9.17,\"category\":{\"id\":\"other\"}}]");

        var passing = ShopApiAssertions.categoryProductsSortedByPrice(ascending, categoryId);
        var unsorted = ShopApiAssertions.categoryProductsSortedByPrice(wrongOrder, categoryId);
        var unfiltered = ShopApiAssertions.categoryProductsSortedByPrice(wrongCategory, categoryId);

        assertTrue(passing.stream().allMatch(a -> a.passed()));
        assertFalse(unsorted.stream().filter(a -> a.name().equals(
                "API response · ascending price order")).findFirst().orElseThrow().passed());
        assertFalse(unfiltered.stream().filter(a -> a.name().equals(
                "API response · category filter")).findFirst().orElseThrow().passed());
    }

    @Test
    void cartMutationAssertionsRequireReadBackAndAnEmptyFinalCart() {
        String cartId = "cart-live-1";
        String productId = "product-live-1";
        String updatePath = ShopApiRoutes.updateCartQuantity(cartId);
        String removePath = ShopApiRoutes.removeCartProduct(cartId, productId);
        var update = exchange(HttpMethod.PUT.value(), updatePath, ShopApiHttpStatus.OK,
                "{\"product_id\":\"product-live-1\",\"quantity\":3}",
                "{\"result\":\"item updated\"}");
        var readUpdated = exchange(HttpMethod.GET.value(), ShopApiRoutes.cart(cartId),
                ShopApiHttpStatus.OK, "",
                "{\"cart_items\":[{\"product_id\":\"product-live-1\",\"quantity\":3}]}");
        var remove = exchange(HttpMethod.DELETE.value(), removePath,
                ShopApiHttpStatus.NO_CONTENT, "", "");
        var readEmpty = exchange(HttpMethod.GET.value(), ShopApiRoutes.cart(cartId),
                ShopApiHttpStatus.OK, "", "{\"cart_items\":[]}");

        var passing = ShopApiAssertions.cartQuantityUpdateAndRemoval(
                update, readUpdated, remove, readEmpty, cartId, productId, 3);
        var notClean = ShopApiAssertions.cartQuantityUpdateAndRemoval(
                update, readUpdated, remove, readUpdated, cartId, productId, 3);

        assertTrue(passing.stream().allMatch(a -> a.passed()));
        assertFalse(notClean.stream().filter(a -> a.name().equals(
                "API state · cart empty after removal")).findFirst().orElseThrow().passed());
    }

    private static ShopApiNetworkJournal.Exchange categoryExchange(String categoryId, String data) {
        String url = ShopApiScenarioFixture.API_ENDPOINT + ShopApiRoutes.PRODUCT_COLLECTION
                + "?" + ShopApiQueryParameter.CATEGORY.wireValue() + "=" + categoryId
                + "&" + ShopApiQueryParameter.SORT.wireValue() + "="
                + ShopProductSort.PRICE_ASC.wireValue().replace(",", "%2C");
        return new ShopApiNetworkJournal.Exchange(HttpMethod.GET.value(),
                ShopApiRoutes.PRODUCT_COLLECTION, url, ShopApiHttpStatus.OK.code(), "",
                "{\"data\":" + data + "}", Map.of(), HttpHeaderProfiles.JSON_CONTENT);
    }

    private static ShopApiNetworkJournal.Exchange exchange(String method, String path,
                                                                     ShopApiHttpStatus status,
                                                                     String requestBody, String responseBody) {
        return exchange(method, path, status, requestBody, responseBody,
                HttpHeaderProfiles.JSON_CONTENT);
    }

    private static ShopApiNetworkJournal.Exchange exchange(String method, String path,
                                                                     ShopApiHttpStatus status,
                                                                     String requestBody, String responseBody,
                                                                     Map<String, String> responseHeaders) {
        return new ShopApiNetworkJournal.Exchange(method, path,
                ShopApiScenarioFixture.API_ENDPOINT + path, status.code(),
                requestBody, responseBody, Map.of(), responseHeaders);
    }
}
