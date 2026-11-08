package atf.reporting.browser.contract;

import atf.reporting.browser.process.BrowserProcessInventory;
import atf.testkit.AtfComponentTest;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Controlled process generations keep PID reuse out of the ownership boundary. */
@AtfComponentTest
@org.junit.jupiter.api.Tag("report-browser")
class BrowserProcessInventoryContractTest {
    @Test void frequentDeadlinePollingDoesNotRepeatedlyEnumerateTheOperatingSystem() {
        var clock = new AtomicLong(); var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var root = handle(1, Instant.EPOCH, null, new ArrayList<>(), scans, closes, new AtomicInteger());
        var inventory = new BrowserProcessInventory(root, clock::get);
        for (int tick = 0; tick < 100; tick++) { clock.set(Duration.ofMillis(tick).toNanos()); inventory.captureIfDue(); }
        assertEquals(1, scans.get()); assertEquals(1, closes.get());
        clock.set(Duration.ofMillis(100).toNanos()); inventory.captureIfDue();
        assertEquals(2, scans.get()); assertEquals(2, closes.get());
    }

    @Test void theFailureBoundaryCapturesANewChildWithoutWaitingForTheNextSample() {
        var children = new ArrayList<ProcessHandle>(); var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var root = handle(1, Instant.EPOCH, null, children, scans, closes, new AtomicInteger());
        var inventory = new BrowserProcessInventory(root, () -> 0); inventory.captureIfDue();
        var child = handle(2, Instant.ofEpochSecond(1), root, List.of(), scans, closes, new AtomicInteger());
        children.add(child); inventory.captureNow();
        assertSame(child, inventory.handles().get(2L)); assertEquals(2, scans.get()); assertEquals(2, closes.get());
    }

    @Test void aPreExistingProcessBehindAReusedParentPidIsNotOwned() {
        var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var descendants = new ArrayList<ProcessHandle>();
        var root = handle(1, Instant.ofEpochSecond(20), null, descendants, scans, closes, new AtomicInteger());
        var reusedParent = handle(2, Instant.ofEpochSecond(30), root, List.of(), scans, closes, new AtomicInteger());
        var preExistingAdb = handle(3, Instant.ofEpochSecond(10), reusedParent, List.of(), scans, closes, new AtomicInteger());
        descendants.addAll(List.of(reusedParent, preExistingAdb));
        var inventory = new BrowserProcessInventory(root, () -> 0); inventory.captureNow();
        assertTrue(inventory.handles().containsKey(2L));
        assertFalse(inventory.handles().containsKey(3L));
        assertFalse(inventory.aliveHandles().containsKey(3L));
    }

    @Test void aProcessWithoutARecordedStartIdentityIsNotOwned() {
        var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var children = new ArrayList<ProcessHandle>();
        var root = handle(1, Instant.EPOCH, null, children, scans, closes, new AtomicInteger());
        children.add(handle(2, null, root, List.of(), scans, closes, new AtomicInteger()));
        var inventory = new BrowserProcessInventory(root, () -> 0); inventory.captureNow();
        assertFalse(inventory.handles().containsKey(2L));
    }

    @Test void pidReuseCannotReplaceTheFirstCapturedProcessGeneration() {
        var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var rootChildren = new ArrayList<ProcessHandle>();
        var root = handle(1, Instant.EPOCH, null, rootChildren, scans, closes, new AtomicInteger());
        var activeStart = new AtomicReference<>(Instant.ofEpochSecond(1));
        var original = handleWithStartRef(2, activeStart, root, List.of(), scans, closes, new AtomicInteger());
        rootChildren.add(original);
        var inventory = new BrowserProcessInventory(root, () -> 0); inventory.captureNow();
        var replacement = handle(2, Instant.ofEpochSecond(2), root, List.of(), scans, closes, new AtomicInteger());
        activeStart.set(Instant.ofEpochSecond(2)); rootChildren.set(0, replacement); inventory.captureNow();
        assertSame(original, inventory.handles().get(2L));
        assertFalse(inventory.isSameLiveProcess(original));
        assertFalse(inventory.aliveHandles().containsKey(2L));
        assertThrows(UnsupportedOperationException.class, () -> inventory.handles().clear());
    }

    private static ProcessHandle handle(long pid, Instant started, ProcessHandle parent,
                                        List<ProcessHandle> children, AtomicInteger scans,
                                        AtomicInteger closes, AtomicInteger kills) {
        return handleWithStartRef(pid, new AtomicReference<>(started), parent, children, scans, closes, kills);
    }

    private static ProcessHandle handleWithStartRef(long pid, AtomicReference<Instant> reportedStart,
                                                    ProcessHandle parent, List<ProcessHandle> children,
                                                    AtomicInteger scans, AtomicInteger closes, AtomicInteger kills) {
        var alive = new AtomicBoolean(true);
        var info = (ProcessHandle.Info) Proxy.newProxyInstance(ProcessHandle.Info.class.getClassLoader(),
                new Class<?>[] {ProcessHandle.Info.class}, (proxy, method, arguments) ->
                        method.getName().equals("startInstant") ? Optional.ofNullable(reportedStart.get()) : Optional.empty());
        return (ProcessHandle) Proxy.newProxyInstance(ProcessHandle.class.getClassLoader(),
                new Class<?>[] {ProcessHandle.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "pid" -> pid;
                    case "isAlive" -> alive.get();
                    case "info" -> info;
                    case "parent" -> Optional.ofNullable(parent);
                    case "descendants" -> { scans.incrementAndGet(); yield List.copyOf(children).stream().onClose(closes::incrementAndGet); }
                    case "destroyForcibly" -> { kills.incrementAndGet(); alive.set(false); yield proxy; }
                    default -> throw new AssertionError("Unexpected handle operation: " + method);
                });
    }
}
