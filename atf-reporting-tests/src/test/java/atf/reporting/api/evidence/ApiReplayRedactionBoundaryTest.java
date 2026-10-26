package atf.reporting.api.evidence;

import atf.reporting.api.ApiManualReplay;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.ReportRequestSignature;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfUnitTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ApiReplayRedactionBoundaryTest {
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();

    @Test
    void rendererRedactionKeepsSafeCurlAndHeaderDelimitersIntact() {
        var request = new ReportRequest("QUERY", "/resource", Map.of(),
                Map.of("Authorization", "Bearer raw-secret"), "bearer", "{}");
        String replay = ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP,
                request, "https://api.example.test/resource?token=raw-url-secret", request.bodyText());
        String headers = ApiManualReplay.redactedHeaders(request);
        assertEquals(replay, redactor.redactText(replay));
        assertEquals(headers, redactor.redactText(headers));
        assertFalse(replay.contains("raw-secret"));
        assertFalse(replay.contains("raw-url-secret"));
    }

    @Test
    void quotedCredentialsAreRedactedWithoutEatingTheirClosingQuote() {
        assertEquals("Authorization: 'Bearer ***REDACTED***'",
                redactor.redactText("Authorization: 'Bearer raw-secret'"));
        assertEquals("Authorization=Basic ***REDACTED***]",
                redactor.redactText("Authorization=Basic raw-secret]"));
    }

    @Test
    void uncapturedBodyCannotBeReplayedOrClassifiedAsAbsent() {
        var missing = new ReportRequest("POST", "/resource", Map.of(), Map.of(), "none", null);
        assertTrue(ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP, missing,
                "https://api.example.test/resource", null).startsWith("NOT REPLAYABLE"));
        assertTrue(ReportRequestSignature.displaySignature(ReportRequestSignature.forRequest(missing))
                .endsWith("#body=not-captured"));
        var absent = new ReportRequest("GET", "/resource", Map.of(), Map.of(), "none", "");
        assertTrue(ReportRequestSignature.displaySignature(ReportRequestSignature.forRequest(absent))
                .endsWith("#body=absent"));
    }
}
