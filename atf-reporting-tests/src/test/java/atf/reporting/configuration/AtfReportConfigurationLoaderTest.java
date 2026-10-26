package atf.reporting.configuration;
import atf.testkit.AtfUnitTest;
import atf.reporting.evidence.ReportScreenshotPolicy;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AtfReportConfigurationLoaderTest {
    @Test
    void resolvesRelativeOutputFromYamlLocationInsteadOfWorkingDirectory() throws Exception {
        Path project = Files.createTempDirectory("atf-config-");
        Path config = project.resolve("config").resolve("atf.yaml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, """
                reporting:
                  extent:
                    outputDirectory: reports/extent
                    fileName: intellij-run.html
                """);

        var settings = AtfReportConfigurationLoader.load(config);

        assertEquals(project.resolve("config/reports/extent").toAbsolutePath().normalize(), settings.outputDirectory());
        assertEquals("intellij-run.html", settings.fileName());
        assertEquals(settings.outputDirectory().resolve("intellij-run.html"), settings.reportPath());
        assertTrue(settings.timestampEnabled());
    }

    @Test
    void resolvesAConfigurableTimestampBeforeTheFileExtension() throws Exception {
        Path project = Files.createTempDirectory("atf-timestamp-config-");
        Path config = project.resolve("atf.yaml");
        Files.writeString(config, """
                reporting:
                  extent:
                    outputDirectory: reports
                    fileName: api-atf-extent-report.html
                    timestampEnabled: true
                    timestampPattern: yyyy-MM-dd_HH-mm-ss-SSS
                    timestampZone: UTC
                """);

        var settings = AtfReportConfigurationLoader.load(config);
        Path resolved = settings.resolveReportPath(Clock.fixed(
                Instant.parse("2026-10-14T12:34:56.789Z"), ZoneOffset.UTC));

        assertEquals(project.resolve("reports/api-atf-extent-report_2026-10-14_12-34-56-789.html")
                .toAbsolutePath().normalize(), resolved);
    }

    @Test
    void readsTheExtentSwitchUsedByDirectResourceTests() throws Exception {
        Path project = Files.createTempDirectory("atf-extent-switch-");
        Path config = project.resolve("atf.yaml");
        Files.writeString(config, """
                reporting:
                  extent:
                    enabled: false
                """);

        assertFalse(AtfReportConfigurationLoader.load(config).enabled());
    }

    @Test
    void readsTheTypedScreenshotPolicyFromExtentConfiguration() throws Exception {
        Path config = Files.createTempFile("atf-screenshot-policy-", ".yaml");
        try {
            Files.writeString(config, """
                    reporting:
                      extent:
                        screenshotPolicy: final-and-failure
                    """);

            assertEquals(ReportScreenshotPolicy.FINAL_AND_FAILURE,
                    AtfReportConfigurationLoader.load(config).screenshotPolicy());
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void rejectsAnUnknownScreenshotPolicyInsteadOfGuessing() throws Exception {
        Path config = Files.createTempFile("atf-invalid-screenshot-policy-", ".yaml");
        try {
            Files.writeString(config, """
                    reporting:
                      extent:
                        screenshotPolicy: final-or-failure
                    """);

            var error = assertThrows(IllegalStateException.class,
                    () -> AtfReportConfigurationLoader.load(config));
            assertTrue(error.getMessage().contains("screenshotPolicy"));
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void readsTheConfiguredJsonBodyDisplayLimitFromReportingYaml() throws Exception {
        Path config = Files.createTempFile("atf-body-limit-", ".yaml");
        try {
            Files.writeString(config, """
                    reporting:
                      assertionMaxDisplayCharacters: 240
                      bodyMaxDisplayCharacters: 321
                    """);

            var settings = AtfReportConfigurationLoader.load(config).reportingSettings();

            assertEquals(240, settings.assertionMaxDisplayCharacters());
            assertEquals(321, settings.bodyMaxDisplayCharacters());
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void usesACompactBodyPreviewDefault() {
        assertEquals(100, ReportingSettings.defaults().bodyMaxDisplayCharacters());
    }

    @Test
    void allowsZeroToDisableJsonBodyDisplayLimit() throws Exception {
        Path config = Files.createTempFile("atf-invalid-body-limit-", ".yaml");
        try {
            Files.writeString(config, "reporting:\n  bodyMaxDisplayCharacters: 0\n");
            assertEquals(0, AtfReportConfigurationLoader.load(config)
                    .reportingSettings().bodyMaxDisplayCharacters());
        } finally {
            Files.deleteIfExists(config);
        }
    }

    @Test
    void keepsStablePathWhenTimestampingIsDisabled() {
        var settings = new ExtentReportConfiguration(Path.of("target"), "report.html",
                false, "yyyy-MM-dd_HH-mm-ss-SSS", "UTC");

        assertEquals(settings.reportPath(), settings.resolveReportPath(Clock.fixed(
                Instant.parse("2026-10-14T12:34:56Z"), ZoneOffset.UTC)));
    }

    @Test
    void rejectsInvalidTimestampConfiguration() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ExtentReportConfiguration(Path.of("target"), "report.html", true, "", "UTC")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ExtentReportConfiguration(Path.of("target"), "report.html", true, "yyyy", "Not/AZone")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new ExtentReportConfiguration(Path.of("target"), "report.html", true, "yyyy/MM/dd", "UTC")),
                () -> assertThrows(IllegalStateException.class, () -> {
                    Path config = Files.createTempFile("atf-invalid-timestamp-", ".yaml");
                    try {
                        Files.writeString(config, "reporting:\n  extent:\n    timestampEnabled: maybe\n");
                        AtfReportConfigurationLoader.load(config);
                    } finally {
                        Files.deleteIfExists(config);
                    }
                }));
    }

    @Test
    void usesSafeDefaultsWhenNoYamlIsPresent() {
        var settings = ExtentReportConfiguration.defaults(Path.of("C:/atf"));

        assertEquals(Path.of("C:/atf/target").toAbsolutePath().normalize(), settings.outputDirectory());
        assertEquals("api-atf-extent-report.html", settings.fileName());
    }

    @Test
    void discoversConfigurationFromAParentDirectory() throws Exception {
        Path project = Files.createTempDirectory("atf-discovery-");
        Path nested = project.resolve("atf-tests/target");
        Files.createDirectories(nested);
        Path config = project.resolve("atf.yml");
        Files.writeString(config, "reporting:\n  extent:\n    fileName: discovered.html\n");

        assertEquals(config.toAbsolutePath().normalize(), AtfReportConfigurationLoader.discover(nested));
    }

    @Test
    void resolvesFromAnImmutableContextWithoutSamplingTheProcessWorkingDirectory() throws Exception {
        Path firstRoot = Files.createTempDirectory("atf-context-first-");
        Path secondRoot = Files.createTempDirectory("atf-context-second-");
        Files.writeString(firstRoot.resolve("atf.yaml"), """
                reporting:
                  extent:
                    outputDirectory: first-reports
                    fileName: first.html
                """);
        Files.writeString(secondRoot.resolve("atf.yaml"), """
                reporting:
                  extent:
                    outputDirectory: second-reports
                    fileName: second.html
                """);

        var settings = AtfReportConfigurationLoader.load(
                new AtfReportConfigurationContext(firstRoot, null, null));

        assertEquals(firstRoot.resolve("first-reports").toAbsolutePath().normalize(),
                settings.outputDirectory());
        assertEquals("first.html", settings.fileName());
        assertNotEquals(secondRoot.resolve("second-reports").toAbsolutePath().normalize(),
                settings.outputDirectory());
    }

    @Test
    void givesExplicitSystemPropertyContextPrecedenceOverEnvironmentContext() throws Exception {
        Path propertyRoot = Files.createTempDirectory("atf-context-property-");
        Path environmentRoot = Files.createTempDirectory("atf-context-environment-");
        Path propertyConfig = propertyRoot.resolve("property.yaml");
        Path environmentConfig = environmentRoot.resolve("environment.yaml");
        Files.writeString(propertyConfig, "reporting:\n  extent:\n    fileName: property.html\n");
        Files.writeString(environmentConfig, "reporting:\n  extent:\n    fileName: environment.html\n");

        var settings = AtfReportConfigurationLoader.load(new AtfReportConfigurationContext(
                propertyRoot, propertyConfig.toString(), environmentConfig.toString()));

        assertEquals("property.html", settings.fileName());
        assertEquals(propertyRoot.resolve("target").toAbsolutePath().normalize(),
                settings.outputDirectory());
    }

    @Test
    void resolvesRelativeExplicitConfigAgainstTheCapturedStartDirectory() throws Exception {
        Path root = Files.createTempDirectory("atf-relative-context-");
        Path config = root.resolve("config/atf.yaml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "reporting:\n  extent:\n    fileName: relative.html\n");

        var settings = AtfReportConfigurationLoader.load(new AtfReportConfigurationContext(
                root, "config/atf.yaml", null));

        assertEquals("relative.html", settings.fileName());
        assertEquals(root.resolve("config/target").toAbsolutePath().normalize(),
                settings.outputDirectory());
    }

    @Test
    void rejectsOutputDirectoryTraversalOutsideTheYamlDirectory() throws Exception {
        Path root = Files.createTempDirectory("atf-traversal-");
        Path config = root.resolve("config/atf.yaml");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "reporting:\n  extent:\n    outputDirectory: ../../outside\n");

        var error = assertThrows(IllegalStateException.class,
                () -> AtfReportConfigurationLoader.load(config));
        assertTrue(error.getMessage().contains("escapes"));
    }

    @Test
    void rejectsAPathAsTheConfiguredFileName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExtentReportConfiguration(Path.of("target"), "nested/report.html"));
    }
}
