package atf.shop.tests.component.reporting;

import atf.shop.tests.support.ShopComponentTest;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.RecordedAssertions;
import atf.testkit.scope.TestRunScope;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.suite.api.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import static atf.testkit.assertions.AuditedAssertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

/** Verifies the real consumer adapter; the inner stopwatch supplies an independent lower bound. */
@ShopComponentTest
@ResourceLock("java.lang.System.properties")
class ShopComponentPhaseTimingComponentTest {
    private static final String ENABLED = "atf.shop.component-timing.fixture";
    private static final AtomicLong INNER_NANOS = new AtomicLong();
    private static final String SECRET = "timing-private-value-should-never-be-published";
    private static final Pattern MARKERS = Pattern.compile("<span[^>]*data-atf-execution-duration-ms[^>]*>");
    private record Run(String html, long wallNanos, long innerNanos) { }

    @Test void suiteMeasuresInheritedCallbacksAndDuplicateTitlesInOneReport() throws Exception {
        var run = launch(selectClass(TimingSuite.class), 3);
        var markers = MARKERS.matcher(run.html()).results().map(java.util.regex.MatchResult::group).toList();
        assertEquals(5, markers.size(), "three lifecycle phases plus two parameterized bodies");
        assertEquals(List.of(1L, 1L, 1L, 1L, 2L), markers.stream()
                .map(marker -> attribute(marker, "execution-intervals")).sorted().toList());
        long executionNanos = markers.stream().mapToLong(marker -> attribute(marker, "execution-duration-nanos")).sum();
        assertTrue(executionNanos >= run.innerNanos(), "execution encloses the fixture's inner stopwatch");
        assertTrue(executionNanos <= run.wallNanos(), "sequential callbacks fit inside the launcher lifetime");
        for (String marker : markers) {
            long nanos = attribute(marker, "execution-duration-nanos");
            assertEquals(nanos / 1_000_000 + (nanos % 1_000_000 == 0 ? 0 : 1), attribute(marker, "execution-duration-ms"));
            assertTrue(attribute(marker, "publication-duration-ms") >= 0);
        }
        assertFalse(run.html().contains(SECRET), "full values are redacted before timing publication");
        assertFalse(run.html().contains("bounded-safe-value-".repeat(40)), "240-character evidence policy is retained");
    }

    @Test void individualMethodAlsoPublishesItsOwnMeasuredBody() throws Exception {
        var run = launch(selectMethod(LifecycleFixture.class, "body", RecordedAssertions.class), 1);
        assertEquals(3L, MARKERS.matcher(run.html()).results().count());
        assertTrue(run.innerNanos() > 0);
    }

    private static long attribute(String marker, String name) {
        var match = Pattern.compile("data-atf-" + name + "=\"([0-9]+)\"").matcher(marker);
        if (!match.find()) throw new AssertionError("Missing typed timing attribute " + name);
        return Long.parseLong(match.group(1));
    }
    private static Run launch(org.junit.platform.engine.DiscoverySelector selector, int expected) throws Exception {
        Path output = Files.createDirectories(Path.of("target", "component-phase-timing", UUID.randomUUID().toString()).toAbsolutePath());
        String property = "atf.practice.api-report-directory";
        var previous = new HashMap<String, String>();
        previous.put(property, System.getProperty(property)); previous.put(ENABLED, System.getProperty(ENABLED));
        try {
            System.setProperty(property, output.toString()); System.setProperty(ENABLED, "true");
            INNER_NANOS.set(0);
            var summary = new SummaryGeneratingListener();
            long started = System.nanoTime();
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(selector)
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build(), summary);
            long elapsed = System.nanoTime() - started;
            assertEquals(expected, summary.getSummary().getTestsSucceededCount());
            assertEquals(0, summary.getSummary().getContainersFailedCount());
            try (var files = Files.list(output.resolve("component"))) {
                var reports = files.filter(file -> file.toString().endsWith(".html")).toList();
                assertEquals(1, reports.size(), "one report per owner");
                return new Run(Files.readString(reports.getFirst()), elapsed, INNER_NANOS.get());
            }
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
    }
    private static void measured(Runnable action) {
        long start = System.nanoTime();
        try { action.run(); } finally { INNER_NANOS.addAndGet(System.nanoTime() - start); }
    }

    @Suite @AtfComponentTest @SelectClasses({LifecycleFixture.class, ParametersFixture.class})
    @ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
    public static class TimingSuite { }

    public static class ParentFixture {
        @BeforeEach void inheritedSetup() { measured(() -> assertTrue(true, "parent setup")); }
    }
    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class LifecycleFixture extends ParentFixture {
        @BeforeEach void setup() { measured(() -> assertTrue(true, "child setup")); }
        @AfterEach void teardown() { measured(() -> assertTrue(true, "child teardown")); }
        @Test void body(RecordedAssertions audit) {
            measured(() -> {
                audit.equals("password", SECRET, SECRET);
                String safe = "bounded-safe-value-".repeat(40);
                audit.equals("safe long value", safe, safe);
            });
        }
    }
    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class ParametersFixture {
        @ParameterizedTest(name = "same timing title") @ValueSource(ints = {1, 2})
        void body(int value) { measured(() -> assertTrue(value > 0, "parameter evaluated")); }
    }
}

