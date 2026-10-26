package atf.shop.tests.api.network;

import atf.core.http.response.ApiBodyEvidence;
import atf.core.http.request.HttpMethod;
import atf.reporting.api.ReportRequestSignature;
import atf.testkit.AtfComponentTest;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.ShopApiRoutes;
import atf.shop.support.api.ShopApiEvidenceMapper;
import atf.shop.tests.support.ShopReportHtmlTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Duration;

import static atf.shop.tests.api.network.ShopApiMethodFidelityFixture.QUERY;
import static atf.shop.tests.api.network.ShopApiMethodFidelityFixture.REQUEST_JSON;
import static atf.reporting.api.evidence.ReportBodyAvailability.NOT_CAPTURED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

@AtfComponentTest
class ShopApiMethodFidelityTest {
    @Test
    void browserTimingAndUnavailablePayloadAreNotManufacturedAsZeroOrJsonNull() {
        var unknown = ShopApiMethodFidelityFixture.browserEvidence(QUERY);
        assertNull(unknown.response().duration());
        assertNull(unknown.totalDuration());
        assertNull(unknown.attemptHistory().getFirst().duration());
        var unavailable = ShopApiMethodFidelityFixture.exchange(QUERY).withResponseBody(
                ApiBodyEvidence.unavailable("Browser", "body not exposed"));
        assertEquals(NOT_CAPTURED,
                ShopApiEvidenceMapper.toExecution(unavailable).response().bodyAvailability());
    }
    @Test
    void capturedMethodsRemainFaithfulInModelSignatureAndReplay() throws Exception {
        var json = new ObjectMapper();
        for (String method : ShopApiMethodFidelityFixture.METHODS) {
            var evidence = ShopApiMethodFidelityFixture.browserEvidence(method);
            var request = evidence.request();
            assertEquals(method, request.method());
            assertEquals(json.readTree(REQUEST_JSON), json.readTree(request.bodyText()));
            assertEquals(ReportRequestSignature.forRequest(request), evidence.trace().requestSignature());
            assertTrue(evidence.trace().requestSignature().startsWith(method + " "));
        }
        assertNotEquals(ShopApiMethodFidelityFixture.browserEvidence(QUERY).trace().requestSignature(),
                ShopApiMethodFidelityFixture.browserEvidence(HttpMethod.GET.value()).trace().requestSignature());
        assertEquals(ShopApiOperation.SEARCH,
                ShopApiOperation.resolve(QUERY, ShopApiRoutes.PRODUCT_SEARCH));
    }

    @Test
    void directCoreAdapterPreservesMeasuredResponseAndTotalDurations() {
        var evidence = ShopApiEvidenceMapper.toExecution(ShopApiMethodFidelityFixture.directExecution(QUERY));
        assertEquals(QUERY, evidence.request().method());
        assertEquals(Duration.ofMillis(17), evidence.response().duration());
        assertEquals(Duration.ofMillis(43), evidence.totalDuration());
    }

    @Test
    void bothShopPublishersAndUiEvidenceRenderCapturedQueryRatherThanGet() throws Exception {
        var reports = ShopApiMethodFidelityFixture.publish();
        assertEquals(6, reports.apiRequests().size());
        for (int index = 0; index < reports.apiRequests().size(); index++) {
            var request = reports.apiRequests().get(index);
            String expectedMethod = ShopApiMethodFidelityFixture.METHODS.get(index / 2);
            assertEquals(expectedMethod, request.method(), request.title());
            assertTrue(request.title().startsWith(expectedMethod + " "), request.title());
        }
        String api = Files.readString(reports.api());
        String ui = Files.readString(reports.ui());
        ShopReportHtmlTestSupport.requireText(api,
                "QUERY /products/search — find configured product", "observed and standalone request title");
        ShopReportHtmlTestSupport.requireText(ui,
                "QUERY /products/search", "nested browser-captured request title");

    }
}
