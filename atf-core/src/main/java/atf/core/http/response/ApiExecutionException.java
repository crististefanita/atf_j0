package atf.core.http.response;

import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.execution.evidence.ApiExecutionSnapshot;
import java.util.Optional;

/** Infrastructure failure with its original cause and optional immutable transport evidence. */
public final class ApiExecutionException extends RuntimeException implements ApiExecutionFailure {
    private final ApiExecutionSnapshot snapshot;
    public ApiExecutionException(String message, Throwable cause) {
        this(message, cause, null);
    }
    public ApiExecutionException(String message, Throwable cause, ApiExecutionSnapshot snapshot) {
        super(message, cause);
        this.snapshot = snapshot;
    }
    @Override public Optional<ApiExecutionSnapshot> snapshot() { return Optional.ofNullable(snapshot); }

    /** Adds invocation-owned evidence without replacing the primary cause, stack or suppressed failures. */
    public ApiExecutionException withSnapshot(ApiExecutionSnapshot value) {
        var enriched = new ApiExecutionException(getMessage(), getCause(), java.util.Objects.requireNonNull(value));
        enriched.setStackTrace(getStackTrace());
        for (Throwable secondary : getSuppressed()) enriched.addSuppressed(secondary);
        return enriched;
    }
}
