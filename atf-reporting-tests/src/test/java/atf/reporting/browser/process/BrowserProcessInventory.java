package atf.reporting.browser.process;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Ownership sampling is bounded independently of the watchdog's deadline polling. */
public final class BrowserProcessInventory {
    private static final long INTERVAL = Duration.ofMillis(100).toNanos();
    private final ProcessHandle root;
    private final LongSupplier clock;
    private final Map<Long, ProcessHandle> handles = new LinkedHashMap<>();
    private long last;
    private boolean sampled;

    public BrowserProcessInventory(ProcessHandle root) { this(root, System::nanoTime); }
    public BrowserProcessInventory(ProcessHandle root, LongSupplier clock) {
        this.root = root; this.clock = clock; handles.put(root.pid(), root);
    }

    public void captureIfDue() {
        long now = clock.getAsLong();
        if (!sampled || now - last >= INTERVAL) { captureNow(); last = clock.getAsLong(); sampled = true; }
    }

    public void captureNow() {
        try (var descendants = root.descendants()) {
            descendants.forEach(handle -> handles.compute(handle.pid(), (pid, previous) ->
                    previous == null || !previous.isAlive() ? handle : previous));
        }
    }

    public Map<Long, ProcessHandle> handles() { return Map.copyOf(handles); }
}
