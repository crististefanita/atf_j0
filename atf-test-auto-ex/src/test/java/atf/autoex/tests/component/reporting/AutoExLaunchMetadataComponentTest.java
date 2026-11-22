package atf.autoex.tests.component.reporting;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.reporting.AutoExLaunchMetadata;
import atf.autoex.tests.support.reporting.AutoExLaunchReportProbe;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.jupiter.api.Tag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static atf.testkit.assertions.AuditedAssertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

/** Real JUnit selection and report rendering; names never determine launch scope. */
@AutoExComponentTest
@ResourceLock("java.lang.System.properties")
class AutoExLaunchMetadataComponentTest {
    private static final String ENABLED = "atf.autoex.launch-metadata.fixture";
    private static final String OUTPUT = ENABLED + ".output";

    @ParameterizedTest
    @CsvSource({"CLASS,2", "METHOD,1", "SUITE,3", "FILTERED_SUITE,2"})
    void sourceIdentitySurvivesSelectionAndMisleadingNames(String selection, int expected) throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "launch-metadata", UUID.randomUUID().toString()).toAbsolutePath());
        String enabled = System.getProperty(ENABLED), output = System.getProperty(OUTPUT);
        var summary = new SummaryGeneratingListener();
        try {
            System.setProperty(ENABLED, "true");
            System.setProperty(OUTPUT, directory.toString());
            var selector = switch (selection) {
                case "CLASS" -> selectClass(First.class);
                case "METHOD" -> selectMethod(First.class, "local");
                case "SUITE" -> selectClass(CanonicalSuite.class);
                case "FILTERED_SUITE" -> selectClass(FilteredSuite.class);
                default -> throw new IllegalArgumentException(selection);
            };
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(selector)
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build(), summary);
        } finally {
            restore(ENABLED, enabled);
            restore(OUTPUT, output);
        }
        assertEquals(0, summary.getSummary().getContainersFailedCount(), summary.getSummary().getFailures().toString());
        assertEquals(expected, summary.getSummary().getTestsSucceededCount(), summary.getSummary().getFailures().toString());
        List<Path> reports;
        try (var paths = Files.list(directory)) {
            reports = paths.filter(path -> path.toString().endsWith(".html")).toList();
        }
        assertEquals(1, reports.size(), "One root-store report survives each independent launch");
        String html = Files.readString(reports.getFirst()).replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertTrue(html.contains(First.class.getName()), "Canonical source survives duplicate display titles");
        assertTrue(html.contains("local()"), "Method source is available without launcher logs");
        assertEquals(selection.equals("SUITE"), html.contains(Second.class.getName()), "Filtered selection excludes the other source");
        String suite = switch (selection) {
            case "SUITE" -> CanonicalSuite.class.getName();
            case "FILTERED_SUITE" -> FilteredSuite.class.getName();
            default -> "unknown (no suite ancestor exposed)";
        };
        assertTrue(html.contains(suite), "Suite identity comes from JUnit unique-ID ancestry");
        assertFalse(html.contains("unavailable (no active TestPlan)"), "The physical Kit service captures discovery");
        String requested = selection.equals("METHOD") ? "method: " + First.class.getName() + "#local()"
                : "class: " + (selection.equals("SUITE") ? CanonicalSuite.class.getName()
                : selection.equals("FILTERED_SUITE") ? FilteredSuite.class.getName() : First.class.getName());
        AutoExLaunchReportProbe.verify(reports.getFirst(), expected, requested);
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
    }

    public static final class MetadataExtension implements BeforeEachCallback, AfterEachCallback {
        private static final ExtensionContext.Namespace STORE = ExtensionContext.Namespace.create(MetadataExtension.class);
        @Override public void beforeEach(ExtensionContext context) {
            var report = context.getRoot().getStore(STORE).getOrComputeIfAbsent("report",
                    ignored -> new ReportOwner(), ReportOwner.class).report;
            var scenario = report.startScenario(context.getRequiredTestClass().getSimpleName(),
                    context.getRequiredTestClass().getPackageName(), "same story", List.of("component"));
            context.getStore(STORE).put("scenario", scenario);
            AutoExLaunchMetadata.capture(context).publish(scenario);
        }
        @Override public void afterEach(ExtensionContext context) {
            context.getStore(STORE).remove("scenario", UiExtentReportSession.UiScenario.class)
                    .complete(AtfReportStatus.PASS);
        }
    }

    private static final class ReportOwner implements ExtensionContext.Store.CloseableResource {
        private final UiExtentReportSession report = UiExtentReportSession.openConfigured(
                UiExtentReportOptions.builder(Path.of(System.getProperty(OUTPUT)))
                        .fileName("looks-like-another-suite.html").timestampEnabled(false)
                        .evidenceProfile(ReportEvidenceProfile
                                .forScreenshotPolicy(ReportScreenshotPolicy.NONE)).build());
        @Override public void close() { report.close(); }
    }

    @DisplayName("identical display title")
    @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    @ExtendWith(MetadataExtension.class)
    @Tag("launch-selected")
    public static class First {
        @Test void local() { }
        @Test void other() { }
    }

    @DisplayName("identical display title")
    @EnabledIfSystemProperty(named = ENABLED, matches = "true")
    @ExtendWith(MetadataExtension.class)
    public static class Second {
        @Test void local() { }
    }

    @Suite @SelectClasses({First.class, Second.class})
    public static class CanonicalSuite { }

    @Suite @SelectClasses({First.class, Second.class}) @IncludeTags("launch-selected")
    public static class FilteredSuite { }
}
