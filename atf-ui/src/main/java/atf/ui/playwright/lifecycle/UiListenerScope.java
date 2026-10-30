package atf.ui.playwright.lifecycle;

import java.util.*;

/** One browser observer owns an atomic attachment and a complete, one-shot withdrawal. */
public final class UiListenerScope implements AutoCloseable {
    private record Binding(Runnable attach, Runnable detach) { }
    private final List<Binding> bindings = new ArrayList<>();
    private boolean attaching;
    private boolean active;
    private boolean closed;

    public UiListenerScope add(Runnable attach, Runnable detach) {
        if (closed || attaching || active) throw new IllegalStateException("Listener scope is already acquired or closed");
        bindings.add(new Binding(Objects.requireNonNull(attach), Objects.requireNonNull(detach)));
        return this;
    }

    /** Callbacks must check this before accessing driver objects, including during partial acquisition. */
    public boolean active() { return active && !closed; }

    public void attach() {
        if (closed || attaching || active) throw new IllegalStateException("Listener scope cannot be acquired twice");
        attaching = true;
        try {
            for (var binding : bindings) {
                binding.attach().run();
                if (closed) throw new IllegalStateException("Listener scope closed during attachment");
            }
            active = true;
        } catch (RuntimeException | Error primary) {
            release(primary);
            throw primary;
        } finally {
            attaching = false;
        }
    }

    @Override public void close() { release(null); }

    private void release(Throwable primary) {
        if (closed) return;
        closed = true;
        active = false;
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        if (primary != null) seen.add(primary);
        Throwable failure = primary;
        try {
            for (var binding : bindings) {
                try { binding.detach().run(); }
                catch (RuntimeException | Error secondary) {
                    if (seen.add(secondary)) {
                        if (failure == null) failure = secondary;
                        else failure.addSuppressed(secondary);
                    }
                }
            }
        } finally {
            bindings.clear();
        }
        if (primary == null) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
        }
    }
}
