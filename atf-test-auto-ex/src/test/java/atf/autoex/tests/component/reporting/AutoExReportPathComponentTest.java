package atf.autoex.tests.component.reporting;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.testkit.reporting.AtfReportPath;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;
import java.util.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Actual consumer publication; nested launchers keep the failed/aborted fixtures controlled. */
@AutoExComponentTest
class AutoExReportPathComponentTest {
    private static final String OUTCOME = "atf.fixture.reportpath.autoex.outcome";
    private static final String PREFIX = "Extent report: ";

    @Test void explicitPolicyAndGlobalConsoleSwitchControlOnlyTheNotification() throws Exception {
        verify(Disabled.class, "pass", true, 1, 0, 0, 1, 0);
        verify(Enabled.class, "pass", true, 1, 0, 0, 1, 1);
        verify(Default.class, "pass", true, 1, 0, 0, 1, 0);
        verify(Enabled.class, "pass", false, 1, 0, 0, 1, 0);
        verify(Enabled.class, "pass", true, 1, 0, 0, 1, 1);
        verify(Disabled.class, "pass", true, 1, 0, 0, 1, 0);
    }

    @Test void terminalOutcomesTemplatesAndSuiteKeepTheExactPublishedArtifact() throws Exception {
        verify(Enabled.class, "fail", true, 0, 1, 0, 1, 1);
        verify(Enabled.class, "abort", true, 0, 0, 1, 1, 1);
        verify(Enabled.class, "setup", true, 0, 1, 0, 1, 1);
        verify(Enabled.class, "teardown", true, 0, 1, 0, 1, 1);
        verify(Parameters.class, "pass", true, 2, 0, 0, 1, 1);
        verify(Selected.class, "pass", true, 2, 0, 0, 1, 1);
    }

    private static void verify(Class<?> fixture, String outcome, boolean console,
                               int passed, int failed, int aborted, int artifacts, int notifications) throws Exception {
        Path root = atf.autoex.runtime.config.AutoExProjectRoot.discover();
        Path directory = Files.createDirectories(root.resolve("target/report-path-proof/" + UUID.randomUUID()));
        Path config = directory.resolve("atf.yaml");
        Files.writeString(config, "reporting:\n  console:\n    enabled: " + console + "\n    detailsEnabled: false\n");
        var before = reports(directory);
        String oldConfig = System.getProperty("atf.report.config"), oldOutcome = System.getProperty(OUTCOME);
        String outputProperty = atf.autoex.tests.extension.AutoExExtentTestExtension.REPORT_DIRECTORY_PROPERTY;
        String oldOutput = System.getProperty(outputProperty);
        var premature = new ArrayList<String>();
        var listener = new SummaryGeneratingListener();
        atf.console.capture.AtfConsoleOutputCapture.Observation<Void> observation;
        try {
            observation = atf.console.capture.AtfConsoleOutputCapture.capture(() -> {
                System.setProperty("atf.report.config", config.toString());
                System.setProperty(OUTCOME, outcome);
                System.setProperty(outputProperty, root.relativize(directory.resolve("reports")).toString());
                LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(fixture)).build(), listener);
                return null;
            }, line -> {
                if (line.contains(PREFIX) && !Files.isRegularFile(Path.of(line.substring(line.indexOf(PREFIX) + PREFIX.length()))))
                    premature.add(line);
            });
        } finally {
            restore("atf.report.config", oldConfig); restore(OUTCOME, oldOutcome); restore(outputProperty, oldOutput);
        }
        var summary = listener.getSummary();
        assertEquals(0, summary.getContainersFailedCount(), summary.getFailures().toString());
        assertEquals(passed, summary.getTestsSucceededCount());
        assertEquals(failed, summary.getTestsFailedCount());
        assertEquals(aborted, summary.getTestsAbortedCount());
        var created = reports(directory); created.removeAll(before);
        assertEquals(artifacts, created.size(), "notification policy cannot change report creation");
        var paths = observation.output().lines().filter(line -> line.contains(PREFIX))
                .map(line -> Path.of(line.substring(line.indexOf(PREFIX) + PREFIX.length()))).toList();
        assertEquals(notifications, paths.size());
        assertEquals(notifications, new HashSet<>(paths).size(), "one notification per artifact");
        assertTrue(premature.isEmpty(), "notification precedes final artifact: " + premature);
        for (var path : paths) {
            assertTrue(path.isAbsolute()); assertEquals(path.normalize(), path);
            assertTrue(created.contains(path));
        }
        for (var path : created) {
            String html = Files.readString(path);
            assertTrue(html.contains(fixture == Selected.class ? "Enabled" : fixture.getSimpleName())
                    || (fixture == Selected.class && html.contains("Disabled")));
            if (Set.of("pass", "fail", "teardown").contains(outcome)) assertTrue(html.contains("Component assertions"));
        }
    }

    private static Set<Path> reports(Path directory) throws Exception {
        if (!Files.exists(directory)) return new HashSet<>();
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".html"))
                    .filter(p -> !p.toString().contains(".atf-extent-session-"))
                    .map(p -> p.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        }
    }
    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
    }

    @AutoExComponentTest
    public abstract static class Case {
        @BeforeEach void setup() { if ("setup".equals(System.getProperty(OUTCOME))) throw new IllegalStateException("controlled setup"); }
        @Test void audited() {
            if ("abort".equals(System.getProperty(OUTCOME))) Assumptions.abort("controlled abort");
            assertEquals("expected", "fail".equals(System.getProperty(OUTCOME)) ? "wrong" : "expected");
        }
        @AfterEach void teardown() { if ("teardown".equals(System.getProperty(OUTCOME))) throw new IllegalStateException("controlled teardown"); }
    }
    @AutoExComponentTest @AtfReportPath @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF)
    public static class Enabled extends Case { }
    @AutoExComponentTest @AtfReportPath(AtfReportPath.Policy.DISABLED)
    public static class Disabled extends Case { }
    @AutoExComponentTest public static class Default extends Case { }
    @AutoExComponentTest @AtfReportPath public static class Parameters {
        @ParameterizedTest @ValueSource(ints = {1, 2}) void audited(int value) { assertTrue(value > 0); }
    }
    @Suite @AtfComponentTest @SelectClasses({Enabled.class, Disabled.class})
    @org.junit.platform.suite.api.ConfigurationParameter(key = atf.testkit.scope.TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
    public static class Selected { }
}
