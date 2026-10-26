package atf.shop.api.screen;

import atf.api.core.http.response.ApiExecutionResult;
import atf.shop.api.ShopApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cart-screen API boundary: create → add → read. */
public final class ShopCartApi {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ShopApiClient client;

    public ShopCartApi(ShopApiClient client) { this.client = client; }

    public CartScreen prepare(String productId, int quantity) {
        ApiExecutionResult create = client.execute(client.requests().createCart());
        var createResponse = create.response();
        String cartId;
        try {
            cartId = JSON.readTree(createResponse.body()).path("id").asText("");
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("Cart create response is not usable JSON", error);
        }
        if (cartId.isBlank()) throw new IllegalStateException("Cart create did not return an id");
        ApiExecutionResult add = client.execute(client.requests().addToCart(cartId, productId, quantity));
        ApiExecutionResult read = client.execute(client.requests().readCart(cartId));
        return new CartScreen(cartId, productId, quantity, create, add, read);
    }

    public ApiExecutionResult updateQuantity(String cartId, String productId, int quantity) {
        return client.execute(client.requests().updateCartQuantity(cartId, productId, quantity));
    }

    public ApiExecutionResult removeProduct(String cartId, String productId) {
        return client.execute(client.requests().removeFromCart(cartId, productId));
    }

    public record CartScreen(String cartId, String productId, int quantity,
                             ApiExecutionResult create, ApiExecutionResult add,
                             ApiExecutionResult read) { }
}
