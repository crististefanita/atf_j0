package atf.reporting.model;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportRetryEvent;
import atf.reporting.api.ReportTraceMetadata;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.evidence.ReportTransportTiming;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Standalone transport evidence; no HTTP client, executor or core model leaks into reporting. */
public record ReportExecutionEvidence(ReportResponse response, int attempts, Duration totalDuration,
                                      List<ReportRetryEvent> retryEvents, List<ReportAttemptRecord> attemptHistory,
                                      ReportRequest request, ReportTransportProvenance provenance,
                                      ReportTraceMetadata trace, Optional<Termination> termination) {
    public ReportExecutionEvidence {
        retryEvents=retryEvents==null?List.of():List.copyOf(retryEvents);
        attemptHistory=attemptHistory==null?List.of():List.copyOf(attemptHistory);
        ReportTransportTiming.validate(totalDuration);
        provenance=provenance==null?ReportTransportProvenance.UNKNOWN:provenance;
        trace=trace==null?ReportTraceMetadata.from(request):trace;
        termination=Objects.requireNonNull(termination, "termination");
    }

    /** Compatibility constructor for callers predating terminal execution evidence. */
    public ReportExecutionEvidence(ReportResponse response, int attempts, Duration totalDuration,
                                   List<ReportRetryEvent> retryEvents, List<ReportAttemptRecord> attemptHistory,
                                   ReportRequest request, ReportTransportProvenance provenance, ReportTraceMetadata trace) {
        this(response, attempts, totalDuration, retryEvents, attemptHistory, request, provenance, trace, Optional.empty());
    }

    public record Termination(String outcome, String diagnostic) {
        public Termination {
            if (outcome == null || outcome.isBlank()) throw new IllegalArgumentException("A terminal outcome is required");
            diagnostic=Objects.requireNonNullElse(diagnostic, "");
        }
    }
    public ReportExecutionEvidence withTermination(String outcome, String diagnostic) {
        return new ReportExecutionEvidence(response, attempts, totalDuration, retryEvents, attemptHistory,
                request, provenance, trace, Optional.of(new Termination(outcome, diagnostic)));
    }

    /** Compatibility constructor for all existing consumers. */
    public ReportExecutionEvidence(ReportResponse response, int attempts, Duration totalDuration,
                                   List<ReportRetryEvent> retryEvents, List<ReportAttemptRecord> attemptHistory,
                                   ReportRequest request, ReportTransportProvenance provenance) {
        this(response, attempts, totalDuration, retryEvents, attemptHistory, request, provenance,
                ReportTraceMetadata.from(request));
    }
}
