package atf.ui.playwright.recovery;

import atf.ui.config.recovery.UiRecoveryConfiguration;
import atf.ui.recovery.*;
import com.microsoft.playwright.Page;
import java.time.*;
import java.util.*;
import java.util.function.*;

/** One invocation owns a fixed semantic registry; scopes can reobserve but cannot dispatch or replay an action. */
public final class UiBackendRecovery implements AutoCloseable {
    private final Page page;
    private final UiRecoveryConfiguration policy;
    private final Map<String, UiRecoveryOperation> operations;
    private final Consumer<UiRecoveryTrace> sink;
    private final UUID invocation = UUID.randomUUID();
    private Scope active;
    private boolean closed;

    public UiBackendRecovery(Page page, UiRecoveryConfiguration policy, Collection<UiRecoveryOperation> operations, Consumer<UiRecoveryTrace> sink) {
        this.page = Objects.requireNonNull(page); this.policy = Objects.requireNonNull(policy); this.sink = Objects.requireNonNull(sink);
        var registry = new HashMap<String, UiRecoveryOperation>();
        for (var operation : operations) if (registry.putIfAbsent(operation.name(), operation) != null) throw new IllegalArgumentException("Duplicate recovery operation");
        this.operations = Map.copyOf(registry);
        policy.requireKnownReadOnlyOperations(registry.values().stream().filter(operation -> operation.safety() == UiRecoveryOperation.Safety.READ_ONLY)
                .map(UiRecoveryOperation::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
    public Scope begin(String semanticOperation) {
        if (closed || active != null) throw new IllegalStateException("Recovery scopes cannot overlap, nest or reopen a closed invocation");
        var operation = operations.get(semanticOperation);
        if (operation == null) throw new IllegalArgumentException("Unknown recovery operation: " + semanticOperation);
        if (!UiRecoveryOperation.origin(operation.pageOrigin()).equals(UiRecoveryOperation.origin(java.net.URI.create(page.url())))) {
            throw new IllegalStateException("Recovery requires the invocation's current application document");
        }
        active = new Scope(operation);
        return active;
    }
    @Override public void close() { if (closed) return; closed = true; if (active != null) active.close(); }

    public final class Scope implements AutoCloseable {
        private final UiRecoveryLoop loop;
        private final UiRecoveryObserver observer;
        private boolean closed;
        private Scope(UiRecoveryOperation operation) {
            var stamp = new UiRecoverySignal.Stamp(invocation, UUID.randomUUID(), UUID.randomUUID(), 0);
            loop = new UiRecoveryLoop(policy, operation, stamp, System::nanoTime, Clock.systemUTC(),
                    duration -> page.waitForTimeout(duration.toNanos() / 1_000_000.0), sink);
            observer = new UiRecoveryObserver(page, operation, stamp, Clock.systemUTC(), policy.enabled() && policy.captureBackendSignals());
        }
        /** Pass this remainder to any bounded work in the original action; it never creates a second deadline. */
        public Duration remaining() { if (closed) throw new IllegalStateException("Recovery scope is closed"); return loop.remaining(); }
        public UiRecoveryTrace await(Supplier<UiRecoveryTrace.Dom> readOnlyProbe) {
            if (closed) throw new IllegalStateException("Recovery scope is closed");
            return loop.await(observer, readOnlyProbe);
        }
        @Override public void close() { if (closed) return; closed = true; try { observer.close(); } finally { active = null; } }
    }
}
