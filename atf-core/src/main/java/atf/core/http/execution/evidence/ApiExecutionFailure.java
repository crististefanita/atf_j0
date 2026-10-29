package atf.core.http.execution.evidence;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/** Typed evidence on execution exceptions; legacy exceptions may have no snapshot. */
public interface ApiExecutionFailure {
    Optional<ApiExecutionSnapshot> snapshot();

    /** Follows the primary cause chain only; unrelated suppressed cleanup failures cannot supply evidence. */
    static Optional<ApiExecutionSnapshot> find(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = failure; current != null && seen.add(current); current = current.getCause()) {
            if (current instanceof ApiExecutionFailure evidence && evidence.snapshot().isPresent()) return evidence.snapshot();
        }
        return Optional.empty();
    }
}
