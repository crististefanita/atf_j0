package atf.reporting.api.tests.architecture;

import atf.core.config.module.AtfModuleIds;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps executable API tests independent from the browser/report-audit layer. */
@AtfComponentTest
class ApiTestBoundaryArchitectureTest {
    private static final List<String> FORBIDDEN_API_IMPORTS = List.of(
            "atf.ui.tests",
            "atf.ui",
            "atf.debugui",
            "com.microsoft.playwright",
            "org.junit.jupiter.api.extension.ExtensionContext");

    @Test
    void apiTestNamespaceDoesNotImportUiOrBrowserConcerns() throws Exception {
        Path root = ArchitectureTestSupport.locateProjectRoot();
        Path apiTests = root.resolve(AtfModuleIds.REPORTING_TESTS)
                .resolve("src/test/java/atf/api/tests/api");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(apiTests)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    for (String forbidden : FORBIDDEN_API_IMPORTS) {
                        if (source.lines()
                                .map(String::strip)
                                .filter(line -> line.startsWith("import "))
                                .anyMatch(line -> line.contains(forbidden))) {
                            violations.add(root.relativize(path) + " contains API/UI boundary token: " + forbidden);
                        }
                    }
                } catch (Exception error) {
                    violations.add("cannot read " + root.relativize(path) + ": " + error.getMessage());
                }
            });
        }
        assertTrue(violations.isEmpty(), () -> "API test boundary violations: " + violations);
    }
}
