package atf.shop.tests.api.unit.exchange;

import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.HttpMethod;
import atf.testkit.AtfUnitTest;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.support.api.ShopApiExchangeSelector;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ShopApiExchangeSelectorTest {
    @Test
    void latestSelectsTheMostRecentMatchingExchangeWithoutChangingCaptureOrder() {
        String cartPath = ShopApiRoutes.cart(ShopApiScenarioFixture.MUTATION_CART_ID);
        var first = exchange(HttpMethod.GET.value(), cartPath, ShopApiHttpStatus.OK, "first");
        var second = exchange(HttpMethod.GET.value(), cartPath, ShopApiHttpStatus.OK, "second");

        assertEquals(second, ShopApiExchangeSelector.latest(
                List.of(first, second), HttpMethod.GET.value(), cartPath,
                item -> ShopApiHttpStatus.OK.matches(item.status())));
    }

    @Test
    void operationSelectionAcceptsObservedQueryAliasForCatalogSearch() {
        var observed = exchange("QUERY", ShopApiRoutes.PRODUCT_SEARCH,
                ShopApiHttpStatus.OK, "search-result");

        assertEquals(observed, ShopApiExchangeSelector.latest(
                List.of(observed), ShopApiOperation.SEARCH, item -> true));
        assertEquals("QUERY /products/search → 200 application/json",
                observed.reportSummary());
    }

    @Test
    void selectionUsesTheNormalizedRouteInsteadOfAContainsMatch() {
        var misleading = exchange(HttpMethod.GET.value(), ShopApiRoutes.product(
                ShopApiScenarioFixture.PRODUCT_ID) + "-archive", ShopApiHttpStatus.OK, "wrong");
        assertThrows(IllegalStateException.class, () -> ShopApiExchangeSelector.latest(
                List.of(misleading), HttpMethod.GET.value(),
                ShopApiRoutes.product(ShopApiScenarioFixture.PRODUCT_ID), item -> true));
    }

    @Test
    void tracedExchangesKeepSequencePhaseSignatureAndDigest() {
        var traced = exchange(HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK,
                ShopApiHttpStatus.OK, "payment")
                .withTrace(7, Instant.parse("2026-10-20T10:00:00Z"), "checkout");

        assertEquals(7, traced.sequence());
        assertEquals("checkout", traced.phase());
        assertFalse(traced.signature().isBlank());
        assertEquals(64, traced.digest().length());
    }

    @Test
    void requestSignatureIsStableForRetriesButChangesWithTheRequestBody() {
        var first = exchange(HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK,
                ShopApiHttpStatus.OK, "{\"payment_method\":\"cash-on-delivery\"}")
                .withTrace(1, Instant.parse("2026-10-20T10:00:00Z"), "checkout");
        var retry = exchange(HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK,
                ShopApiHttpStatus.OK, "{\"payment_method\":\"cash-on-delivery\"}")
                .withTrace(2, Instant.parse("2026-10-20T10:00:01Z"), "checkout");
        var differentBody = exchange(HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK,
                ShopApiHttpStatus.OK, "{\"payment_method\":\"bank-transfer\"}")
                .withTrace(3, Instant.parse("2026-10-20T10:00:02Z"), "checkout");

        assertEquals(first.signature(), retry.signature());
        assertFalse(first.signature().isBlank());
        assertFalse(first.signature().equals(differentBody.signature()));
    }

    @Test
    void productIdExtractionIgnoresQueryAndFragmentButRejectsNonProductRoutes() {
        assertEquals("product-42", ShopApiExchangeSelector.productIdFromPath(
                ShopApiRoutes.product("product-42") + "?source=search#details"));
        assertNull(ShopApiExchangeSelector.productIdFromPath(ShopApiRoutes.PRODUCTS));
        assertNull(ShopApiExchangeSelector.productIdFromPath(null));
    }

    private static ShopApiNetworkJournal.Exchange exchange(
            String method, String path, ShopApiHttpStatus status, String body) {
        return new ShopApiNetworkJournal.Exchange(
                method, path, ShopApiScenarioFixture.API_ENDPOINT + path, status.code(),
                body, body, Map.of(),
                HttpHeaderProfiles.JSON_CONTENT);
    }
}
