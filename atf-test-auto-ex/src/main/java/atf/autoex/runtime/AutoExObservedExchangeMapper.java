package atf.autoex.runtime;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportAuthMethod;
import atf.reporting.api.ReportAuthObservation;
import atf.reporting.api.ReportAuthSource;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportRequestSignature;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTraceMetadata;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.api.request.ReportRequestHeaders;
import atf.reporting.model.ReportExecutionEvidence;
import atf.ui.network.UiNetworkObservation;

import java.util.List;
import java.util.Locale;

import static atf.reporting.ui.contract.ReportUiContract.Phase.TEST_BODY;

/** Converts already sanitized UI observations into Reporting values at the consumer boundary. */
public final class AutoExObservedExchangeMapper {
    private AutoExObservedExchangeMapper() { }
    public static ReportExecutionEvidence map(UiNetworkObservation observation) {
        var request = ReportRequest.builder(observation.method(), observation.path())
                .headers(ReportRequestHeaders.observed(observation.requestHeaders()))
                .authProfile(observation.authMethod())
                .body(observation.requestBody(), ReportBodyAvailability.from(observation.requestBody()))
                .authentication(new ReportAuthObservation(ReportAuthMethod.fromProfile(observation.authMethod()),
                        switch (observation.authSource()) {
                            case "request-header" -> ReportAuthSource.HEADER;
                            case "query" -> ReportAuthSource.QUERY;
                            default -> ReportAuthSource.NONE;
                        })).build();
        var response = new ReportResponse(observation.status(), observation.responseBody(),
                observation.responseHeaders(), null, observation.url(), observation.responseBodyAvailable()
                        ? ReportBodyAvailability.from(observation.responseBody())
                        : ReportBodyAvailability.NOT_CAPTURED);
        var trace = ReportTraceMetadata.from(request).withCapture("", observation.sequence(),
                TEST_BODY.displayName(), "api", "", observation.resourceKind().name().toLowerCase(Locale.ROOT),
                observation.evidenceSource(), ReportRequestSignature.responseBodyDigest(response), "", "")
                .withService(observation.service());
        var attempts = observation.requestFailed()
                ? List.of(new ReportAttemptRecord(1, -1, null, "FAILURE", observation.failureReason()))
                : List.<ReportAttemptRecord>of();
        return new ReportExecutionEvidence(response, 1, null, List.of(), attempts, request,
                ReportTransportProvenance.LIVE_HTTP, trace);
    }

}
