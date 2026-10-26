package atf.api.tests.governance.console;

import atf.console.AtfConsoleConfigurationContext;
import atf.console.AtfConsoleConfigurationLoader;
import atf.console.ConsoleSettings;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Whole-ATF ratchet for defaults and the canonical UI/API composition roots. */
@AtfComponentTest
public class ConsoleDefaultsGovernanceTest {
    @Test
    void detailsAreEnabledWithoutYamlAndFromEachCanonicalLaunchDirectory(@TempDir Path isolated) {
        assertTrue(ConsoleSettings.defaults().enabled(), "CONSOLE-DEFAULT: console must be enabled");
        assertTrue(ConsoleSettings.defaults().detailsEnabled(), "CONSOLE-DEFAULT: details must be enabled");
        var fallback = AtfConsoleConfigurationLoader.load(new AtfConsoleConfigurationContext(isolated, null, null));
        assertTrue(fallback.settings().detailsEnabled());
        Path root = locateProjectRoot();
        for (Path start : new Path[]{root, root.resolve("atf-reporting-tests"), root.resolve("atf-test-practice-shop")}) {
            var actual = AtfConsoleConfigurationLoader.load(new AtfConsoleConfigurationContext(start, null, null));
            assertTrue(actual.settings().enabled() && actual.settings().detailsEnabled(),
                    () -> "CONSOLE-CONFIG " + start + ": enable reporting.console.enabled and detailsEnabled in the default config");
        }
    }

    @Test
    void explicitYamlOptOutRemainsEffective(@TempDir Path isolated) throws Exception {
        Path config = isolated.resolve("silent.yaml");
        Files.writeString(config, "reporting:\n  console:\n    enabled: false\n    detailsEnabled: false\n");
        var actual = AtfConsoleConfigurationLoader.load(config);
        assertFalse(actual.settings().enabled());
        assertFalse(actual.settings().detailsEnabled());
    }

    @Test
    void canonicalEntryPointsDoNotDiscardConfiguredConsoleTelemetry() throws Exception {
        Path root = locateProjectRoot();
        require(root, "atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/audit/ReportAuditContext.java",
                "ReportingTestConsole.configured()", "console.reporting()", "console.core()");
        require(root, "atf-reporting-tests/src/test/java/atf/reporting/api/tests/support/junit/ApiTestFixture.java",
                "AtfConsoleReporter.configuredStdout()", "lifecycle.console()");
        require(root, "atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/api/ApiReportAuditSetup.java",
                "AtfConsoleReporter.configuredStdout()");
        require(root, "atf-test-practice-shop/src/test/atf/shop/tests/support/lifecycle/ShopE2eContext.java",
                "AtfConsoleReporter.configuredStdout()");
        require(root, "atf-test-practice-shop/src/main/atf/shop/api/ShopApiClient.java",
                "AtfConsoleReporter.configuredStdout()", "settings.snapshot(), safeConsole(console)");
    }

    private static void require(Path root, String path, String... required) throws Exception {
        String source = Files.readString(root.resolve(path));
        for (String call : required) {
            assertTrue(source.contains(call), () -> "CONSOLE-WIRING " + path
                    + ": restore configured telemetry at " + call + "; silent fixtures must be explicit");
        }
    }
}
