package atf.ui.tests.extent.support.config;

import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockConfigurationLoader;
import atf.mock.AtfMockTransport;
import atf.testkit.output.ReportOutputDirectory;
import atf.testkit.channel.AtfTestChannelResolver;
import atf.testkit.channel.TestChannel;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Test-only ownership boundary for reporting fixtures.
 *
 * <p>The reporting module is independently testable: API fixtures use the
 * module-owned mock catalog and UI fixtures use the module-owned UI policy.
 * Explicit process overrides remain supported for IntelliJ and CI runs.</p>
 */
public final class ReportingTestResources {
    public static final String MOCK_RESOURCE = "atf-mock.yaml";
    public static final String UI_RESOURCE = "atf-reporting-ui.yaml";

    private ReportingTestResources() { }

    public static Path resourcePath(String resourceName) {
        URL resource = ReportingTestResources.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalStateException("Reporting test resource is missing: " + resourceName);
        }
        try {
            Path path = Path.of(resource.toURI()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Reporting test resource is not a file: " + path);
            }
            return path;
        } catch (URISyntaxException | RuntimeException error) {
            throw new IllegalStateException("Cannot resolve reporting test resource: " + resourceName, error);
        }
    }

    public static AtfMockConfiguration mockConfiguration() {
        return AtfMockConfigurationLoader.load(resourcePath(MOCK_RESOURCE));
    }

    public static AtfMockTransport mockTransport() {
        return new AtfMockTransport(mockConfiguration());
    }

    public static UiConfiguration uiConfiguration() {
        return uiConfiguration(null);
    }

    public static UiConfiguration uiConfiguration(Class<?> testClass) {
        if (testClass != null) {
            AtfTestChannelResolver.resolve(testClass)
                    .ifPresent(plan -> plan.requireSupported(Set.of(TestChannel.UI)));
        }
        AtfUiConfigurationContext process = AtfUiConfigurationContext.fromProcess();
        UiConfiguration loaded = process.explicitConfigPath() == null
                ? AtfUiConfigurationLoader.load(resourcePath(UI_RESOURCE))
                : AtfUiConfigurationLoader.load(process);
        Path reportOverride = process.reportOverridePath();
        if (reportOverride != null) {
            loaded = loaded.withAuditInputReport(reportOverride);
        }
        Path projectRoot = projectRoot(process);
        Path uiBase = projectRoot.resolve("artifacts/reports/ui");
        Path directory = ReportOutputDirectory.resolve(projectRoot, uiBase, testClass);
        return loaded.withReportDirectory(directory);
    }

    static Path projectRoot(AtfUiConfigurationContext process) {
        Path fromResource = locateProjectRoot(resourcePath(UI_RESOURCE).getParent());
        if (fromResource != null) return fromResource;
        Path fromProcess = locateProjectRoot(process.startDirectory());
        if (fromProcess != null) return fromProcess;
        throw new IllegalStateException("Cannot locate the ATF reactor root from the Reporting "
                + "test resource or working directory: " + process.startDirectory());
    }

    private static Path locateProjectRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isRegularFile(current.resolve("atf-reporting-tests/pom.xml"))) {
                return current.toAbsolutePath().normalize();
            }
            current = current.getParent();
        }
        return null;
    }
}
