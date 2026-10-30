package atf.shop.tests.component.reporting;

import atf.shop.tests.support.ShopComponentTest;


import atf.testkit.AtfComponentTest;
import atf.testkit.AtfUnitTest;
import atf.testkit.reporting.AtfReportPath;
import atf.testkit.scope.TestRunScope;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.suite.api.*;
import java.nio.file.*;
import java.util.*;
import static atf.testkit.assertions.AuditedAssertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

/** Consumer wiring over real Jupiter owners; intentional failure fixtures never call the remote store. */
@ShopComponentTest
@ResourceLock("java.lang.System.properties")
class ShopComponentReportScopeComponentTest {
    private static final String ENABLED = "atf.shop.component-scope.fixture";

    @ParameterizedTest @CsvSource({"18,false", "18,true", "240,false", "240,true", "480,false", "480,true"})
    void theConfiguredPreviewReachesTheCollectorIndividuallyAndThroughASuite(int limit, boolean suite) throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "preview-config", UUID.randomUUID().toString()).toAbsolutePath());
        Path config = directory.resolve("atf.yaml");
        Files.writeString(config, limit == 240 ? "reporting: {}\n"
                : "reporting:\n  assertionMaxDisplayCharacters: " + limit + "\n");
        var run = launch(null, false, Map.of("atf.report.config", config.toString()), suite
                ? selectClass(PreviewSuiteFixture.class) : selectMethod(PreviewFixture.class, "differentSuffix"));
        assertEquals(1, run.summary().getTestsFailedCount(), "A difference beyond the preview must remain FAIL");
        assertEquals(0, run.summary().getContainersFailedCount(), "Only the controlled comparison fails");
        assertEquals(1, run.reports().size(), "One artifact per declared owner");
        String html = run.html();
        assertTrue(html.contains("x".repeat(limit - 1) + "…"), "Collector uses the resolved budget, including values above 240");
        assertFalse(html.contains("x".repeat(600)), "No raw value dump");
        assertTrue(html.contains("Assertion preview") && html.contains(limit + " characters"), "Effective System setting");
    }

    @Suite @AtfComponentTest @SelectClasses(PreviewFixture.class)
    @ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
    public static class PreviewSuiteFixture { }

    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class PreviewFixture {
        @Test void differentSuffix() {
            String prefix = "x".repeat(600);
            assertEquals(prefix + "expected", prefix + "actual", "Full values differ beyond the displayed prefix");
        }
    }

    @Test void actualSuiteOwnsOneArtifactIncludingParameterizedInvocations() throws Exception {
        var run = launch(null, false, selectClass(CombinedFixture.class));
        assertEquals(4, run.summary().getTestsSucceededCount());
        assertEquals(0, run.summary().getContainersFailedCount());
        assertEquals(1, run.reports().size());
        String html = run.html();
        assertTrue(html.contains("FirstFixture") && html.contains("ParametersFixture"));
        assertTrue(html.contains("same parameter title"));
        assertTrue(html.contains("Component assertions"));
        assertEquals(1L, run.console().lines().filter(line -> line.contains("Extent report:")).count());
        assertTrue(run.console().contains(run.reports().getFirst().toAbsolutePath().toString()));
    }

    @Test void individualMethodClassAndRepeatedLaunchesOwnTheirArtifacts() throws Exception {
        var method = launch(null, false, selectMethod(FirstFixture.class, "alpha"));
        assertEquals(1, method.summary().getTestsSucceededCount());
        assertEquals(1, method.reports().size());
        var classes = launch(null, false, selectClass(FirstFixture.class), selectClass(ParametersFixture.class));
        assertEquals(4, classes.summary().getTestsSucceededCount());
        assertEquals(2, classes.reports().size());
        var first = launch(null, false, selectClass(CombinedFixture.class));
        var second = launch(null, false, selectClass(CombinedFixture.class));
        assertEquals(1, first.reports().size());
        assertEquals(1, second.reports().size());
        assertFalse(first.reports().getFirst().equals(second.reports().getFirst()));
        assertEquals(0L, method.console().lines().filter(line -> line.contains("Extent report:")).count());
    }

    @Test void mixedResultsKeepTheFailureAndAbortInTheSharedArtifact() throws Exception {
        var run = launch("suite", false, selectClass(FirstFixture.class), selectClass(MixedFixture.class));
        assertEquals(3, run.summary().getTestsSucceededCount());
        assertEquals(1, run.summary().getTestsFailedCount());
        assertEquals(1, run.summary().getTestsAbortedCount());
        assertEquals(0, run.summary().getContainersFailedCount());
        assertEquals(1, run.reports().size());
        assertTrue(run.html().contains("controlled component failure"));
        assertTrue(run.html().contains("controlled assumption"));
        assertTrue(run.html().contains("MixedFixture") && run.html().contains("FirstFixture"));
    }

    @Test void classLifecycleFailuresRemainVisibleWithoutFabricatedAssertions() throws Exception {
        var setup = launch("suite", false, selectClass(BeforeAllFixture.class), selectClass(FirstFixture.class));
        assertEquals(1, setup.summary().getContainersFailedCount());
        assertEquals(2, setup.summary().getTestsSucceededCount());
        assertEquals(1, setup.reports().size());
        assertTrue(setup.html().contains("controlled class setup"));
        assertTrue(setup.html().contains("Class lifecycle") && setup.html().contains("SETUP"));
        var teardown = launch("suite", false, selectClass(AfterAllFixture.class));
        assertEquals(1, teardown.summary().getContainersFailedCount());
        assertEquals(1, teardown.summary().getTestsSucceededCount());
        assertEquals(1, teardown.reports().size());
        assertTrue(teardown.html().contains("controlled class teardown"));
        assertTrue(teardown.html().contains("TEARDOWN"));
    }

    @Test void concurrentClassesKeepOneReportAndIndependentInvocations() throws Exception {
        var run = launch("suite", true, selectClass(FirstFixture.class), selectClass(ParametersFixture.class));
        assertEquals(4, run.summary().getTestsSucceededCount());
        assertEquals(0, run.summary().getContainersFailedCount());
        assertEquals(1, run.reports().size());
        assertTrue(run.html().contains("FirstFixture") && run.html().contains("ParametersFixture"));
    }

    @Test void distinctSuitesInOneLauncherKeepSeparateReportOwners() throws Exception {
        var run = launch(null, false, selectClass(CombinedFixture.class), selectClass(AlternateFixture.class));
        assertEquals(6, run.summary().getTestsSucceededCount());
        assertEquals(0, run.summary().getContainersFailedCount());
        assertEquals(2, run.reports().size());
        assertEquals(1L, run.console().lines().filter(line -> line.contains("Extent report:")).count());
    }

    @Test void unitSelectionRemainsOutsideTheComponentAudit() throws Exception {
        var run = launch("suite", false, selectClass(FirstFixture.class), selectClass(UnitFixture.class));
        assertEquals(3, run.summary().getTestsSucceededCount());
        assertEquals(1, run.reports().size());
        assertFalse(run.html().contains("UnitFixture"));
    }

    private record Run(TestExecutionSummary summary, List<Path> reports, String console) {
        String html() throws Exception { return Files.readString(reports.getFirst()); }
    }

    private static Run launch(String scope, boolean parallel, DiscoverySelector... selectors) throws Exception {
        return launch(scope, parallel, Map.of(), selectors);
    }

    private static Run launch(String scope, boolean parallel, Map<String, String> overrides, DiscoverySelector... selectors) throws Exception {
        Path output = Files.createDirectories(Path.of("target", "component-report-scope", UUID.randomUUID().toString()).toAbsolutePath());
        String reportProperty = "atf.practice.api-report-directory";
        var properties = new HashMap<>(Map.of(reportProperty, output.toString(), ENABLED, "true"));
        properties.putAll(overrides);
        var previous = new HashMap<String, String>();
        properties.forEach((key, value) -> previous.put(key, System.getProperty(key)));
        try {
            properties.forEach(System::setProperty);
            var observation = atf.console.capture.AtfConsoleOutputCapture.capture(() -> {
            var request = LauncherDiscoveryRequestBuilder.request().selectors(selectors)
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", Boolean.toString(parallel))
                    .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                    .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent");
            if (scope != null) request.configurationParameter(TestRunScope.CONFIGURATION_PARAMETER, scope);
            var listener = new SummaryGeneratingListener();
            LauncherFactory.create().execute(request.build(), listener);
            return listener.getSummary();
            }, line -> { });
            Path directory = output.resolve("component");
            List<Path> files;
            try (var reports = Files.list(directory)) {
                files = reports.filter(file -> file.toString().endsWith(".html")).toList();
            }
            return new Run(observation.result(), files, observation.output());
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
    }

    @Suite @AtfComponentTest
    @SelectClasses(FirstFixture.class)
    @ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
    public static class AlternateFixture { }

    @Suite @AtfComponentTest
    @SelectClasses({FirstFixture.class, ParametersFixture.class})
    @ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
    public static class CombinedFixture { }

    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    @AtfReportPath(AtfReportPath.Policy.DISABLED)
    public static class FirstFixture {
        @Test void alpha() { assertEquals("alpha", "alpha"); }
        @Test void beta() { assertEquals("beta", "beta"); }
    }

    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true") @AtfReportPath
    public static class ParametersFixture {
        @ParameterizedTest(name = "same parameter title") @ValueSource(ints = {1, 2})
        void duplicateTitle(int value) { assertTrue(value > 0); }
    }

    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class MixedFixture {
        @Test void pass() { assertTrue(true); }
        @Test void fail() { assertEquals(1, 2, "controlled component failure"); }
        @Test void skip() { Assumptions.assumeTrue(false, "controlled assumption"); }
    }

    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class BeforeAllFixture {
        @BeforeAll static void setup() { throw new IllegalStateException("controlled class setup"); }
        @Test void unreachable() { throw new AssertionError("must not run"); }
    }

    @ShopComponentTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class AfterAllFixture {
        @Test void body() { assertTrue(true); }
        @AfterAll static void teardown() { throw new IllegalStateException("controlled class teardown"); }
    }

    @AtfUnitTest @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    public static class UnitFixture {
        @Test void unit() { if (!getClass().getSimpleName().equals("UnitFixture")) throw new AssertionError("unit fixture identity"); }
    }
}
