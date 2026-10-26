package atf.reporting.api.tests.architecture;

import atf.core.config.module.AtfModuleIds;
import atf.core.files.AtfJavaSourceReferences;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Keeps executable API tests independent from the browser/report-audit layer. */
@AtfComponentTest
class ApiTestBoundaryArchitectureTest {
    @TempDir Path fixtureRoot;
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
                .resolve("src/test/java/atf/reporting/api/tests/api");
        List<String> violations = inspect(root, apiTests);
        assertTrue(violations.isEmpty(), () -> "API test boundary violations: " + violations);
    }

    @Test
    void rejectsJavaDependenciesAtBothSidesOfANamespaceMove() throws Exception {
        List<String> references = List.of(
                "import atf.ui.Widget;", "import static atf.ui.Widget.VALUE;",
                "import atf.ui.*;", "package atf.ui.misplaced;",
                "class Bad { Object value = new atf.ui.Widget(); }",
                "import com.microsoft.playwright.Page;",
                "class Bad { com.microsoft.playwright.Page page; }",
                "class Bad { org.junit.jupiter.api.extension.ExtensionContext context; }",
                "import " + "\\" + "u0061" + "tf.ui.Widget;",
                "// example " + "\\" + "u000a" + " import atf.ui.Widget;");
        for (String location : List.of("legacy/Example.java", "canonical/Example.java")) {
            Path file = fixtureRoot.resolve(location);
            Files.createDirectories(file.getParent());
            for (String source : references) {
                Files.writeString(file, source);
                List<String> violations = inspect(fixtureRoot, file.getParent());
                assertTrue(violations.stream().anyMatch(message -> message.contains(location.replace('/',
                                        java.io.File.separatorChar)) && message.contains("API/UI boundary token:")),
                        () -> "Dependency escaped after move: " + location + ": " + source);
            }
        }
    }

    @Test
    void ignoresExamplesAndSimilarPrefixesWithoutHidingActualCode() throws Exception {
        Path file = fixtureRoot.resolve("Example.java");
        String examples = "// import atf.ui.Widget;\n/* atf.ui.Widget example */\n"
                + "class Good { String key = \"atf.ui.timeout\";"
                + " String escaped = \"" + "\\" + "u0061" + "tf.ui.timeout\";"
                + " atf.uikit.Widget permitted; }";
        Files.writeString(file, examples);
        assertEquals(List.of(), inspect(fixtureRoot, fixtureRoot));
        Files.writeString(file, examples + "\nclass Bad { atf.ui.Widget real; }");
        assertTrue(inspect(fixtureRoot, fixtureRoot).stream()
                .anyMatch(message -> message.contains("Example.java") && message.endsWith("atf.ui")));
    }

    private static List<String> inspect(Path root, Path apiTests) throws Exception {
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(apiTests)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    for (String forbidden : FORBIDDEN_API_IMPORTS) {
                        if (AtfJavaSourceReferences.containsNamespace(source, forbidden)) {
                            violations.add(root.relativize(path) + " contains API/UI boundary token: " + forbidden);
                        }
                    }
                } catch (Exception error) {
                    violations.add("cannot read " + root.relativize(path) + ": " + error.getMessage());
                }
            });
        }
        return List.copyOf(violations);
    }
}
