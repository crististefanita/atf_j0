package atf.shop.tests.api.integration.support;

import atf.api.core.http.response.ApiExecutionResult;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.support.ShopJsonEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Owns one remote cart and guarantees that its configured product is removed on close. */
public final class ShopApiLiveCartScope implements AutoCloseable {
    private final ShopApiClient client;
    private final String productId;
    private final List<ApiExecutionResult> executions = new ArrayList<>();
    private List<ApiExecutionResult> cleanupExecutions = List.of();
    private String cartId = "";
    private boolean closed;

    public ShopApiLiveCartScope(ShopApiClient client, String productId) {
        this.client = Objects.requireNonNull(client, "client");
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("product id must not be blank");
        }
        this.productId = productId;
    }

    public ApiExecutionResult create() {
        ApiExecutionResult result = execute(client.requests().createCart());
        if (!ShopApiHttpStatus.CREATED.matches(result.response().status())) {
            throw new IllegalStateException("Live cart creation did not return HTTP 201");
        }
        var body = ShopJsonEvidence.parse(result.response().body());
        cartId = body == null ? "" : body.path(ShopApiJsonField.ID.wireValue()).asText("");
        if (cartId.isBlank()) throw new IllegalStateException("Live cart creation returned no cart id");
        return result;
    }

    public ApiExecutionResult add(int quantity) {
        return execute(client.requests().addToCart(requiredCartId(), productId, quantity));
    }

    public ApiExecutionResult read() {
        return execute(client.requests().readCart(requiredCartId()));
    }

    public ApiExecutionResult updateQuantity(int quantity) {
        return execute(client.requests().updateCartQuantity(requiredCartId(), productId, quantity));
    }

    public ApiExecutionResult remove() {
        return execute(client.requests().removeFromCart(requiredCartId(), productId));
    }

    public String cartId() {
        return requiredCartId();
    }

    public List<ApiExecutionResult> executions() {
        return List.copyOf(executions);
    }

    public List<ApiExecutionResult> cleanupExecutions() {
        return cleanupExecutions;
    }

    @Override
    public void close() {
        if (closed) return;
        int cleanupStart = executions.size();
        try {
            if (cartId.isBlank()) return;

            var current = read();
            if (containsProduct(current.response().body())) {
                var removal = remove();
                if (!ShopApiHttpStatus.NO_CONTENT.matches(removal.response().status())) {
                    throw new IllegalStateException("Live cart cleanup did not return HTTP 204");
                }
            }

            var finalState = read();
            var finalBody = ShopJsonEvidence.parse(finalState.response().body());
            var cartItems = finalBody == null ? null
                    : finalBody.path(ShopApiJsonField.CART_ITEMS.wireValue());
            if (!ShopApiHttpStatus.OK.matches(finalState.response().status())
                    || cartItems == null || !cartItems.isArray() || !cartItems.isEmpty()) {
                throw new IllegalStateException("Live cart cleanup could not verify an empty cart");
            }
        } finally {
            cleanupExecutions = List.copyOf(executions.subList(cleanupStart, executions.size()));
            closed = true;
        }
    }

    private ApiExecutionResult execute(atf.api.core.http.request.ApiRequest request) {
        if (closed) throw new IllegalStateException("Live cart scope is closed");
        ApiExecutionResult result = client.execute(request);
        executions.add(result);
        return result;
    }

    private String requiredCartId() {
        if (cartId.isBlank()) throw new IllegalStateException("Live cart scope has not created a cart");
        return cartId;
    }

    private boolean containsProduct(String body) {
        var root = ShopJsonEvidence.parse(body);
        var items = root == null ? null : root.path(ShopApiJsonField.CART_ITEMS.wireValue());
        if (items == null || !items.isArray()) {
            throw new IllegalStateException("Live cart read returned no usable cart_items array");
        }
        for (var item : items) {
            if (productId.equals(item.path(ShopApiJsonField.PRODUCT_ID.wireValue()).asText())) {
                return true;
            }
        }
        return false;
    }
}
