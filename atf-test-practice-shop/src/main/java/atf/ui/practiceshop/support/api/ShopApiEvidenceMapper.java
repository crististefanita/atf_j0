package atf.ui.practiceshop.support.api;

import atf.api.core.http.response.ApiBodyEvidence;
import atf.api.reporting.api.ReportAttemptRecord;
import atf.api.reporting.api.ReportRequest;
import atf.api.reporting.api.request.ReportRequestHeaders;
import atf.api.reporting.api.ReportAuthObservation;
import atf.api.reporting.api.ReportAuthMethod;
import atf.api.reporting.api.ReportResponse;
import atf.api.reporting.api.ReportTransportProvenance;
import atf.api.reporting.api.ReportRequestSignature;
import atf.api.reporting.api.ReportTraceMetadata;
import atf.api.reporting.api.ReportModelAdapters;
import atf.api.reporting.api.ReportQueryParameters;
import atf.api.reporting.api.evidence.ReportBodyAvailability;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.core.http.response.ApiExecutionResult;
import atf.ui.practiceshop.support.ShopJsonEvidence;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Maps one observed API exchange into the reporting contract. */
public final class ShopApiEvidenceMapper {
    private ShopApiEvidenceMapper() { }

    /** Converts a sanitized observed value; capture and checkpoint selection remain the journal's responsibility. */
    public static ReportExecutionEvidence toExecution(ShopApiNetworkJournal.Exchange exchange) {
        return toExecution(exchange, "");
    }

    static ReportExecutionEvidence toExecution(ShopApiNetworkJournal.Exchange exchange,
                                               String serviceAlias) {
        var query = query(exchange.url());
        var authentication = ReportAuthObservation.from("none", exchange.requestHeaders(), query);
        var capturedMethod = ReportAuthMethod.fromProfile(exchange.authType());
        if (capturedMethod != ReportAuthMethod.NONE) {
            authentication = new ReportAuthObservation(capturedMethod, authentication.source());
        }
        ReportRequest request = ReportRequest.builder(exchange.method(), exchange.path()).query(query)
                .headers(ReportRequestHeaders.observed(ShopJsonEvidence.redactHeaders(exchange.requestHeaders())))
                .authProfile("none").authentication(authentication)
                .body(redactJson(exchange.requestBody()), ApiBodyEvidence.isUnavailable(exchange.requestBody())
                        ? ReportBodyAvailability.NOT_CAPTURED : ReportBodyAvailability.from(exchange.requestBody()))
                .build();
        ReportResponse response = new ReportResponse(exchange.status(), redactJson(exchange.responseBody()),
                ShopJsonEvidence.redactHeaders(exchange.responseHeaders()), null, exchange.url(),
                exchange.status() < 0 || ApiBodyEvidence.isUnavailable(exchange.responseBody())
                        ? ReportBodyAvailability.NOT_CAPTURED
                        : ReportBodyAvailability.from(exchange.responseBody()));
        var attempt = new ReportAttemptRecord(1, exchange.status(), null,
                exchange.status() >= 200 && exchange.status() < 400 ? "SUCCESS" : "FAILURE",
                exchange.status() < 0 ? exchange.failureReason() : "final response");
        ReportTraceMetadata trace = ReportTraceMetadata.from(request).withCapture(
                exchange.eventId(), exchange.sequence(), exchange.phase(), exchange.role(),
                exchange.stepId(), exchange.initiator(), exchange.evidenceSource(),
                ReportRequestSignature.responseBodyDigest(response),
                exchange.expected(), exchange.observed());
        if (serviceAlias != null && !serviceAlias.isBlank()) trace = trace.withService(serviceAlias);
        return new ReportExecutionEvidence(response, 1, null, List.of(), List.of(attempt),
                request, ReportTransportProvenance.LIVE_HTTP, trace);
    }

    /** Maps a direct screen-flow execution for the standalone API artifact. */
    public static ReportExecutionEvidence toExecution(ApiExecutionResult execution) {
        return toExecution(execution, 0, "", "", "", "", "", "", "");
    }

    /** Maps a direct execution while retaining its ordered causal context. */
    public static ReportExecutionEvidence toExecution(ApiExecutionResult execution,
                                                       long sequence, String phase,
                                                       String role, String stepId,
                                                       String initiator, String evidenceSource,
                                                       String expected, String observed) {
        ReportExecutionEvidence adapted = ReportModelAdapters.execution(execution);
        var request = adapted.request();
        var response = adapted.response();
        if (request == null || response == null) {
            throw new IllegalArgumentException("API execution must contain request and response");
        }
        ReportRequest reportRequest = request.toBuilder().headers(new ReportRequestHeaders(
                        request.headerEvidence().source(), ShopJsonEvidence.redactHeaders(request.headerEvidence().values())))
                .body(redactJson(request.bodyText()), request.bodyAvailability()).build();
        ReportResponse reportResponse = new ReportResponse(response.status(), redactJson(response.body()),
                ShopJsonEvidence.redactHeaders(response.headers()), response.duration(), response.url(),
                ApiBodyEvidence.isUnavailable(response.body())
                        ? ReportBodyAvailability.NOT_CAPTURED : response.bodyAvailability());
        ReportTraceMetadata trace = ReportTraceMetadata.from(reportRequest).withCapture(
                sequence > 0 ? "E" + sequence : "", sequence, phase, role, stepId,
                initiator, evidenceSource, ReportRequestSignature.responseBodyDigest(reportResponse),
                expected, observed);
        return new ReportExecutionEvidence(reportResponse, adapted.attempts(), adapted.totalDuration(),
                adapted.retryEvents(), adapted.attemptHistory(), reportRequest, adapted.provenance(), trace);
    }

    static Map<String, String> redactHeaders(Map<String, String> headers) {
        return ShopJsonEvidence.redactHeaders(headers);
    }

    public static String redactJson(String value) {
        return ShopJsonEvidence.redact(value);
    }

    private static Map<String, String> query(String url) {
        return ReportQueryParameters.fromUri(URI.create(url)).asMap();
    }
}
