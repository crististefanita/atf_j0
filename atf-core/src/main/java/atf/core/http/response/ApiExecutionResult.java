package atf.core.http.response;
import atf.core.http.request.ApiRequest;
import atf.core.http.transport.ApiTransportProvenance;

import java.time.Duration;
import java.util.List;

public record ApiExecutionResult(ApiResponse response, int attempts, Duration totalDuration,
                                 List<RetryEvent> retryEvents, List<ApiAttemptRecord> attemptHistory,
                                 ApiRequest request, ApiTransportProvenance provenance) {
    public ApiExecutionResult {
        retryEvents = retryEvents == null ? List.of() : List.copyOf(retryEvents);
        attemptHistory = attemptHistory == null ? List.of() : List.copyOf(attemptHistory);
        provenance = provenance == null ? ApiTransportProvenance.UNKNOWN : provenance;
    }

    /** Compatibility constructor for callers that already provide the request. */
    public ApiExecutionResult(ApiResponse response, int attempts, Duration totalDuration,
                              List<RetryEvent> retryEvents, List<ApiAttemptRecord> attemptHistory,
                              ApiRequest request) {
        this(response, attempts, totalDuration, retryEvents, attemptHistory, request,
                ApiTransportProvenance.UNKNOWN);
    }

    /** Compatibility constructor for callers that only need retry metadata. */
    public ApiExecutionResult(ApiResponse response, int attempts, Duration totalDuration, List<RetryEvent> retryEvents) {
        this(response, attempts, totalDuration, retryEvents, List.of(), null);
    }

    /** Compatibility constructor for callers that already provide attempt history. */
    public ApiExecutionResult(ApiResponse response, int attempts, Duration totalDuration,
                              List<RetryEvent> retryEvents, List<ApiAttemptRecord> attemptHistory) {
        this(response, attempts, totalDuration, retryEvents, attemptHistory, null);
    }
}
