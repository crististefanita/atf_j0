package atf.api.reporting.api.evidence;

import atf.api.reporting.api.ApiManualReplay;
import atf.api.reporting.api.ReportRequest;
import atf.api.reporting.api.ReportRequestSignature;
import atf.api.reporting.api.ReportResponse;
import atf.api.reporting.api.ReportTransportProvenance;
import atf.api.reporting.model.ReportJsonBody;
import atf.api.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiWhitespaceBodyComponentTest {
    private static final String WHITESPACE = " \r\n\t ";
    private static final String MIME = "text/plain";

    @ParameterizedTest
    @EnumSource(ApiBodyArtifactFixture.Projection.class)
    void capturedWhitespaceSurvivesRequestAndResponsePublication(ApiBodyArtifactFixture.Projection projection)
            throws Exception {
        var exchange = ApiBodyArtifactFixture.exchange(WHITESPACE, MIME, ReportBodyAvailability.AVAILABLE);
        var published = ApiBodyArtifactFixture.publish("whitespace", projection, List.of(exchange));
        assertEquals(List.of(WHITESPACE), ApiBodyArtifactFixture.bodyText(published.html(), "request"));
        assertEquals(List.of(WHITESPACE), ApiBodyArtifactFixture.bodyText(published.html(), "response"));
        assertTrue(published.html().contains("REQUEST BODY"));
        assertFalse(published.html().contains("REQUEST JSON"));
        assertTrue(published.html().contains("Captured"));
        var projected = new LinkedHashMap<String, String>();
        ApiExchangeEvidence.publishFields(exchange, (name, value) -> { }, projected::put);
        assertEquals(WHITESPACE, projected.get("REQUEST BODY"));
        assertEquals(WHITESPACE, projected.get("RESPONSE BODY"));
    }

    @Test
    void availabilityReplayAndSignaturesDistinguishWhitespaceFromAbsence() {
        var request = new ReportRequest("POST", "/body-audit", Map.of(), Map.of("Content-Type", MIME), "none", WHITESPACE);
        var response = new ReportResponse(200, WHITESPACE, Map.of(), Duration.ZERO, "https://audit.example.test/body-audit");
        assertEquals(ReportBodyAvailability.AVAILABLE, request.bodyAvailability());
        assertEquals(ReportBodyAvailability.AVAILABLE, response.bodyAvailability());
        assertTrue(new ReportJsonBody("REQUEST BODY", WHITESPACE,
                ReportJsonBody.Direction.REQUEST, ReportJsonBody.Format.TEXT).isPresent());
        assertNotEquals(ReportRequestSignature.bodyDigest(""), ReportRequestSignature.bodyDigest(WHITESPACE));
        assertNotEquals(ReportRequestSignature.bodyDigest(" text"), ReportRequestSignature.bodyDigest("text "));
        assertTrue(ReportRequestSignature.displaySignature(ReportRequestSignature.forRequest(request)).endsWith("#body=present"));
        String replay = ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP, request, response.url(), WHITESPACE);
        assertTrue(replay.endsWith(" --data-raw '" + WHITESPACE + "'"));
    }

    @Test
    void absentLiteralNullAndUncapturedStillHaveDistinctEvidence() {
        var absent = new LinkedHashMap<String, String>();
        var absentDetails = new LinkedHashMap<String, String>();
        ApiExchangeEvidence.publishFields(ApiBodyArtifactFixture.exchange("", MIME, ReportBodyAvailability.ABSENT),
                absentDetails::put, absent::put);
        assertTrue(absent.isEmpty(), "confirmed absence must not become a JSON null document");
        assertEquals("Absent — confirmed no HTTP body", absentDetails.get("request.body.capture"));
        var literal = new LinkedHashMap<String, String>();
        var literalDetails = new LinkedHashMap<String, String>();
        ApiExchangeEvidence.publishFields(ApiBodyArtifactFixture.exchange("null", "application/json", ReportBodyAvailability.AVAILABLE),
                literalDetails::put, literal::put);
        assertEquals("null", literal.get("REQUEST JSON"));
        assertEquals("Captured", literalDetails.get("request.body.capture"));
        var missing = new LinkedHashMap<String, String>();
        ApiExchangeEvidence.publishFields(ApiBodyArtifactFixture.exchange("", MIME, ReportBodyAvailability.NOT_CAPTURED),
                (name, value) -> { }, missing::put);
        assertTrue(missing.isEmpty());
        assertEquals(ReportBodyAvailability.ABSENT, ReportBodyAvailability.from(""));
        assertEquals(ReportBodyAvailability.NOT_CAPTURED, ReportBodyAvailability.from(null));
    }
}
