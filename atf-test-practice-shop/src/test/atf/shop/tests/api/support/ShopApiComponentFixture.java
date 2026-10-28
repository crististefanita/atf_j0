package atf.shop.tests.api.support;

import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpMethod;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.ApiTransportProvenance;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.ShopApiRoutes;
import atf.shop.support.ShopJsonEvidence;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Deterministic component boundary; live integration remains a separate test level. */
public final class ShopApiComponentFixture {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ShopApiComponentFixture() { }

    public static ShopApiClient client() {
        return ShopApiClient.forTransport(
                ShopApiScenarioFixture.API_ENDPOINT,
                recordedTransport(transport(), ApiTransportProvenance.ACCEPTANCE_MOCK));
    }

    /**
     * Returns a deterministic stateful cart boundary for read-back contract tests.
     * The ordinary fixture remains stateless so isolated request-variant tests do
     * not accidentally depend on execution order.
     */
    public static ShopApiClient statefulCartClient() {
        return ShopApiClient.forTransport(
                ShopApiScenarioFixture.API_ENDPOINT,
                recordedTransport(new StatefulCartTransport(), ApiTransportProvenance.ACCEPTANCE_MOCK));
    }

    /** Fresh cart state, composable with the ordinary endpoint fixture in run-lifecycle proofs. */
    public static ApiTransport statefulCartTransport() { return new StatefulCartTransport(); }

    /** Wraps a deterministic test transport so API-call audit suites retain its actual exchanges. */
    public static ApiTransport recordedTransport(ApiTransport delegate,
                                                 ApiTransportProvenance provenance) {
        if (!ShopApiCallAuditContextTestSupport.isRecording()) return delegate;
        return (request, url, auth) -> {
            long startedAt = System.nanoTime();
            return delegate.send(request, url, auth).thenApply(response -> {
                ShopApiCallAuditContextTestSupport.capture(new ApiExecutionResult(response, 1,
                        Duration.ofNanos(System.nanoTime() - startedAt), List.of(), List.of(), request,
                        provenance));
                return response;
            });
        };
    }

    public static ApiTransport transport() {
        return (request, url, auth) -> CompletableFuture.completedFuture(
                response(request, url, auth));
    }

    private static ApiResponse response(ApiRequest request, String url, String auth) {
        if (is(request, HttpMethod.POST, ShopApiRoutes.LOGIN)) {
            var requestBody = ShopJsonEvidence.parse(request.bodyText());
            boolean invalid = requestBody != null
                    && ShopApiScenarioFixture.INVALID_EMAIL.equals(
                    requestBody.path(ShopApiJsonField.EMAIL.wireValue()).asText());
            return json(invalid ? ShopApiHttpStatus.UNAUTHORIZED
                            : ShopApiHttpStatus.OK,
                    invalid ? ShopApiScenarioFixture.UNAUTHORIZED_BODY
                            : ShopApiScenarioFixture.LOGIN_TOKEN_BODY, url);
        }
        if (is(request, HttpMethod.POST, ShopApiRoutes.USER_REGISTER)) {
            boolean invalid = hasBlankField(request, ShopApiJsonField.FIRST_NAME,
                            ShopApiJsonField.LAST_NAME, ShopApiJsonField.EMAIL,
                            ShopApiJsonField.PASSWORD);
            return json(invalid ? ShopApiHttpStatus.UNPROCESSABLE_ENTITY
                            : ShopApiHttpStatus.CREATED,
                    invalid ? ShopApiScenarioFixture.UNPROCESSABLE_BODY
                            : ShopApiScenarioFixture.REGISTERED_USER_BODY, url);
        }
        if (is(request, HttpMethod.POST, ShopApiRoutes.INVOICE_CREATE)) {
            boolean invalid = hasBlankField(request, ShopApiJsonField.BILLING_STREET,
                    ShopApiJsonField.BILLING_CITY, ShopApiJsonField.BILLING_STATE,
                    ShopApiJsonField.BILLING_COUNTRY, ShopApiJsonField.BILLING_POSTAL_CODE,
                    ShopApiJsonField.PAYMENT_METHOD, ShopApiJsonField.CART_ID);
            return json(invalid ? ShopApiHttpStatus.UNPROCESSABLE_ENTITY : ShopApiHttpStatus.OK,
                    invalid ? ShopApiScenarioFixture.UNPROCESSABLE_BODY
                            : ShopApiScenarioFixture.CREATED_INVOICE_BODY, url);
        }
        if (is(request, HttpMethod.POST, ShopApiRoutes.GUEST_INVOICE_CREATE)) {
            boolean invalid = hasBlankField(request, ShopApiJsonField.BILLING_STREET,
                    ShopApiJsonField.BILLING_CITY, ShopApiJsonField.BILLING_STATE,
                    ShopApiJsonField.BILLING_COUNTRY, ShopApiJsonField.BILLING_POSTAL_CODE,
                    ShopApiJsonField.PAYMENT_METHOD, ShopApiJsonField.CART_ID,
                    ShopApiJsonField.GUEST_EMAIL, ShopApiJsonField.GUEST_FIRST_NAME,
                    ShopApiJsonField.GUEST_LAST_NAME);
            return json(invalid ? ShopApiHttpStatus.UNPROCESSABLE_ENTITY : ShopApiHttpStatus.OK,
                    invalid ? ShopApiScenarioFixture.UNPROCESSABLE_BODY
                            : ShopApiScenarioFixture.CREATED_INVOICE_BODY, url);
        }
        if (is(request, ShopApiOperation.SEARCH, ShopApiRoutes.PRODUCT_SEARCH)) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.PRODUCT_SEARCH_BODY, url);
        }
        if (is(request, HttpMethod.GET, ShopApiRoutes.PRODUCT_COLLECTION)) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.PRODUCT_COLLECTION_BODY, url);
        }
        if (is(request, HttpMethod.GET, ShopApiRoutes.CATEGORIES)) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.CATEGORIES_BODY, url);
        }
        if (is(request, HttpMethod.GET,
                ShopApiRoutes.product(ShopApiScenarioFixture.OUT_OF_STOCK_PRODUCT_ID))) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.OUT_OF_STOCK_BODY, url);
        }
        if (is(request, HttpMethod.GET,
                ShopApiRoutes.product(ShopApiScenarioFixture.MISSING_PRODUCT_ID))) {
            return json(ShopApiHttpStatus.NOT_FOUND, ShopApiScenarioFixture.NOT_FOUND_BODY, url);
        }
        if (is(request, HttpMethod.GET,
                ShopApiRoutes.product(ShopApiScenarioFixture.PRODUCT_ID))) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.PRODUCT_BODY, url);
        }
        if (is(request, HttpMethod.POST, ShopApiRoutes.CARTS)) {
            return json(ShopApiHttpStatus.CREATED, ShopApiScenarioFixture.CART_BODY, url);
        }
        if (is(request, HttpMethod.GET,
                ShopApiRoutes.cart(ShopApiScenarioFixture.CART_ID))) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.CART_ITEM_BODY, url);
        }
        if (is(request, HttpMethod.POST,
                ShopApiRoutes.cart(ShopApiScenarioFixture.CART_ID))) {
            if (hasInvalidProduct(request)) {
                return json(ShopApiHttpStatus.UNPROCESSABLE_ENTITY,
                        ShopApiScenarioFixture.UNPROCESSABLE_BODY, url);
            }
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.CART_MUTATION_BODY, url);
        }
        if (is(request, HttpMethod.PUT,
                ShopApiRoutes.updateCartQuantity(ShopApiScenarioFixture.MUTATION_CART_ID))) {
            if (hasInvalidProduct(request)) {
                return json(ShopApiHttpStatus.UNPROCESSABLE_ENTITY,
                        ShopApiScenarioFixture.UNPROCESSABLE_BODY, url);
            }
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.CART_UPDATE_BODY, url);
        }
        if (is(request, HttpMethod.DELETE,
                ShopApiRoutes.removeCartProduct(ShopApiScenarioFixture.MUTATION_CART_ID,
                        ShopApiScenarioFixture.MUTATION_PRODUCT_ID))) {
            return response(ShopApiHttpStatus.NO_CONTENT, "", Map.of(), url);
        }
        if (is(request, HttpMethod.GET, ShopApiRoutes.POSTCODE_LOOKUP)) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.ADDRESS_BODY, url);
        }
        if (is(request, HttpMethod.POST, ShopApiRoutes.PAYMENT_CHECK)) {
            return json(ShopApiHttpStatus.OK, ShopApiScenarioFixture.PAYMENT_SUCCESS_BODY, url);
        }
        return json(ShopApiHttpStatus.INTERNAL_SERVER_ERROR,
                ShopApiScenarioFixture.UNEXPECTED_ROUTE_BODY, url);
    }

    private static boolean is(ApiRequest request, HttpMethod method, String path) {
        return method.value().equals(request.method()) && path.equals(request.path());
    }

    private static boolean is(ApiRequest request, ShopApiOperation operation, String path) {
        return operation.wireValue().equals(request.method()) && path.equals(request.path());
    }

    private static boolean hasInvalidProduct(ApiRequest request) {
        var body = ShopJsonEvidence.parse(request.bodyText());
        return body != null && ShopApiScenarioFixture.INVALID_PRODUCT_ID.equals(
                body.path(ShopApiJsonField.PRODUCT_ID.wireValue()).asText());
    }

    private static boolean hasBlankField(ApiRequest request, ShopApiJsonField... fields) {
        var body = ShopJsonEvidence.parse(request.bodyText());
        if (body == null) return true;
        for (ShopApiJsonField field : fields) {
            if (body.path(field.wireValue()).asText().isBlank()) return true;
        }
        return false;
    }

    private static ApiResponse json(ShopApiHttpStatus status, String body, String url) {
        return response(status, body, HttpHeaderProfiles.JSON_CONTENT, url);
    }

    private static ApiResponse response(ShopApiHttpStatus status, String body,
                                        Map<String, String> headers, String url) {
        return new ApiResponse(status.code(), body, headers, Duration.ZERO, url);
    }

    private static final class StatefulCartTransport implements ApiTransport {
        private final Map<String, CartLine> carts = new HashMap<>();

        @Override
        public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String auth) {
            return CompletableFuture.completedFuture(response(request, url));
        }

        private ApiResponse response(ApiRequest request, String url) {
            if (is(request, HttpMethod.POST, ShopApiRoutes.CARTS)) {
                String cartId = ShopApiScenarioFixture.MUTATION_CART_ID;
                carts.remove(cartId);
                return json(ShopApiHttpStatus.CREATED,
                        "{\"id\":\"" + cartId + "\"}", url);
            }

            String cartId = cartId(request.path());
            if (cartId == null) {
                return json(ShopApiHttpStatus.INTERNAL_SERVER_ERROR,
                        ShopApiScenarioFixture.UNEXPECTED_ROUTE_BODY, url);
            }

            if (request.method().equals(HttpMethod.POST.value())) {
                CartLine line = line(request);
                carts.put(cartId, line);
                return json(ShopApiHttpStatus.OK, mutation("item added or updated"), url);
            }
            if (request.method().equals(HttpMethod.PUT.value())) {
                CartLine line = line(request);
                carts.put(cartId, line);
                return json(ShopApiHttpStatus.OK, mutation("item updated"), url);
            }
            if (request.method().equals(HttpMethod.DELETE.value())) {
                carts.remove(cartId);
                return ShopApiComponentFixture.response(
                        ShopApiHttpStatus.NO_CONTENT, "", Map.<String, String>of(), url);
            }
            if (request.method().equals(HttpMethod.GET.value())) {
                return json(ShopApiHttpStatus.OK, cartBody(carts.get(cartId)), url);
            }
            return json(ShopApiHttpStatus.INTERNAL_SERVER_ERROR,
                    ShopApiScenarioFixture.UNEXPECTED_ROUTE_BODY, url);
        }

        private static String cartId(String path) {
            String prefix = ShopApiRoutes.CARTS + "/";
            if (!path.startsWith(prefix)) return null;
            String remainder = path.substring(prefix.length());
            int slash = remainder.indexOf('/');
            return slash < 0 ? remainder : remainder.substring(0, slash);
        }

        private static CartLine line(ApiRequest request) {
            var body = ShopJsonEvidence.parse(request.bodyText());
            return new CartLine(body.path(ShopApiJsonField.PRODUCT_ID.wireValue()).asText(),
                    body.path(ShopApiJsonField.QUANTITY.wireValue()).asInt());
        }

        private static String cartBody(CartLine line) {
            try {
                var root = JSON.createObjectNode();
                var items = root.putArray(ShopApiJsonField.CART_ITEMS.wireValue());
                if (line != null) {
                    var item = items.addObject();
                    item.put(ShopApiJsonField.PRODUCT_ID.wireValue(), line.productId());
                    item.put(ShopApiJsonField.QUANTITY.wireValue(), line.quantity());
                }
                return JSON.writeValueAsString(root);
            } catch (Exception error) {
                throw new IllegalStateException("Cannot serialize stateful cart fixture", error);
            }
        }

        private static String mutation(String result) {
            return "{\"result\":\"" + result + "\"}";
        }

        private record CartLine(String productId, int quantity) { }
    }
}
