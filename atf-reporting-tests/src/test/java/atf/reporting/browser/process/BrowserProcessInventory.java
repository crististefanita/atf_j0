package atf.reporting.browser.process;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/** Ownership sampling is bounded independently of the watchdog's deadline polling. */
public final class BrowserProcessInventory {
    private static final long INTERVAL = Duration.ofMillis(100).toNanos();
    private final ProcessHandle root;
    private final LongSupplier clock;
    private final Map<Long, OwnedProcess> handles = new LinkedHashMap<>();
    private final Optional<Instant> rootStart;
    private long last;
    private boolean sampled;

    public BrowserProcessInventory(ProcessHandle root) { this(root, System::nanoTime); }
    public BrowserProcessInventory(ProcessHandle root, LongSupplier clock) {
        this.root = root;
        this.clock = clock;
        this.rootStart = start(root);
        handles.put(root.pid(), OwnedProcess.capture(root));
    }

    public void captureIfDue() {
        long now = clock.getAsLong();
        if (!sampled || now - last >= INTERVAL) { captureNow(); last = clock.getAsLong(); sampled = true; }
    }

    public void captureNow() {
        try (var descendants = root.descendants()) {
            descendants.filter(this::belongsToRoot)
                    .forEach(handle -> handles.putIfAbsent(handle.pid(), OwnedProcess.capture(handle)));
        }
    }

    private boolean belongsToRoot(ProcessHandle descendant) {
        if (rootStart.isEmpty()) return false;
        ProcessHandle current = descendant;
        Optional<Instant> childStart = start(current);
        if (childStart.isEmpty() || childStart.get().isBefore(rootStart.get())) return false;
        var visited = new java.util.HashSet<Long>();
        while (visited.add(current.pid())) {
            var parent = current.parent();
            if (parent.isEmpty()) return false;
            current = parent.get();
            Optional<Instant> parentStart = start(current);
            if (parentStart.isEmpty() || parentStart.get().isAfter(childStart.get())) return false;
            if (current.pid() == root.pid()) return rootStart.equals(parentStart);
            childStart = parentStart;
        }
        return false;
    }

    private static Optional<Instant> start(ProcessHandle handle) {
        return handle.info().startInstant();
    }

    public Map<Long, ProcessHandle> handles() {
        return handles.values().stream().collect(Collectors.toUnmodifiableMap(
                OwnedProcess::pid, OwnedProcess::handle));
    }

    public Map<Long, ProcessHandle> aliveHandles() {
        return handles.values().stream().filter(OwnedProcess::isSameLiveProcess)
                .collect(Collectors.toUnmodifiableMap(OwnedProcess::pid, OwnedProcess::handle));
    }

    public boolean isSameLiveProcess(ProcessHandle handle) {
        OwnedProcess owned = handles.get(handle.pid());
        return owned != null && owned.handle() == handle && owned.isSameLiveProcess();
    }

    private record OwnedProcess(ProcessHandle handle, Optional<Instant> startedAt) {
        private static OwnedProcess capture(ProcessHandle handle) {
            return new OwnedProcess(handle, start(handle));
        }

        private long pid() { return handle.pid(); }

        private boolean isSameLiveProcess() {
            return startedAt.isPresent() && handle.isAlive() && startedAt.equals(start(handle));
        }
    }
}
