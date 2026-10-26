package atf.shop.tests.api.support;

import atf.core.http.protocol.ApiUris;
import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.HttpMethod;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.UiNetworkObservation;
import atf.shop.api.ShopApiRoutes;

import java.time.Duration;
import java.time.Instant;

/**
 * Consumer-owned network fixture for settlement and presentation contracts.
 * It models the same cart mutation/read-back identity without opening a real
 * browser or enabling replay against the external Practice Shop.
 */
public final class ShopUiNetworkSettlementFixture {
    public static final String API_HOST = "api.practicesoftwaretesting.com";
    public static final String CART_ID = ShopApiScenarioFixture.MUTATION_CART_ID;
    public static final String PRODUCT_ID = ShopApiScenarioFixture.MUTATION_PRODUCT_ID;
    public static final String CART_PATH = ShopApiRoutes.cart(CART_ID);
    public static final String MUTATION_PATH = CART_PATH + "/products";

    private ShopUiNetworkSettlementFixture() { }

    public static UiNetworkInspector inspector() {
        return new UiNetworkInspector(new UiNetworkInspectionConfiguration(true,
                java.util.Set.of(API_HOST), 16, 2_000, true,
                UiNetworkSettlementPolicy.bounded(Duration.ofMillis(250),
                        Duration.ofMillis(5))));
    }

    public static UiNetworkObservation mutation(long sequence, int quantity) {
        String body = "{\"product_id\":\"" + PRODUCT_ID + "\",\"quantity\":" + quantity + "}";
        return observation(sequence, HttpMethod.POST.value(), MUTATION_PATH, body,
                "{\"result\":\"item added or updated\",\"quantity\":" + quantity + "}",
                "POST " + MUTATION_PATH + "#body=cart-line");
    }

    public static UiNetworkObservation readBack(long sequence, int quantity) {
        return observation(sequence, HttpMethod.GET.value(), CART_PATH, "",
                "{\"cart_items\":[{\"product_id\":\"" + PRODUCT_ID
                        + "\",\"quantity\":" + quantity + "}]}",
                "GET " + CART_PATH + "#body=empty");
    }

    private static UiNetworkObservation observation(long sequence, String method, String path,
                                                     String requestBody, String responseBody,
                                                     String signature) {
        return new UiNetworkObservation(sequence, Instant.EPOCH, method,
                ApiUris.httpsUrl(API_HOST, path), path, 200,
                HttpHeaderProfiles.JSON_CONTENT, HttpHeaderProfiles.JSON_CONTENT, requestBody, responseBody,
                "none", "none", API_HOST, "UI_BROWSER", signature, true);
    }
}
