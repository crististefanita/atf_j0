package atf.shop.api.screen;

import atf.api.core.http.response.ApiExecutionResult;
import atf.shop.api.ShopApiClient;
import atf.shop.api.ShopProductQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Catalog-screen API boundary: search is the source of truth for the product id. */
public final class ShopCatalogApi {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ShopApiClient client;

    public ShopCatalogApi(ShopApiClient client) { this.client = client; }

    public CatalogScreen openProduct(String productName) {
        ApiExecutionResult search = client.execute(client.requests().search(productName));
        var searchResponse = search.response();
        String productId = productId(searchResponse.body(), productName);
        ApiExecutionResult detail = client.execute(client.requests().product(productId, client.bearerToken()));
        return new CatalogScreen(productName, productId, search, detail);
    }

    public ApiExecutionResult listProducts(ShopProductQuery filters) {
        return client.execute(client.requests().products(filters));
    }

    public ApiExecutionResult listCategories() {
        return client.execute(client.requests().categories());
    }

    private static String productId(String body, String productName) {
        try {
            JsonNode products = JSON.readTree(body).path("data");
            for (JsonNode product : products) {
                if (productName.equalsIgnoreCase(product.path("name").asText())) {
                    String id = product.path("id").asText();
                    if (!id.isBlank()) return id;
                }
            }
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Catalog search response is not usable JSON", error);
        }
        throw new IllegalStateException("Catalog search did not return product: " + productName);
    }

    public record CatalogScreen(String productName, String productId,
                                ApiExecutionResult search, ApiExecutionResult detail) { }
}
