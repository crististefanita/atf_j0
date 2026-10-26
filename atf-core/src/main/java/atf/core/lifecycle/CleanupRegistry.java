package atf.core.lifecycle;

import java.util.ArrayDeque;
import java.util.Deque;

/** LIFO cleanup boundary; failures are retained while later cleanup continues. */
public final class CleanupRegistry {
    private final Deque<Runnable> actions = new ArrayDeque<>();
    private CleanupResult completedResult;
    private boolean running;

    /**
     * Registers cleanup for this lifecycle owner. Registration is intentionally
     * rejected after execution starts: a late action would have no deterministic
     * place in the already-running LIFO sequence.
     */
    public synchronized void register(Runnable cleanup) {
        if (cleanup == null) throw new IllegalArgumentException("cleanup must not be null");
        if (running || completedResult != null) {
            throw new IllegalStateException("cleanup registry is already running or completed");
        }
        actions.push(cleanup);
    }

    /**
     * Executes this registry exactly once. Concurrent callers share the same
     * immutable result; cleanup actions therefore cannot be run twice by racing
     * teardown paths.
     */
    public synchronized CleanupResult run() {
        if (completedResult != null) return completedResult;
        if (running) throw new IllegalStateException("cleanup registry cannot be re-entered");
        running = true;
        int executed = 0;
        var failures = new java.util.ArrayList<RuntimeException>();
        try {
            while (!actions.isEmpty()) {
                try { actions.pop().run(); }
                catch (RuntimeException error) { failures.add(error); }
                executed++;
            }
            completedResult = new CleanupResult(executed, failures);
            return completedResult;
        } finally {
            running = false;
        }
    }

    public record CleanupResult(int executed, java.util.List<RuntimeException> failures) {
        public CleanupResult { failures = java.util.List.copyOf(failures); }
        public boolean succeeded() { return failures.isEmpty(); }
    }
}
