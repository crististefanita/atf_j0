package atf.reporting.api.request;

import atf.reporting.api.ApiManualReplay;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportRequestHeadersTest {
    @Test
    void observedHeadersAreAuthoritativeAndReplayCannotInventJsonDefaults() {
        var request = ReportRequest.builder("GET", "/observed")
                .headers(ReportRequestHeaders.observed(Map.of("X-Observed", "yes")))
                .body("", ReportBodyAvailability.ABSENT).build();
        String replay = ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP,
                request, "https://fixture.invalid/observed", request.bodyText());
        assertEquals(ReportRequestHeaders.Source.OBSERVED, request.headerEvidence().source());
        assertEquals("[X-Observed=yes]", ApiManualReplay.redactedHeaders(request));
        assertFalse(replay.contains("Content-Type"));
        assertFalse(replay.contains("Accept"));
        assertFalse(replay.contains("--data-raw"));
    }

    @Test
    void declaredClientDefaultsAreCaseInsensitivelyOverriddenNotDuplicated() {
        var headers = ReportRequestHeaders.configured(
                Map.of("Accept", "text/csv", "Content-Type", "application/json"),
                Map.of("content-type", "application/x-www-form-urlencoded"));
        assertEquals(ReportRequestHeaders.Source.CLIENT_CONFIGURED, headers.source());
        assertEquals(2, headers.values().size());
        assertEquals("text/csv", headers.values().get("Accept"));
        assertEquals("application/x-www-form-urlencoded", headers.values().get("content-type"));
        var request = ReportRequest.builder("POST", "/configured").headers(headers)
                .body("name=demo", ReportBodyAvailability.AVAILABLE).build();
        assertTrue(ApiManualReplay.redactedHeaders(request).contains("Content-Type=application/x-www-form-urlencoded"));
    }

    @Test
    void existingClientConstructorsRetainTheirCompatibilityContract() {
        var request = new ReportRequest("GET", "/client", Map.of(), Map.of(), "none", "");
        assertTrue(request.headers().isEmpty());
        assertEquals(ReportRequestHeaders.Source.CLIENT_CONFIGURED, request.headerEvidence().source());
        assertTrue(ApiManualReplay.redactedHeaders(request).contains("Accept=application/json"));
        assertTrue(ApiManualReplay.redactedHeaders(request).contains("Content-Type=application/json"));
    }

    @Test
    void evidenceAndBuilderCopiesAreImmutableAndPreserveTheirSource() {
        var captured = new LinkedHashMap<>(Map.of("Accept", "text/plain"));
        var request = ReportRequest.builder("GET", "/immutable")
                .headers(ReportRequestHeaders.observed(captured)).body("null", ReportBodyAvailability.AVAILABLE).build();
        captured.clear();
        assertEquals("text/plain", request.headers().get("Accept"));
        assertThrows(UnsupportedOperationException.class, () -> request.headerEvidence().values().clear());
        var copy = request.toBuilder().build();
        assertEquals(request, copy);
        assertEquals(request.hashCode(), copy.hashCode());
        assertEquals(ReportRequestHeaders.Source.OBSERVED, copy.headerEvidence().source());
        assertEquals("null", copy.bodyText());
        assertEquals(ReportBodyAvailability.AVAILABLE, copy.bodyAvailability());
    }
}
