package atf.ui.tests.extent.support.config;
import atf.testkit.AtfUnitTest;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiScreenValidationConfiguration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfUiConfigurationLoaderTest {
    @Test
    void reportingOutputRootComesFromItsResourceWhenTheWorkingDirectoryIsUnrelated() throws Exception {
        Path unrelated = Files.createTempDirectory("foreign-ide-working-directory-");
        var process = new AtfUiConfigurationContext(unrelated, null, null);

        Path root = ReportingTestResources.projectRoot(process);
        Path resource = ReportingTestResources.resourcePath(ReportingTestResources.UI_RESOURCE);

        assertNotEquals(unrelated, root);
        assertTrue(Files.isRegularFile(root.resolve("atf-reporting/pom.xml")));
        assertTrue(resource.startsWith(root));
        assertEquals(root.resolve("artifacts/reports/ui"),
                ReportingTestResources.uiConfiguration().reportDirectory());
    }

    @Test
    void reportingOutputRootIsStableWhenLaunchedFromTheReportingModuleDirectory() throws Exception {
        Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
        var process = new AtfUiConfigurationContext(moduleDirectory, null, null);

        Path root = ReportingTestResources.projectRoot(process);

        assertNotEquals(moduleDirectory, root,
                "the classpath resource must locate the reactor root, not adopt a module cwd");
        assertEquals(moduleDirectory.getParent(), root);
        assertEquals(root.resolve("artifacts/reports/ui"),
                ReportingTestResources.uiConfiguration().reportDirectory());
    }

    @Test
    void loadsTheUiSectionAndResolvesReportDirectoryRelativeToYaml() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-config-");
        Path yaml = root.resolve("atf.yaml");
        Files.writeString(yaml, """
                ui:
                  browser: firefox
                  baseUrl: https://atf.local
                  timeout: PT4S
                  navigationTimeout: PT7S
                  viewport:
                    width: 1280
                    height: 720
                  zoomPercent: 70
                  retry:
                    maxAttempts: 3
                    backoff: PT0S
                    eligibleOperations: [wait-for-visible, wait-for-text]
                    reason: report stabilization
                  report:
                    directory: artifacts/reports
                    filePrefix: report_
                  audit:
                    inputReport: artifacts/reports/input.html
                  apiSetup:
                    scenarioFamilies: [posts, requests]
                  screenValidation:
                    enabled: false
                    fullPage: true
                    screenshotTimeout: PT45S
                """);

        UiConfiguration configuration = AtfUiConfigurationLoader.load(yaml);

        assertEquals("firefox", configuration.browser().browser());
        assertEquals(1280, configuration.browser().viewportWidth());
        assertEquals(70, configuration.browser().zoomPercent());
        assertEquals(3, configuration.browser().retryPolicy().maxAttempts());
        assertTrue(configuration.browser().retryPolicy()
                .allows("Wait for visible", 1));
        assertFalse(configuration.browser().retryPolicy().allows("Click", 1));
        assertEquals(root.resolve("artifacts/reports").toAbsolutePath().normalize(),
                configuration.reportDirectory());
        assertEquals("report_", configuration.reportFilePrefix());
        assertEquals(root.resolve("artifacts/reports/input.html").toAbsolutePath().normalize(),
                configuration.auditInputReport().orElseThrow());
        assertEquals(java.util.List.of("posts", "requests"), configuration.apiScenarioFamilies());
        assertFalse(configuration.screenValidation().enabled());
        assertTrue(configuration.screenValidation().fullPage());
        assertEquals(java.time.Duration.ofSeconds(45),
                configuration.screenValidation().screenshotTimeout());
    }

    @Test
    void explicitConfigPathLoadsWithoutAnEnableSwitch() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-config-override-");
        Path yaml = root.resolve("atf.yaml");
        Files.writeString(yaml, "ui:\n  browser: chromium\n");

        UiConfiguration configuration = AtfUiConfigurationLoader.load(
                new AtfUiConfigurationContext(root, yaml.toString(), null));

        assertEquals("chromium", configuration.browser().browser());
        assertEquals("chromium", AtfUiConfigurationLoader.load(yaml).browser().browser());
    }

    @Test
    void systemPropertyConfigurationWinsOverEnvironmentConfigurationAndReportOverrideWinsOverYaml(
            ) throws Exception {
        Path root = Files.createTempDirectory("atf-ui-config-precedence-");
        Path propertyYaml = root.resolve("property.yaml");
        Path environmentYaml = root.resolve("environment.yaml");
        Files.writeString(propertyYaml, """
                ui:
                  browser: firefox
                  audit:
                    inputReport: from-property-yaml.html
                """);
        Files.writeString(environmentYaml, """
                ui:
                  browser: webkit
                """);

        UiConfiguration selected = AtfUiConfigurationLoader.load(new AtfUiConfigurationContext(
                root, propertyYaml.toString(), environmentYaml.toString(), "manual-override.html"));

        assertEquals("firefox", selected.browser().browser(),
                "an explicit system property must outrank the environment configuration");
        assertEquals(root.resolve("manual-override.html").toAbsolutePath().normalize(),
                selected.auditInputReport().orElseThrow(),
                "the explicit report override must outrank YAML while resolving beside that YAML");
    }

    @Test
    void explicitReportOverrideWinsOverYamlInputReport() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-report-override-");
        Path yaml = root.resolve("atf.yaml");
        Files.writeString(yaml, "ui:\n  audit:\n    inputReport: yaml.html\n");

        Path override = root.resolve("manual.html");
        UiConfiguration configuration = AtfUiConfigurationLoader.load(
                new AtfUiConfigurationContext(root, yaml.toString(), null, override.toString()));

        assertEquals(override.toAbsolutePath().normalize(), configuration.auditInputReport().orElseThrow());
    }

    @Test
    void relativeReportOverrideUsesYamlDirectoryNotSurefireModuleDirectory() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-report-base-");
        Path yaml = root.resolve("atf.yaml");
        Files.writeString(yaml, "ui:\n  browser: chromium\n");
        Path moduleWorkingDirectory = root.resolve("atf-tests");
        Files.createDirectories(moduleWorkingDirectory);

        UiConfiguration configuration = AtfUiConfigurationLoader.load(
                new AtfUiConfigurationContext(moduleWorkingDirectory, yaml.toString(), null,
                        "artifacts/reports/input.html"));

        assertEquals(root.resolve("artifacts/reports/input.html").toAbsolutePath().normalize(),
                configuration.auditInputReport().orElseThrow());
    }

    @Test
    void defaultsScenarioFamiliesWhenTheUiSetupIsOmitted() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-config-defaults-");
        Path yaml = root.resolve("atf.yaml");
        Files.writeString(yaml, "ui:\n  browser: chromium\n");

        assertEquals(java.util.List.of("posts", "users", "requests", "headers", "lifecycle"),
                AtfUiConfigurationLoader.load(yaml).apiScenarioFamilies());
        assertEquals(root.resolve("artifacts/reports/ui").toAbsolutePath().normalize(),
                AtfUiConfigurationLoader.load(yaml).reportDirectory());
        assertTrue(AtfUiConfigurationLoader.load(yaml).screenValidation().enabled());
        assertFalse(AtfUiConfigurationLoader.load(yaml).screenValidation().fullPage());
        assertEquals(UiScreenValidationConfiguration.DEFAULT_SCREENSHOT_TIMEOUT,
                AtfUiConfigurationLoader.load(yaml).screenValidation().screenshotTimeout());
    }

    @Test
    void configuredReportPrefixProducesTimestampedNamesForParallelAudits() {
        Path root = Path.of("target/ui-config-prefix").toAbsolutePath().normalize();
        UiConfiguration configuration = new UiConfiguration(
                UiBrowserConfiguration.defaults(), root, "ui-audit_",
                List.of("posts"), UiScreenValidationConfiguration.defaults(),
                java.util.Optional.empty());

        assertEquals("ui-audit_existing_2026-10-16_10-00-00-123.html",
                configuration.timestampedReportFileName("existing", Clock.fixed(
                        Instant.parse("2026-10-16T10:00:00.123Z"), ZoneOffset.UTC)));
    }

    @Test
    void reservesCollisionFreeReportPathsUnderParallelWritersAndIndependentRoots() throws Exception {
        Path firstRoot = Files.createTempDirectory("atf-reserved-report-first-");
        Path secondRoot = Files.createTempDirectory("atf-reserved-report-second-");
        var first = UiConfiguration.defaults(firstRoot).withReportDirectory(firstRoot.resolve("reports"));
        var second = UiConfiguration.defaults(secondRoot).withReportDirectory(secondRoot.resolve("reports"));
        Clock sameTick = Clock.fixed(Instant.parse("2026-10-23T12:00:00.123Z"), ZoneOffset.UTC);

        Set<Path> parallelPaths;
        try (var executor = Executors.newFixedThreadPool(8)) {
            parallelPaths = IntStream.range(0, 32)
                    .mapToObj(index -> executor.submit(() -> first.reserveTimestampedReportPath(
                            "same-owner", sameTick)))
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception failure) {
                            throw new IllegalStateException("Parallel report reservation failed", failure);
                        }
                    })
                    .collect(java.util.stream.Collectors.toSet());
        }

        assertEquals(32, parallelPaths.size(), "same owner and clock tick must never collide");
        assertTrue(parallelPaths.stream().allMatch(path -> path.getParent().equals(first.reportDirectory())));
        assertTrue(parallelPaths.stream().allMatch(Files::isRegularFile),
                "each reservation must be an atomic filesystem claim, not just a distinct string");
        var expectedContents = new java.util.concurrent.ConcurrentHashMap<Path, String>();
        parallelPaths.forEach(path -> {
            String marker = "report-owner-" + path.getFileName();
            try {
                Files.writeString(path, marker);
            } catch (Exception failure) {
                throw new IllegalStateException("Could not write reserved report " + path, failure);
            }
            expectedContents.put(path, marker);
        });
        expectedContents.forEach((path, marker) -> {
            try {
                assertEquals(marker, Files.readString(path), "a later reservation must not overwrite output");
            } catch (Exception failure) {
                throw new IllegalStateException("Could not verify reserved report " + path, failure);
            }
        });
        var otherRootPath = second.reserveTimestampedReportPath("same-owner", sameTick);
        assertEquals(second.reportDirectory(), otherRootPath.getParent());
        assertFalse(parallelPaths.contains(otherRootPath));
    }

    @Test
    void loadsDeclarativeAuditSuitesWithFamiliesTabsValidatorsDepthAndSource() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-suite-config-");
        Path yaml = root.resolve("atf.yaml");
        Files.writeString(yaml, """
                ui:
                  audit:
                    suites:
                      users-posts:
                        scenarioFamilies: [users, posts]
                        tabs: [tests, overview, categories]
                        validators: [setup, request-contract, cross-tab]
                        depth: deep
                        source: yaml
                """);

        UiConfiguration configuration = AtfUiConfigurationLoader.load(yaml);
        UiAuditSuiteConfiguration suite = configuration.auditSuite("USERS_POSTS").orElseThrow();

        assertEquals(List.of("users", "posts"), suite.scenarioFamilies());
        assertEquals(List.of("tests", "overview", "categories"), suite.tabs());
        assertEquals(List.of("setup", "request-contract", "cross-tab"), suite.validators());
        assertEquals("deep", suite.depth());
        assertEquals("yaml", suite.source());
    }
}
