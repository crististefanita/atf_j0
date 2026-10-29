package atf.core.http.execution.evidence;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiAttemptRecord;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.RetryEvent;
import atf.core.http.transport.ApiTransportProvenance;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable partial execution, including zero dispatches and an explicitly absent terminal response. */
public record ApiExecutionSnapshot(ApiRequest request, String url, ApiTransportProvenance provenance,
                                   Duration totalDuration, List<RetryEvent> retryEvents,
                                   List<ApiAttemptRecord> attemptHistory, Optional<ApiResponse> response,
                                   ApiAttemptOutcome termination) {
    public ApiExecutionSnapshot {
        Objects.requireNonNull(request, "request"); Objects.requireNonNull(url, "url");
        Objects.requireNonNull(provenance, "provenance"); Objects.requireNonNull(totalDuration, "totalDuration");
        if (totalDuration.isNegative()) throw new IllegalArgumentException("totalDuration must not be negative");
        retryEvents = List.copyOf(retryEvents); attemptHistory = List.copyOf(attemptHistory);
        response = Objects.requireNonNull(response, "response");
        if (termination != ApiAttemptOutcome.FAILED && termination != ApiAttemptOutcome.TIMED_OUT
                && termination != ApiAttemptOutcome.CANCELLED && termination != ApiAttemptOutcome.INTERRUPTED) {
            throw new IllegalArgumentException("A failed snapshot requires a terminal outcome");
        }
        for (int index = 0; index < attemptHistory.size(); index++) {
            if (attemptHistory.get(index).number() != index + 1) throw new IllegalArgumentException("Attempt order must be contiguous");
        }
    }
    /** Actual dispatches; a cancelled backoff does not invent another attempt. */
    public int attempts() { return attemptHistory.size(); }
}
