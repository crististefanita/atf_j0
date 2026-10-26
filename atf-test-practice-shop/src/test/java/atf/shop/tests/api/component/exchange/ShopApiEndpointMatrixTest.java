package atf.shop.tests.api.component.exchange;

import atf.api.core.http.request.ApiRequest;
import atf.api.core.http.request.HttpHeader;
import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.request.MediaTypes;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.ShopApiRequestFactory;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopApiCallComponentTest;
import atf.shop.tests.api.support.ShopApiCallAuditContextTestSupport;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Audited endpoint/status matrix against deterministic component transports. */
@ShopApiCallComponentTest
class ShopApiEndpointMatrixTest {
    private static final ShopApiRequestFactory REQUESTS = new ShopApiRequestFactory();

    @Test
    void endpointCasesUseConciseContractDisplayNames() {
        assertTrue(endpointCases().allMatch(scenario -> scenario.toString().equals(scenario.name())
                        && !scenario.toString().contains("method=")
                        && !scenario.toString().contains("requestSupplier=")),
                "parameterized report titles must show the business case, not record implementation fields");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointCases")
    void endpointExchangeMatchesItsMethodRouteStatusAndJsonContract(
            ShopApiEndpointCase scenario) {
        ApiRequest request = scenario.requestSupplier().create();
        ShopApiCallAuditContextTestSupport.expectNext(scenario.name(), scenario.status());
        var execution = ShopApiComponentFixture.client().execute(request);
        var response = execution.response();

        assertEquals(scenario.method(), request.method(), scenario.name());
        assertEquals(scenario.path(), request.path(), scenario.name());
        assertTrue(scenario.status().matches(response.status()), scenario.name());
        assertTrue(execution.attempts() > 0, scenario.name());

        if (HttpMethod.POST.value().equals(scenario.method())
                || HttpMethod.PUT.value().equals(scenario.method())) {
            assertEquals(MediaTypes.APPLICATION_JSON,
                    request.headers().get(HttpHeader.CONTENT_TYPE.value()), scenario.name());
            assertNotNull(ShopJsonEvidence.parse(request.bodyText()), scenario.name());
        }
        if (ShopApiHttpStatus.NO_CONTENT.matches(response.status())) {
            assertTrue(response.body().isEmpty(), scenario.name());
        } else {
            assertTrue(response.header(HttpHeader.CONTENT_TYPE.value()).contains(MediaTypes.APPLICATION_JSON),
                    scenario.name());
            assertNotNull(ShopJsonEvidence.parse(response.body()), scenario.name());
        }
    }

    private static Stream<ShopApiEndpointCase> endpointCases() {
        String cartId = ShopApiScenarioFixture.CART_ID;
        String mutationCartId = ShopApiScenarioFixture.MUTATION_CART_ID;
        String productId = ShopApiScenarioFixture.PRODUCT_ID;
        String mutationProductId = ShopApiScenarioFixture.MUTATION_PRODUCT_ID;

        return Stream.of(
                endpoint("login endpoint returns HTTP 200 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.LOGIN, ShopApiHttpStatus.OK,
                        () -> REQUESTS.login(ShopApiScenarioFixture.validCredentials())),
                endpoint("login endpoint returns HTTP 401 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.LOGIN, ShopApiHttpStatus.UNAUTHORIZED,
                        () -> REQUESTS.login(ShopApiScenarioFixture.invalidCredentials())),
                endpoint("registration endpoint returns HTTP 201 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.USER_REGISTER, ShopApiHttpStatus.CREATED,
                        () -> REQUESTS.register(ShopApiScenarioFixture.validCredentials())),
                endpoint("product search endpoint returns HTTP 200 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.PRODUCT_SEARCH, ShopApiHttpStatus.OK,
                        () -> REQUESTS.search(ShopApiScenarioFixture.PRODUCT_NAME)),
                endpoint("product collection endpoint returns HTTP 200 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.PRODUCT_COLLECTION, ShopApiHttpStatus.OK,
                        () -> REQUESTS.products(null)),
                endpoint("categories endpoint returns HTTP 200 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.CATEGORIES, ShopApiHttpStatus.OK, REQUESTS::categories),
                endpoint("product detail endpoint returns HTTP 200 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.product(productId), ShopApiHttpStatus.OK,
                        () -> REQUESTS.product(productId, "")),
                endpoint("unknown product endpoint returns HTTP 404 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.product(ShopApiScenarioFixture.MISSING_PRODUCT_ID),
                        ShopApiHttpStatus.NOT_FOUND,
                        () -> REQUESTS.product(ShopApiScenarioFixture.MISSING_PRODUCT_ID, "")),
                endpoint("cart creation endpoint returns HTTP 201 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.CARTS, ShopApiHttpStatus.CREATED, REQUESTS::createCart),
                endpoint("cart read endpoint returns HTTP 200 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.cart(cartId), ShopApiHttpStatus.OK,
                        () -> REQUESTS.readCart(cartId)),
                endpoint("cart add endpoint returns HTTP 200 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.cart(cartId), ShopApiHttpStatus.OK,
                        () -> REQUESTS.addToCart(cartId, productId, ShopApiScenarioFixture.QUANTITY)),
                endpoint("invalid cart add endpoint returns HTTP 422 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.cart(cartId), ShopApiHttpStatus.UNPROCESSABLE_ENTITY,
                        () -> REQUESTS.addToCart(cartId, ShopApiScenarioFixture.INVALID_PRODUCT_ID,
                                ShopApiScenarioFixture.QUANTITY)),
                endpoint("cart update endpoint returns HTTP 200 JSON", HttpMethod.PUT.value(),
                        ShopApiRoutes.updateCartQuantity(mutationCartId), ShopApiHttpStatus.OK,
                        () -> REQUESTS.updateCartQuantity(mutationCartId, mutationProductId,
                                ShopApiScenarioFixture.UPDATED_QUANTITY)),
                endpoint("invalid cart update endpoint returns HTTP 422 JSON", HttpMethod.PUT.value(),
                        ShopApiRoutes.updateCartQuantity(mutationCartId),
                        ShopApiHttpStatus.UNPROCESSABLE_ENTITY,
                        () -> REQUESTS.updateCartQuantity(mutationCartId,
                                ShopApiScenarioFixture.INVALID_PRODUCT_ID,
                                ShopApiScenarioFixture.UPDATED_QUANTITY)),
                endpoint("cart removal endpoint returns HTTP 204 without a body", HttpMethod.DELETE.value(),
                        ShopApiRoutes.removeCartProduct(mutationCartId, mutationProductId),
                        ShopApiHttpStatus.NO_CONTENT,
                        () -> REQUESTS.removeFromCart(mutationCartId, mutationProductId)),
                endpoint("postcode endpoint returns HTTP 200 JSON", HttpMethod.GET.value(),
                        ShopApiRoutes.POSTCODE_LOOKUP, ShopApiHttpStatus.OK,
                        () -> REQUESTS.postcodeLookup(ShopApiScenarioFixture.COUNTRY,
                                ShopApiScenarioFixture.POSTCODE, ShopApiScenarioFixture.HOUSE_NUMBER)),
                endpoint("payment endpoint returns HTTP 200 JSON", HttpMethod.POST.value(),
                        ShopApiRoutes.PAYMENT_CHECK, ShopApiHttpStatus.OK,
                        () -> REQUESTS.payment(ShopPaymentMethod.CASH_ON_DELIVERY)),
                endpoint("authenticated invoice endpoint returns HTTP 200 JSON",
                        HttpMethod.POST.value(), ShopApiRoutes.INVOICE_CREATE,
                        ShopApiHttpStatus.OK,
                        () -> REQUESTS.invoice(ShopApiScenarioFixture.validInvoice(),
                                ShopApiScenarioFixture.BEARER_TOKEN)),
                endpoint("guest invoice endpoint returns HTTP 200 JSON",
                        HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE,
                        ShopApiHttpStatus.OK,
                        () -> REQUESTS.guestInvoice(ShopApiScenarioFixture.validGuestInvoice())),
                endpoint("invalid guest invoice endpoint returns HTTP 422 JSON",
                        HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE,
                        ShopApiHttpStatus.UNPROCESSABLE_ENTITY,
                        () -> REQUESTS.guestInvoice(ShopApiScenarioFixture.invalidGuestInvoice()))
        );
    }

    private static ShopApiEndpointCase endpoint(String name, String method, String path,
                                                ShopApiHttpStatus status,
                                                ShopApiRequestSupplier request) {
        return new ShopApiEndpointCase(name, method, path, status, request);
    }

    private record ShopApiEndpointCase(String name, String method, String path,
                                       ShopApiHttpStatus status,
                                       ShopApiRequestSupplier requestSupplier) {
        @Override
        public String toString() {
            return name;
        }
    }

    @FunctionalInterface
    private interface ShopApiRequestSupplier {
        ApiRequest create();
    }
}
