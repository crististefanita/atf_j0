package atf.autoex.tests.component.reporting;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;
import java.util.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Real browser and E2E resource ownership, using only about:blank. */
@AutoExComponentTest
class AutoExE2eReportPathComponentTest {
    @Test void sharedReportIncludesEveryOwnerWithoutLeakingAcrossLaunches() throws Exception {
        verify(true, 1, DisabledFirst.class, EnabledLast.class);
        verify(true, 1, EnabledFirst.class, DisabledLast.class);
        verify(true, 0, DisabledFirst.class, DisabledLast.class);
        verify(true, 1, EnabledLast.class);
        verify(false, 0, EnabledLast.class);
    }

    private static void verify(boolean console, int expected, Class<?>... owners) throws Exception {
        Path root = atf.autoex.runtime.config.AutoExProjectRoot.discover();
        Path directory = Files.createDirectories(root.resolve("target/report-path-e2e/" + UUID.randomUUID()));
        Path config = directory.resolve("atf.yaml");
        Files.writeString(config, "reporting:\n  console:\n    enabled: " + console + "\n");
        var yaml = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        var profile = yaml.readTree(atf.testkit.files.AtfTestResourceReader.readResourceUtf8(
                AutoExE2eReportPathComponentTest.class, atf.autoex.config.AutoExConfigurationLoader.DEFAULT_RESOURCE));
        ((com.fasterxml.jackson.databind.node.ObjectNode) profile.path("automationExercise").path("reports"))
                .put("uiBaseDirectory", root.relativize(directory.resolve("reports")).toString());
        Path consumerConfig = directory.resolve("auto-ex.yaml");
        yaml.writeValue(consumerConfig.toFile(), profile);
        String consumerProperty = atf.autoex.config.AutoExConfigurationLoader.CONFIG_PROPERTY;
        String oldConsumer = System.getProperty(consumerProperty);
        var before = reports(directory);
        String oldConfig = System.getProperty("atf.report.config"), oldEnabled = System.getProperty("atf.autoex.e2e.enabled");
        var premature = new ArrayList<String>();
        var listener = new SummaryGeneratingListener();
        atf.console.capture.AtfConsoleOutputCapture.Observation<Void> observation;
        try {
            observation = atf.console.capture.AtfConsoleOutputCapture.capture(() -> {
                System.setProperty("atf.report.config", config.toString());
                System.setProperty("atf.autoex.e2e.enabled", "true");
                System.setProperty(consumerProperty, consumerConfig.toString());
                var request = LauncherDiscoveryRequestBuilder.request()
                    .configurationParameter("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$OrderAnnotation");
                for (Class<?> owner : owners) request.selectors(selectClass(owner));
                LauncherFactory.create().execute(request.build(), listener);
                return null;
            }, line -> {
                if (line.contains("Extent report: ") && !Files.isRegularFile(Path.of(line.substring(line.indexOf("Extent report: ") + 15))))
                    premature.add(line);
            });
        } finally {
            restore("atf.report.config", oldConfig); restore("atf.autoex.e2e.enabled", oldEnabled); restore(consumerProperty, oldConsumer);
        }
        var summary = listener.getSummary();
        assertEquals(0, summary.getContainersFailedCount(), summary.getFailures().toString());
        assertEquals(owners.length, summary.getTestsSucceededCount(), summary.getFailures().toString());
        var created = reports(directory); created.removeAll(before);
        assertEquals(1, created.size(), "one report resource for the whole launcher");
        var paths = observation.output().lines().filter(line -> line.contains("Extent report: "))
                .map(line -> Path.of(line.substring(line.indexOf("Extent report: ") + 15))).toList();
        assertEquals(expected, paths.size());
        assertTrue(premature.isEmpty(), "publication must precede notification");
        if (expected == 1) assertEquals(created.iterator().next(), paths.getFirst());
        String html = Files.readString(created.iterator().next());
        for (Class<?> owner : owners) assertTrue(html.contains(owner.getSimpleName()));
        assertTrue(html.contains("Local fixture"));
    }
    private static Set<Path> reports(Path directory) throws Exception {
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".html"))
                    .filter(p -> !p.toString().contains(".atf-extent-session-"))
                    .map(p -> p.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        }
    }
    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
    }
    public abstract static class Case {
        @Test void offline(AutoExE2eRun run) {
            String actual = run.currentUrl();
            run.checkpoint(new AutoExTraceStep() {
                public AtfGherkinKeyword keyword() { return AtfGherkinKeyword.THEN; }
                public String title() { return "local fixture remains on about:blank"; }
            }, List.of(new AtfAssertion("Local fixture", "about:blank", actual, "about:blank".equals(actual))));
        }
    }
    @Order(1) @AutoExE2eTest @AtfTestChannel(TestChannel.UI) @AtfReportPath(AtfReportPath.Policy.DISABLED)
    public static class DisabledFirst extends Case { }
    @Order(2) @atf.testkit.AtfE2eTest @AtfTestChannel(TestChannel.UI) @AtfReportPath
    @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF)
    @org.junit.jupiter.api.extension.ExtendWith({atf.console.junit.AtfConsoleTestExtension.class,
            atf.autoex.tests.extension.AutoExE2eExtension.class})
    public static class EnabledLast extends Case { }
    @Order(1) @AutoExE2eTest @AtfTestChannel(TestChannel.UI)
    public static class EnabledFirst extends Case { }
    @Order(2) @AutoExE2eTest @AtfTestChannel(TestChannel.UI) @AtfReportPath(AtfReportPath.Policy.DISABLED)
    public static class DisabledLast extends Case { }
}
