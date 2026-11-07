package atf.reporting.browser.contract;

import atf.reporting.browser.process.BrowserProcessInventory;
import atf.testkit.AtfComponentTest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Controlled clocks and handles separate ownership guarantees from OS enumeration cost. */
@AtfComponentTest
@org.junit.jupiter.api.Tag("report-browser")
class BrowserProcessInventoryContractTest {
    @Test void frequentDeadlinePollingDoesNotRepeatedlyEnumerateTheOperatingSystem() {
        var clock = new AtomicLong(); var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var root = handle(1, new AtomicBoolean(true), new ArrayList<>(), scans, closes);
        var inventory = new BrowserProcessInventory(root, clock::get);
        for (int tick = 0; tick < 100; tick++) { clock.set(Duration.ofMillis(tick).toNanos()); inventory.captureIfDue(); }
        assertEquals(1, scans.get()); assertEquals(1, closes.get());
        clock.set(Duration.ofMillis(100).toNanos()); inventory.captureIfDue();
        assertEquals(2, scans.get()); assertEquals(2, closes.get());
    }

    @Test void theFailureBoundaryCapturesANewChildWithoutWaitingForTheNextSample() {
        var children = new ArrayList<ProcessHandle>(); var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var root = handle(1, new AtomicBoolean(true), children, scans, closes);
        var inventory = new BrowserProcessInventory(root, () -> 0); inventory.captureIfDue();
        var child = handle(2, new AtomicBoolean(true), List.of(), scans, closes); children.add(child);
        inventory.captureNow();
        assertSame(child, inventory.handles().get(2L)); assertEquals(2, scans.get()); assertEquals(2, closes.get());
    }

    @Test void aReusedPidReplacesOnlyTheDeadPreviouslyOwnedHandle() {
        var alive = new AtomicBoolean(true); var children = new ArrayList<ProcessHandle>();
        var scans = new AtomicInteger(); var closes = new AtomicInteger();
        var original = handle(2, alive, List.of(), scans, closes); children.add(original);
        var inventory = new BrowserProcessInventory(handle(1, new AtomicBoolean(true), children, scans, closes), () -> 0);
        inventory.captureNow(); alive.set(false);
        var replacement = handle(2, new AtomicBoolean(true), List.of(), scans, closes); children.set(0, replacement);
        inventory.captureNow(); assertSame(replacement, inventory.handles().get(2L));
        assertThrows(UnsupportedOperationException.class, () -> inventory.handles().clear());
    }

    private static ProcessHandle handle(long pid, AtomicBoolean alive, List<ProcessHandle> children, AtomicInteger scans, AtomicInteger closes) {
        return (ProcessHandle) java.lang.reflect.Proxy.newProxyInstance(ProcessHandle.class.getClassLoader(), new Class<?>[] {ProcessHandle.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "pid" -> pid;
                    case "isAlive" -> alive.get();
                    case "descendants" -> { scans.incrementAndGet(); yield List.copyOf(children).stream().onClose(closes::incrementAndGet); }
                    default -> throw new AssertionError("Unexpected handle operation: " + method);
                });
    }
}
