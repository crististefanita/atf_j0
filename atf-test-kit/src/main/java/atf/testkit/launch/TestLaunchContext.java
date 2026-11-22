package atf.testkit.launch;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Invocation-bound access; no source names or completed-test totals are inferred. */
public final class TestLaunchContext {
    private static final ThreadLocal<List<Scope>> SCOPES = ThreadLocal.withInitial(ArrayList::new);
    private TestLaunchContext() { }

    public static Optional<TestLaunchSnapshot> current() {
        var scopes = SCOPES.get();
        synchronized (scopes) {
            if (!scopes.isEmpty()) return Optional.of(scopes.getLast().snapshot.get());
            SCOPES.remove();
            return Optional.empty();
        }
    }

    public static Optional<TestLaunchSnapshot> capture(ExtensionContext context) {
        var id = UniqueId.parse(context.getUniqueId());
        var scopes = SCOPES.get();
        synchronized (scopes) {
            if (!scopes.isEmpty() && id.hasPrefix(scopes.getLast().id)) {
                return Optional.of(scopes.getLast().snapshot.get());
            }
            if (scopes.isEmpty()) SCOPES.remove();
            return Optional.empty();
        }
    }

    static Optional<Supplier<TestLaunchSnapshot>> enclosingSuite(List<UniqueId> roots) {
        var scopes = SCOPES.get();
        synchronized (scopes) {
            for (int index = scopes.size() - 1; index >= 0; index--) {
                var scope = scopes.get(index);
                if (!scope.id.getLastSegment().getType().equals("suite")) continue;
                if (roots.stream().allMatch(root -> root.hasPrefix(scope.id))) return Optional.of(scope.snapshot);
            }
            if (scopes.isEmpty()) SCOPES.remove();
            return Optional.empty();
        }
    }

    static Scope bind(UniqueId id, Supplier<TestLaunchSnapshot> snapshot) {
        var scopes = SCOPES.get();
        var scope = new Scope(scopes, id, snapshot);
        synchronized (scopes) { scopes.add(scope); }
        return scope;
    }

    static final class Scope implements AutoCloseable {
        private final List<Scope> owner;
        private final UniqueId id;
        private final Supplier<TestLaunchSnapshot> snapshot;
        private Scope(List<Scope> owner, UniqueId id, Supplier<TestLaunchSnapshot> snapshot) {
            this.owner = owner; this.id = id; this.snapshot = snapshot;
        }
        @Override public void close() {
            synchronized (owner) {
                owner.remove(this);
                if (owner.isEmpty() && SCOPES.get() == owner) SCOPES.remove();
            }
        }
    }
}
