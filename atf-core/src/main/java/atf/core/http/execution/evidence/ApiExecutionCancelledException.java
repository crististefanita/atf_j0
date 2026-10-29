package atf.core.http.execution.evidence;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;

/** Preserves the public cancellation category and original cancellation while carrying the immutable snapshot. */
public final class ApiExecutionCancelledException extends CancellationException implements ApiExecutionFailure {
    private final ApiExecutionSnapshot snapshot;
    public ApiExecutionCancelledException(CancellationException original, ApiExecutionSnapshot snapshot) {
        super(original.getMessage());
        initCause(original);
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }
    @Override public Optional<ApiExecutionSnapshot> snapshot() { return Optional.of(snapshot); }
}
