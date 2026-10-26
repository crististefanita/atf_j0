package atf.shop.tests.api.component.exchange;

import atf.api.core.http.request.HttpMethod;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.ShopApiRequestFactory;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.ShopProductQuery;
import atf.shop.api.contract.ShopApiQueryParameter;
import atf.shop.api.contract.ShopProductSort;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiCallComponentTest;
import atf.shop.tests.api.support.ShopApiCallAuditContextTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component matrix for catalog variation and cart mutation boundaries. */
@ShopApiCallComponentTest
class ShopApiCatalogCartVariantTest {
    @Test
    void categoryQueryAndOutOfStockRemainObservableWithoutASecondE2e() {
        ShopApiClient client = ShopApiComponentFixture.client();
        ShopApiRequestFactory requests = client.requests();

        String categoryId = ShopApiScenarioFixture.CATEGORY_ID;
        String outOfStockProductId = ShopApiScenarioFixture.OUT_OF_STOCK_PRODUCT_ID;
        boolean rentalOnly = ShopApiScenarioFixture.FILTER_RENTAL;
        int page = ShopApiScenarioFixture.FIRST_PAGE;

        ShopApiCallAuditContextTestSupport.expectNext("filtered product collection", ShopApiHttpStatus.OK);
        var filteredExecution = client.execute(requests.products(
                new ShopProductQuery(null, categoryId, rentalOnly, null,
                        ShopProductSort.PRICE_ASC, page)));
        ShopApiCallAuditContextTestSupport.expectNext("category collection", ShopApiHttpStatus.OK);
        var categoriesExecution = client.execute(requests.categories());
        ShopApiCallAuditContextTestSupport.expectNext("out-of-stock product detail", ShopApiHttpStatus.OK);
        var outOfStockExecution = client.execute(requests.product(outOfStockProductId, ""));

        var filteredResponse = filteredExecution.response();
        var categoriesResponse = categoriesExecution.response();
        var outOfStockResponse = outOfStockExecution.response();
        var filteredBody = ShopJsonEvidence.parse(filteredResponse.body());
        var categoriesBody = ShopJsonEvidence.parse(categoriesResponse.body());
        var outOfStockBody = ShopJsonEvidence.parse(outOfStockResponse.body());

        assertTrue(ShopApiHttpStatus.OK.matches(filteredResponse.status()));
        assertEquals(categoryId, filteredExecution.request().query()
                .get(ShopApiQueryParameter.CATEGORY.wireValue()));
        assertEquals(Boolean.toString(rentalOnly), filteredExecution.request().query()
                .get(ShopApiQueryParameter.RENTAL.wireValue()));
        assertEquals(ShopProductSort.PRICE_ASC.wireValue(), filteredExecution.request().query()
                .get(ShopApiQueryParameter.SORT.wireValue()));
        assertEquals(Integer.toString(page), filteredExecution.request().query()
                .get(ShopApiQueryParameter.PAGE.wireValue()));
        assertNotNull(filteredBody);
        assertEquals(categoryId, filteredBody.path(ShopApiJsonField.CATEGORY.wireValue()).asText());
        var filteredProducts = filteredBody.path(ShopApiJsonField.DATA.wireValue());
        assertEquals(1, filteredProducts.size());
        assertEquals(ShopApiScenarioFixture.FILTERED_PRODUCT_ID,
                filteredProducts.get(0).path(ShopApiJsonField.ID.wireValue()).asText());
        assertFalse(filteredProducts.get(0).path(ShopApiJsonField.IN_STOCK.wireValue())
                .asBoolean(true));
        assertTrue(ShopApiHttpStatus.OK.matches(categoriesResponse.status()));
        assertNotNull(categoriesBody);
        assertTrue(categoriesBody.findValuesAsText(ShopApiJsonField.ID.wireValue()).contains(categoryId));
        assertTrue(ShopApiHttpStatus.OK.matches(outOfStockResponse.status()));
        assertNotNull(outOfStockBody);
        assertEquals(outOfStockProductId,
                outOfStockBody.path(ShopApiJsonField.ID.wireValue()).asText());
        assertFalse(outOfStockBody.path(ShopApiJsonField.IN_STOCK.wireValue()).asBoolean(true));
    }

    @Test
    void cartUpdateAndRemovalExposeDistinctApiContracts() {
        ShopApiClient client = ShopApiComponentFixture.client();
        ShopApiRequestFactory requests = client.requests();

        String cartId = ShopApiScenarioFixture.MUTATION_CART_ID;
        String productId = ShopApiScenarioFixture.MUTATION_PRODUCT_ID;
        int expectedQuantity = ShopApiScenarioFixture.UPDATED_QUANTITY;

        ShopApiCallAuditContextTestSupport.expectNext("cart quantity update", ShopApiHttpStatus.OK);
        var updateExecution = client.execute(
                requests.updateCartQuantity(cartId, productId, expectedQuantity));
        ShopApiCallAuditContextTestSupport.expectNext("cart item removal", ShopApiHttpStatus.NO_CONTENT);
        var removeExecution = client.execute(requests.removeFromCart(cartId, productId));

        var updateResponse = updateExecution.response();
        var updateRequest = updateExecution.request();
        var removeResponse = removeExecution.response();
        var removeRequest = removeExecution.request();
        var updateRequestBody = ShopJsonEvidence.parse(updateRequest.bodyText());
        var updateResponseBody = ShopJsonEvidence.parse(updateResponse.body());

        assertTrue(ShopApiHttpStatus.OK.matches(updateResponse.status()));
        assertEquals(HttpMethod.PUT.value(), updateRequest.method());
        assertEquals(ShopApiRoutes.updateCartQuantity(cartId), updateRequest.path());
        assertNotNull(updateRequestBody);
        assertEquals(productId,
                updateRequestBody.path(ShopApiJsonField.PRODUCT_ID.wireValue()).asText());
        assertEquals(expectedQuantity,
                updateRequestBody.path(ShopApiJsonField.QUANTITY.wireValue()).asInt());
        assertNotNull(updateResponseBody);
        assertEquals(ShopApiScenarioFixture.CART_UPDATE_RESULT,
                updateResponseBody.path(ShopApiJsonField.RESULT.wireValue()).asText());

        assertTrue(ShopApiHttpStatus.NO_CONTENT.matches(removeResponse.status()));
        assertEquals(HttpMethod.DELETE.value(), removeRequest.method());
        assertEquals(ShopApiRoutes.removeCartProduct(cartId, productId), removeRequest.path());
        assertEquals("", removeRequest.bodyText());
    }

    @Test
    void statefulCartLifecycleKeepsReadBackContractAcrossMutations() {
        ShopApiClient client = ShopApiComponentFixture.statefulCartClient();
        ShopApiRequestFactory requests = client.requests();
        String cartId = ShopApiScenarioFixture.MUTATION_CART_ID;
        String productId = ShopApiScenarioFixture.MUTATION_PRODUCT_ID;

        ShopApiCallAuditContextTestSupport.expectNext("cart creation", ShopApiHttpStatus.CREATED);
        var created = client.execute(requests.createCart());
        ShopApiCallAuditContextTestSupport.expectNext("cart item add", ShopApiHttpStatus.OK);
        var added = client.execute(requests.addToCart(cartId, productId,
                ShopApiScenarioFixture.QUANTITY));
        ShopApiCallAuditContextTestSupport.expectNext("cart read after add", ShopApiHttpStatus.OK);
        var afterAdd = client.execute(requests.readCart(cartId));
        ShopApiCallAuditContextTestSupport.expectNext("cart quantity update", ShopApiHttpStatus.OK);
        var updated = client.execute(requests.updateCartQuantity(cartId, productId,
                ShopApiScenarioFixture.UPDATED_QUANTITY));
        ShopApiCallAuditContextTestSupport.expectNext("cart read after update", ShopApiHttpStatus.OK);
        var afterUpdate = client.execute(requests.readCart(cartId));
        ShopApiCallAuditContextTestSupport.expectNext("cart item removal", ShopApiHttpStatus.NO_CONTENT);
        var removed = client.execute(requests.removeFromCart(cartId, productId));
        ShopApiCallAuditContextTestSupport.expectNext("cart read after removal", ShopApiHttpStatus.OK);
        var afterRemove = client.execute(requests.readCart(cartId));

        assertEquals(ShopApiHttpStatus.CREATED.code(), created.response().status());
        assertEquals(ShopApiHttpStatus.OK.code(), added.response().status());
        assertEquals(ShopApiHttpStatus.OK.code(), afterAdd.response().status());
        assertEquals(ShopApiScenarioFixture.QUANTITY,
                quantity(afterAdd.response().body()));
        assertEquals(ShopApiHttpStatus.OK.code(), updated.response().status());
        assertEquals(ShopApiHttpStatus.OK.code(), afterUpdate.response().status());
        assertEquals(ShopApiScenarioFixture.UPDATED_QUANTITY,
                quantity(afterUpdate.response().body()));
        assertEquals(ShopApiHttpStatus.NO_CONTENT.code(), removed.response().status());
        assertEquals(ShopApiHttpStatus.OK.code(), afterRemove.response().status());
        assertTrue(ShopJsonEvidence.parse(afterRemove.response().body())
                .path(ShopApiJsonField.CART_ITEMS.wireValue()).isEmpty());
    }

    private static int quantity(String body) {
        var root = ShopJsonEvidence.parse(body);
        return root.path(ShopApiJsonField.CART_ITEMS.wireValue()).get(0)
                .path(ShopApiJsonField.QUANTITY.wireValue()).asInt();
    }
}
