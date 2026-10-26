package atf.reporting.api;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportRequestSignatureTest {
    @Test
    void canonicalJsonOrderingProducesOneLogicalSignature() {
        var first = request("{\"quantity\":2,\"product_id\":\"p-1\"}");
        var reordered = request("{ \"product_id\": \"p-1\", \"quantity\": 2 }");

        assertEquals(ReportRequestSignature.forRequest(first),
                ReportRequestSignature.forRequest(reordered));
    }

    @Test
    void absentBodyIsDistinctFromEmptyJsonObject() {
        assertNotEquals(ReportRequestSignature.bodyDigest(""),
                ReportRequestSignature.bodyDigest("{}"));
    }

    @Test
    void sensitiveQueryValuesAreRedactedBeforeSignature() {
        var first = new ReportRequest("GET", "/users", Map.of("token", "one", "page", "1"),
                Map.of(), "none", "");
        var second = new ReportRequest("GET", "/users", Map.of("token", "two", "page", "1"),
                Map.of(), "none", "");

        assertEquals(ReportRequestSignature.forRequest(first),
                ReportRequestSignature.forRequest(second));
        assertTrue(ReportRequestSignature.forRequest(first).contains("body="));
    }

    @Test
    void arrayOrderRemainsBusinessSignificant() {
        assertNotEquals(ReportRequestSignature.bodyDigest("{\"ids\":[1,2]}"),
                ReportRequestSignature.bodyDigest("{\"ids\":[2,1]}"));
    }

    @Test
    void publicDisplayKeepsCorrelationWithoutPublishingDigest() {
        String signature = ReportRequestSignature.forRequest(request("{\"quantity\":2}"));

        String display = ReportRequestSignature.displaySignature(signature);

        assertTrue(display.endsWith("#body=present"));
        assertFalse(display.matches(".*[0-9a-f]{64}.*"));
        assertEquals("absent (internal digest hidden)",
                ReportRequestSignature.displayDigest(ReportRequestSignature.bodyDigest("")));
    }

    private static ReportRequest request(String body) {
        return new ReportRequest("POST", "/carts/cart-1", Map.of("source", "ui"),
                Map.of("Content-Type", "application/json"), "none", body);
    }
}
