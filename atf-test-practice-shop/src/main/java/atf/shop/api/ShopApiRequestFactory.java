package atf.shop.api;

import atf.api.core.http.request.ApiRequest;
import atf.api.core.http.request.HttpHeader;
import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.auth.AuthProfiles;
import atf.api.core.http.auth.AuthType;
import atf.api.core.http.protocol.HttpHeaderProfiles;
import atf.shop.api.contract.ShopApiQueryParameter;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.config.model.ShopCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure request factory; screen APIs own the order in which requests run. */
public final class ShopApiRequestFactory {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NO_AUTH = AuthProfiles.NONE;
    private static final String BEARER_AUTH = AuthProfiles.standard(AuthType.BEARER);
    private static final String BEARER_PREFIX = "Bearer ";

    public ApiRequest login(ShopCredentials credentials) {
        return json(HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                new ShopApiRequestBodies.ShopLoginRequest(credentials.email(), credentials.password()),
                NO_AUTH);
    }

    public ApiRequest register(ShopCredentials credentials) {
        return json(HttpMethod.POST.value(), ShopApiRoutes.USER_REGISTER,
                new ShopApiRequestBodies.ShopRegistrationRequest(credentials.firstName(),
                        credentials.lastName(), credentials.email(), credentials.password()), NO_AUTH);
    }

    public ApiRequest search(String productName) {
        return query(HttpMethod.GET.value(), ShopApiRoutes.PRODUCT_SEARCH,
                Map.of(ShopApiQueryParameter.SEARCH.wireValue(), required(productName, "product name")),
                NO_AUTH);
    }

    public ApiRequest products(ShopProductQuery filters) {
        return query(HttpMethod.GET.value(), ShopApiRoutes.PRODUCT_COLLECTION,
                filters == null ? Map.of() : filters.asQuery(),
                NO_AUTH);
    }

    public ApiRequest categories() {
        return get(ShopApiRoutes.CATEGORIES, "");
    }

    public ApiRequest product(String productId, String bearerToken) {
        return get(ShopApiRoutes.product(productId), bearerToken);
    }

    public ApiRequest createCart() {
        return json(HttpMethod.POST.value(), ShopApiRoutes.CARTS, Map.of(),
                NO_AUTH);
    }

    public ApiRequest addToCart(String cartId, String productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("cart quantity must be positive");
        return json(HttpMethod.POST.value(), ShopApiRoutes.cart(cartId),
                new ShopApiRequestBodies.ShopCartItemRequest(
                        required(productId, "product id"), quantity),
                NO_AUTH);
    }

    public ApiRequest updateCartQuantity(String cartId, String productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("cart quantity must be positive");
        return json(HttpMethod.PUT.value(), ShopApiRoutes.updateCartQuantity(cartId),
                new ShopApiRequestBodies.ShopCartItemRequest(
                        required(productId, "product id"), quantity),
                NO_AUTH);
    }

    public ApiRequest removeFromCart(String cartId, String productId) {
        return new ApiRequest(HttpRequest.BodyPublishers.noBody(), HttpMethod.DELETE.value(),
                ShopApiRoutes.removeCartProduct(cartId, productId), Map.of(), Map.of(),
                NO_AUTH, null, "");
    }

    public ApiRequest readCart(String cartId) {
        return get(ShopApiRoutes.cart(cartId), "");
    }

    public ApiRequest postcodeLookup(String country, String postcode, String houseNumber) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put(ShopApiQueryParameter.COUNTRY.wireValue(), required(country, "country"));
        query.put(ShopApiQueryParameter.POSTCODE.wireValue(), required(postcode, "postcode"));
        query.put(ShopApiQueryParameter.HOUSE_NUMBER.wireValue(), required(houseNumber, "house number"));
        return query(HttpMethod.GET.value(), ShopApiRoutes.POSTCODE_LOOKUP, query,
                NO_AUTH);
    }

    public ApiRequest payment(ShopPaymentMethod method) {
        return json(HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK,
                new ShopApiRequestBodies.ShopPaymentCheckRequest(method.wireValue(), Map.of()),
                NO_AUTH);
    }

    public ApiRequest invoice(ShopApiRequestBodies.ShopInvoiceRequest invoice, String bearerToken) {
        return json(HttpMethod.POST.value(), ShopApiRoutes.INVOICE_CREATE, invoice, BEARER_AUTH,
                Map.of(HttpHeader.AUTHORIZATION.value(),
                        BEARER_PREFIX + required(bearerToken, "bearer token")));
    }

    public ApiRequest guestInvoice(ShopApiRequestBodies.ShopInvoiceRequest invoice) {
        return json(HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE, invoice, NO_AUTH);
    }

    private static ApiRequest get(String path, String bearerToken) {
        Map<String, String> headers = bearerToken == null || bearerToken.isBlank()
                ? Map.of() : Map.of(HttpHeader.AUTHORIZATION.value(), BEARER_PREFIX + bearerToken);
        return new ApiRequest(HttpRequest.BodyPublishers.noBody(), HttpMethod.GET.value(), path, Map.of(), headers,
                bearerToken == null || bearerToken.isBlank()
                        ? NO_AUTH : BEARER_AUTH, null, "");
    }

    private static ApiRequest query(String method, String path, Map<String, String> query,
                                    String authProfile) {
        return new ApiRequest(HttpRequest.BodyPublishers.noBody(), method, path, query,
                HttpHeaderProfiles.JSON_REQUEST, authProfile, null, "");
    }

    private static ApiRequest json(String method, String path, Object body, String authProfile) {
        return json(method, path, body, authProfile, Map.of());
    }

    private static ApiRequest json(String method, String path, Object body, String authProfile,
                                   Map<String, String> additionalHeaders) {
        String bodyText;
        try {
            bodyText = JSON.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Cannot serialize Practice Shop API JSON body", error);
        }
        Map<String, String> headers = new LinkedHashMap<>(HttpHeaderProfiles.JSON_REQUEST);
        headers.putAll(additionalHeaders);
        return new ApiRequest(HttpRequest.BodyPublishers.ofString(bodyText), method, path,
                Map.of(), headers, authProfile, null, bodyText);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
