package atf.reporting.browser.governance;

import atf.testkit.AtfComponentTest;
import java.util.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Cycles and shared dependencies cannot change ownership or make graph traversal revisit every path. */
@AtfComponentTest
@Tag("report-browser")
class BrowserReachabilityContractTest {
    @Test void cyclesWithoutAnSdkFactoryRemainOutsideTheBrowserInventory() {
        var entries = entries(type("A", false, false, false, "B"), type("B", false, false, false, "A"));
        assertFalse(browser("A", entries));
        assertFalse(browser("B", entries));
    }

    @Test void sharedAndCyclicDependenciesRetainTheReachableSdkOwner() {
        var entries = entries(type("A", false, false, false, "B", "C"), type("B", false, false, false, "C"),
                type("C", false, false, false, "B", "Sdk"), type("Sdk", true, false, false));
        assertTrue(browser("A", entries));
        assertTrue(browser("B", entries));
        assertTrue(browser("C", entries));
    }

    @Test void anExplicitWorkerKeepsItsCallerOutsideTheSdkOwnershipBoundary() {
        var entries = entries(type("Caller", false, false, false, "Worker"), type("Worker", false, true, false, "Sdk"), type("Sdk", true, false, false));
        assertFalse(browser("Caller", entries));
        assertFalse(browser("Worker", entries));
        assertTrue(browser("Sdk", entries));
    }

    @Test void annotationReferencesDoNotMakeTheirConsumersBrowserOwners() {
        var entries = entries(type("Caller", false, false, false, "Meta"), type("Meta", false, false, true, "Sdk"), type("Sdk", true, false, false));
        assertFalse(browser("Caller", entries));
        assertFalse(browser("Meta", entries));
        assertTrue(browser("Sdk", entries));
    }

    private static boolean browser(String name, Map<String, BrowserSourceInventory.Entry> entries) {
        return BrowserSourceInventory.browser(entries.get("fixture." + name), entries, new HashSet<>());
    }

    private static Map<String, BrowserSourceInventory.Entry> entries(BrowserSourceInventory.Entry... values) {
        var result = new LinkedHashMap<String, BrowserSourceInventory.Entry>();
        for (var value : values) result.put(value.name(), value);
        return result;
    }

    private static BrowserSourceInventory.Entry type(String name, boolean factory, boolean worker, boolean annotation, String... references) {
        return new BrowserSourceInventory.Entry("fixture." + name, "fixture", "", annotation, false, Set.of(),
                new LinkedHashSet<>(List.of(references)), List.of(), false, factory, worker, List.of());
    }
}
