package atf.api.reporting.ui.failure;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfReportStatus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** An assertion failure propagated from explicitly identified UI evidence, never matched by text. */
public final class UiCheckpointFailure extends AssertionError {
    private final List<AtfAssertion> assertions;

    public UiCheckpointFailure(String message, Collection<AtfAssertion> assertions) {
        super(Objects.requireNonNull(message, "message"));
        this.assertions = List.copyOf(assertions).stream()
                .filter(assertion -> assertion.resultStatus() == AtfReportStatus.FAIL).toList();
        if (this.assertions.isEmpty()) {
            throw new IllegalArgumentException("A checkpoint failure requires failed assertions");
        }
    }

    /** The supplied set must use reference identity and belong to the current scenario. */
    public static boolean isReported(Throwable failure, Set<AtfAssertion> reported) {
        Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
            // Cleanup failures must retain an independent terminal cause.
            if (cause.getSuppressed().length != 0) return false;
            if (cause instanceof UiCheckpointFailure checkpoint) {
                return checkpoint.getCause() == null && reported.containsAll(checkpoint.assertions);
            }
        }
        return false;
    }
}
