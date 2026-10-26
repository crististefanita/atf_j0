package atf.reporting.api;

import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.transport.ApiTransportProvenance;
import atf.testkit.AtfUnitTest;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.RetryEvent;
import atf.core.http.response.ApiAttemptRecord;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.ui.AtfUiActionRecord;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportModelAdaptersTest {
    @Test
    void adaptsLegacyExecutionWithoutDroppingTypedEvidence() {
        var request = new LegacyRequest("POST", "/users", Map.of("expand", "roles"),
                Map.of("Content-Type", "application/json"), "bearer", "{\"name\":\"ATF\"}");
        var response = new LegacyResponse(201, "{\"id\":1}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(12),
                ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_USERS));
        var execution = new LegacyExecution(response, 2, Duration.ofMillis(31),
                List.of(new LegacyRetry(1, 429, "2", Duration.ofMillis(5), "rate limited")),
                List.of(new LegacyAttempt(1, 429, Duration.ofMillis(7), "RETRY", "rate limited"),
                        new LegacyAttempt(2, 201, Duration.ofMillis(12), "PASS", "")),
                request, "LIVE_HTTP");

        ReportExecutionEvidence adapted = ReportModelAdapters.execution(execution);

        assertEquals(201, adapted.response().status());
        assertEquals("POST", adapted.request().method());
        assertEquals("{\"name\":\"ATF\"}", adapted.request().bodyText());
        assertEquals(ReportTransportProvenance.LIVE_HTTP, adapted.provenance());
        assertEquals(2, adapted.attempts());
        assertEquals(1, adapted.retryEvents().size());
        assertEquals(2, adapted.attemptHistory().size());
    }

    @Test
    void adaptsLiveCoreExecutionWithoutLosingRetryOrTransportFacts() {
        var request = new ApiRequest(
                java.net.http.HttpRequest.BodyPublishers.noBody(), "POST", "/orders",
                Map.of("include", "invoice"), Map.of("Authorization", "Bearer raw-token"),
                "bearer", null, "{\"quantity\":2}");
        var response = new ApiResponse(201, "{\"id\":7}",
                Map.of("content-type", "application/json"), Duration.ofMillis(12),
                ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_USERS));
        var execution = new ApiExecutionResult(response, 2,
                Duration.ofMillis(31),
                List.of(new RetryEvent(2, 503, "Retry-After",
                        Duration.ofMillis(5), "temporary")),
                List.of(new ApiAttemptRecord(1, 503,
                                Duration.ofMillis(7),
                                ApiAttemptOutcome.RETRYABLE_RESPONSE,
                                "temporary"),
                        new ApiAttemptRecord(2, 201,
                                Duration.ofMillis(12),
                                ApiAttemptOutcome.SUCCESS, "")),
                request, ApiTransportProvenance.LIVE_HTTP);

        ReportExecutionEvidence adapted = ReportModelAdapters.execution(execution);

        assertEquals("POST", adapted.request().method());
        assertEquals("invoice", adapted.request().query().get("include"));
        assertEquals("{\"quantity\":2}", adapted.request().bodyText());
        assertEquals(201, adapted.response().status());
        assertEquals(2, adapted.attempts());
        assertEquals(1, adapted.retryEvents().size());
        assertEquals(2, adapted.attemptHistory().size());
        assertEquals(ReportTransportProvenance.LIVE_HTTP, adapted.provenance());
    }

    @Test
    void adaptsCoreAssertionsAndUiActionsThroughOneSharedBoundary() {
        var coreAssertion = new AtfAssertion(
                "visible", "yes", "yes", true,
                atf.core.reporting.contract.AtfReportStatus.WARNING);
        var coreAction = new AtfUiActionRecord(4, "Fill", "#email",
                "configured value", "value entered", 18,
                atf.core.reporting.contract.AtfReportStatus.PASS,
                atf.core.ui.AtfUiEvidenceSource.OBSERVE);

        var assertion = ReportModelAdapters.assertion(coreAssertion);
        var actions = ReportModelAdapters.uiActions(List.of(coreAction));

        assertEquals(AtfReportStatus.WARNING, assertion.resultStatus());
        assertEquals("#email", actions.getFirst().target());
        assertEquals(4, actions.getFirst().sequence());
        assertEquals(AtfUiEvidenceSource.OBSERVE, actions.getFirst().source());
        assertEquals(AtfReportStatus.PASS, actions.getFirst().status());
        assertSame(actions.getFirst(), ReportModelAdapters.uiAction(actions.getFirst()));
    }

    @Test
    void rejectsUnknownCoreUiStatusInsteadOfSilentlyChangingEvidence() {
        record InvalidAction(int sequence, String operation, String target, String expected,
                             String observed, long durationMillis, String status, String source) { }

        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportModelAdapters.uiAction(new InvalidAction(1, "Click", "#submit",
                        "enabled", "clicked", 1, "SUCCESS", "ACTION")));

        assertEquals("status", error.field());
        assertTrue(error.getMessage().contains("unsupported status 'SUCCESS'"));
    }

    @Test
    void preservesTypedReportingModelsWithoutReflection() {
        var request = new ReportRequest("GET", "/users", Map.of(), Map.of(), "none", "");
        assertSame(request, ReportModelAdapters.request(request));
        assertEquals(ReportTransportProvenance.ACCEPTANCE_MOCK,
                ReportModelAdapters.provenance(ReportTransportProvenance.ACCEPTANCE_MOCK));
    }

    @Test
    void missingRequiredFieldFailsWithSourceAndField() {
        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportModelAdapters.request(new MissingMethodRequest("/users")));

        assertEquals("method", error.field());
        assertTrue(error.getMessage().contains("MissingMethodRequest"));
        assertTrue(error.getMessage().contains("required"));
    }

    @Test
    void invalidMapValueFailsInsteadOfBeingStringified() {
        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportModelAdapters.request(new LegacyRequest("GET", "/users",
                        Map.of("page", 1), Map.of(), "none", "")));

        assertEquals("query", error.field());
        assertTrue(error.getMessage().contains("non-text value"));
    }

    @Test
    void unknownProvenanceFailsInsteadOfBeingGuessedBySubstring() {
        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportModelAdapters.provenance("LIVE_HTTP_COMPAT"));

        assertEquals("provenance", error.field());
        assertTrue(error.getMessage().contains("unsupported value"));
    }

    @Test
    void invalidRetryContainerFailsWithTheResponsibleField() {
        var response = new LegacyResponse(200, "{}", Map.of(), Duration.ZERO,
                ApiReportTestUrls.atf(ApiReportTestUrls.GENERIC_USERS));
        var execution = new LegacyExecution(response, 1, Duration.ZERO, "not-a-list",
                List.of(), null, null);

        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportModelAdapters.execution(execution));

        assertEquals("retryEvents", error.field());
        assertTrue(error.getMessage().contains("Iterable"));
    }

    @Test
    void queryAdapterAcceptsMapsAndRejectsUnsupportedShapes() {
        assertEquals("details", ReportQueryParameters.from(Map.of("include", "details"))
                .asMap().get("include"));

        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportQueryParameters.from(new Object()));

        assertEquals("asMap", error.field());
        assertTrue(error.getMessage().contains("method is missing"));
    }

    @Test
    void queryAdapterDoesNotStringifyNonTextValues() {
        ReportModelAdapterException error = assertThrows(ReportModelAdapterException.class,
                () -> ReportQueryParameters.from(Map.of("page", 1)));

        assertEquals("queryParameters", error.field());
        assertTrue(error.getMessage().contains("non-text value"));
    }

    @Test
    void uriQueryProjectionDecodesFormValuesAndKeepsOrderedDuplicateObservations() {
        ReportQueryParameters query = ReportQueryParameters.fromUri(
                URI.create("https://service.example/orders?state=ready+to+ship&filter=a%2Bb&state=done&empty="));

        assertEquals(List.of("ready to ship", "a+b", "done", ""),
                query.values().stream().map(ReportQueryParameter::value).toList());
        assertEquals("done", query.asMap().get("state"));
    }

    record LegacyRequest(String method, String path, Object query, Object headers,
                         String authProfile, String bodyText) {
    }

    record LegacyResponse(int status, String body, Object headers, Duration duration, String url) {
    }

    record LegacyRetry(int attempt, int statusCode, String header, Duration delay, String reason) {
    }

    record LegacyAttempt(int number, int statusCode, Duration duration, String outcome, String reason) {
    }

    record LegacyExecution(LegacyResponse response, int attempts, Duration totalDuration,
                           Object retryEvents, Object attemptHistory, Object request,
                           Object provenance) {
    }

    record MissingMethodRequest(String path) {
    }
}
