package atf.testkit.launch;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.IncludeTags;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

@AtfComponentTest
class TestLaunchPlanComponentTest {
    private static final String KEY = "atf.testkit.launch.fixture";
    private static final Map<String, List<TestLaunchSnapshot>> OBSERVED = new ConcurrentHashMap<>();

    @Test void directClassMethodAndFilteredSuiteUseDiscoveryRatherThanExecutionTotals() {
        verify(selectClass(First.class), 2, "class: " + First.class.getName());
        verify(selectMethod(First.class, "one"), 1, "method: " + First.class.getName() + "#one()");
        verify(selectClass(CanonicalSuite.class), 3, "class: " + CanonicalSuite.class.getName());
        verify(selectClass(FilteredSuite.class), 2, "class: " + FilteredSuite.class.getName());
    }

    @Test void nestedIndependentLaunchRestoresItsOuterInvocation() {
        var observations = verify(selectClass(NestedLauncher.class), 1, "class: " + NestedLauncher.class.getName());
        assertEquals(2, observations.size(), "capture before and after the independent launch");
        assertEquals(observations.getFirst(), observations.getLast());
    }

    @Test void concurrentLaunchesWithIdenticalIdsDoNotShareSelection() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> verify(selectMethod(First.class, "one"), 1,
                    "method: " + First.class.getName() + "#one()"));
            var b = pool.submit(() -> verify(selectClass(First.class), 2, "class: " + First.class.getName()));
            a.get(); b.get();
        }
    }

    @Test void parallelTestCallbacksRetainOneImmutableDiscoveryInventory() {
        var result = launch(selectClass(First.class), true);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(snapshot -> snapshot.discoveredTestCount() == 2));
        assertThrows(UnsupportedOperationException.class, () -> result.getFirst().selectors().clear());
    }

    @Test void multipleDiscoveredPlansKeepTheirOwnRequestIdentity() {
        String key = UUID.randomUUID().toString();
        OBSERVED.put(key, new CopyOnWriteArrayList<>());
        try (var session = LauncherFactory.openSession()) {
            var launcher = session.getLauncher();
            var method = launcher.discover(request(selectMethod(First.class, "one"), key, false));
            var type = launcher.discover(request(selectClass(First.class), key, false));
            launcher.execute(method);
            launcher.execute(type);
            var snapshots = OBSERVED.get(key);
            assertEquals(3, snapshots.size());
            assertEquals(List.of("method: " + First.class.getName() + "#one()"), snapshots.get(0).selectors());
            assertEquals(1, snapshots.get(0).discoveredTestCount());
            assertEquals(List.of("class: " + First.class.getName()), snapshots.get(1).selectors());
            assertEquals(2, snapshots.get(1).discoveredTestCount());
        } finally { OBSERVED.remove(key); }
    }

    @Test void aPlanExecutedInAnotherSessionNeverBorrowsAnUnrelatedRequest() {
        String key = UUID.randomUUID().toString();
        OBSERVED.put(key, new CopyOnWriteArrayList<>());
        try {
            var plan = LauncherFactory.create().discover(request(selectMethod(First.class, "one"), key, false));
            try (var session = LauncherFactory.openSession()) {
                session.getLauncher().discover(request(selectClass(Second.class), key, false));
                session.getLauncher().execute(plan);
            }
            var snapshot = OBSERVED.get(key).getFirst();
            assertTrue(snapshot.selectors().isEmpty());
            assertEquals("TestPlan (request unavailable in this session)", snapshot.selectionSource());
            assertEquals(1, snapshot.discoveredTestCount());
            assertTrue(snapshot.discoveredTests().getFirst().source().contains(First.class.getName()));
        } finally { OBSERVED.remove(key); }
    }

    @Test void dynamicRegistrationIsNotPresentedAsAStaticDiscoveryForecast() {
        for (Class<?> owner : List.of(DynamicFixture.class, DynamicSuite.class)) {
            var snapshots = launch(selectClass(owner), false);
            assertEquals(3, snapshots.size());
            assertTrue(snapshots.stream().allMatch(value -> value.discoveredTestCount() == 0));
            assertEquals(2, snapshots.getLast().registeredDynamicTestCount());
        }
    }

    @Test void disabledTestsRemainInTheDiscoveryCount() {
        var snapshots = verify(selectClass(DisabledFixture.class), 2, "class: " + DisabledFixture.class.getName());
        assertEquals(1, snapshots.size(), "Only the enabled invocation captures metadata");
    }

    @Test void aDisabledBridgeNeverReusesTheEnclosingLaunch() {
        try (var session = LauncherFactory.openSession(LauncherConfig.builder()
                .enableLauncherSessionListenerAutoRegistration(false).build())) {
            var summary = new SummaryGeneratingListener();
            session.getLauncher().execute(LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(NoBridgeFixture.class)).build(), summary);
            assertEquals(0, summary.getSummary().getTotalFailureCount(), summary.getSummary().getFailures().toString());
        }
    }

    private static List<TestLaunchSnapshot> verify(org.junit.platform.engine.DiscoverySelector selector,
                                                   long count, String expectedSelector) {
        var snapshots = launch(selector, false);
        assertFalse(snapshots.isEmpty());
        for (var snapshot : snapshots) {
            assertEquals(count, snapshot.discoveredTestCount());
            assertEquals(List.of(expectedSelector), snapshot.selectors());
            assertEquals("launcher request", snapshot.selectionSource());
        }
        return snapshots;
    }

    private static List<TestLaunchSnapshot> launch(org.junit.platform.engine.DiscoverySelector selector, boolean parallel) {
        String key = UUID.randomUUID().toString();
        OBSERVED.put(key, new CopyOnWriteArrayList<>());
        try {
            var summary = new SummaryGeneratingListener();
            LauncherFactory.create().execute(request(selector, key, parallel), summary);
            assertEquals(0, summary.getSummary().getTotalFailureCount(), summary.getSummary().getFailures().toString());
            return List.copyOf(OBSERVED.get(key));
        } finally { OBSERVED.remove(key); }
    }

    private static org.junit.platform.launcher.LauncherDiscoveryRequest request(
            org.junit.platform.engine.DiscoverySelector selector, String key, boolean parallel) {
        return LauncherDiscoveryRequestBuilder.request().selectors(selector)
                .configurationParameter(KEY, key)
                .configurationParameter("junit.jupiter.execution.parallel.enabled", Boolean.toString(parallel))
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent").build();
    }

    public static final class Capture implements BeforeEachCallback {
        @Override public void beforeEach(ExtensionContext context) { capture(context); }
        static void capture(ExtensionContext context) {
            String key = context.getConfigurationParameter(KEY).orElseThrow();
            OBSERVED.get(key).add(TestLaunchContext.capture(context).orElseThrow());
        }
    }

    @ExtendWith(Capture.class) @Tag("selected")
    public static class First {
        @Test void one() { }
        @Test void two() { }
    }
    @ExtendWith(Capture.class)
    public static class Second { @Test void one() { } }
    @ExtendWith(Capture.class)
    public static class DisabledFixture {
        @Test void enabled() { }
        @Test @Disabled void disabled() { }
    }

    @ExtendWith(NoBridgeCapture.class)
    public static class NoBridgeFixture { @Test void local() { } }
    public static final class NoBridgeCapture implements BeforeEachCallback {
        @Override public void beforeEach(ExtensionContext context) {
            assertTrue(TestLaunchContext.capture(context).isEmpty(), "No stale outer launch is available");
        }
    }
    @Suite @SelectClasses({First.class, Second.class})
    public static class CanonicalSuite { }
    @Suite @SelectClasses({First.class, Second.class}) @IncludeTags("selected")
    public static class FilteredSuite { }
    @Suite @SelectClasses(DynamicFixture.class)
    public static class DynamicSuite { }

    @ExtendWith(Capture.class)
    public static class NestedLauncher {
        @Test void nested() {
            verify(selectClass(Second.class), 1, "class: " + Second.class.getName());
            currentSnapshot().ifPresent(snapshot -> OBSERVED.values().stream()
                    .filter(values -> !values.isEmpty() && values.getFirst().selectors().equals(snapshot.selectors()))
                    .findFirst().orElseThrow().add(snapshot));
        }
    }
    private interface ExtensionContextNotInjected { }

    @ExtendWith(Capture.class)
    public static class DynamicFixture {
        @TestFactory Stream<DynamicTest> generate() {
            return Stream.of(DynamicTest.dynamicTest("first", () -> captureDynamic()),
                    DynamicTest.dynamicTest("second", () -> captureDynamic()));
        }
        private static void captureDynamic() {
            var snapshot = currentSnapshot().orElseThrow();
            OBSERVED.values().stream().filter(values -> !values.isEmpty()
                    && values.getFirst().discoveredTests().isEmpty()).findFirst().orElseThrow().add(snapshot);
        }
    }

    private static java.util.Optional<TestLaunchSnapshot> currentSnapshot() {
        return TestLaunchContext.current();
    }
}
