package atf.core.http.execution;

    /** Minimal token for cooperative cancellation of execution and retry delays. */
@FunctionalInterface
public interface CancellationToken {
    boolean isCancelled();

    default void throwIfCancelled() {
        if (isCancelled()) {
            throw new java.util.concurrent.CancellationException("API execution was cancelled");
        }
    }

    static CancellationToken none() {
        return () -> false;
    }
}
