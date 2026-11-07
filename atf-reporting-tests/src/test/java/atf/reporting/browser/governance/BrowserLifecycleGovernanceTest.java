package atf.reporting.browser.governance;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.testkit.AtfComponentTest;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** New browser paths must acquire resources at the bounded owner and execute under a registered worker. */
@AtfComponentTest
@Tag("report-browser")
class BrowserLifecycleGovernanceTest {
    private static Map<String, BrowserSourceInventory.Entry> inventory;

    private static synchronized Map<String, BrowserSourceInventory.Entry> inventory() throws Exception {
        if (inventory == null) inventory = BrowserSourceInventory.read(sourceRoot());
        return inventory;
    }

    @Test void everySdkFactoryRemainsAtItsBoundedOwner() throws Exception {
        var entries = inventory();
        var violations = entries.values().stream().flatMap(entry -> entry.violations().stream()).toList();
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    @Test void everyBrowserExecutableHasAnExplicitIsolationBoundary() throws Exception {
        var entries = inventory();
        var violations = new ArrayList<String>();
        int count = 0;
        for (var entry : entries.values()) {
            if (entry.abstractType() || entry.annotations().contains("AtfUnitTest") || !BrowserSourceInventory.tests(entry, entries, new HashSet<>())) continue;
            if (!BrowserSourceInventory.browser(entry, entries, new HashSet<>())) continue;
            count++;
            Class<?> type = Class.forName(entry.name(), false, getClass().getClassLoader());
            if (!type.isAnnotationPresent(ReportBrowserTest.class)) violations.add(entry.name());
        }
        assertTrue(count >= 60, "The inventory must include direct SDK probes and transitive audit adapters");
        assertTrue(violations.isEmpty(), () -> "Browser entry without an isolated lifecycle: " + violations);
    }

    private static Path sourceRoot() {
        for (Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath(); root != null; root = root.getParent()) {
            Path source = root.resolve("atf-reporting-tests/src/test/java");
            if (Files.isDirectory(source)) return source;
        }
        throw new IllegalStateException("Reporting source inventory is unavailable");
    }
}
