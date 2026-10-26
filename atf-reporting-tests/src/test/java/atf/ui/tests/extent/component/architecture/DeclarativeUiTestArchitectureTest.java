package atf.ui.tests.extent.component.architecture;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps annotation-driven test classes declarative. Browser mechanics belong
 * to production flows/modules and selector knowledge belongs to locators.
 */
@AtfComponentTest
class DeclarativeUiTestArchitectureTest {
    @Test
    void declarativeTestsDoNotOwnPlaywrightOrLocatorTypes() throws Exception {
        Path testRoot = Path.of(DeclarativeUiTestArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent").normalize();
        List<String> forbidden = List.of(
                "import com.microsoft.playwright.Page",
                "import com.microsoft.playwright.Locator",
                "com.microsoft.playwright.Page",
                "com.microsoft.playwright.Locator",
                "ReportUiLocators",
                ".locator(", "getByRole(", "getByText(", "getByLabel(");
        try (Stream<Path> files = Files.walk(testRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureTest.java"))
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureGuardTest.java"))
                    .filter(this::isDeclarativeSource)
                    .toList()) {
                String text = Files.readString(file);
                for (String token : forbidden) {
                    assertTrue(!text.contains(token),
                            () -> "Declarative UI test contains forbidden token '"
                                    + token + "': " + file);
                }
            }
        }
    }

    @Test
    void uiTestsDoNotDuplicateApiScenarioIdentityOrOwnerLiterals() throws Exception {
        Path testRoot = Path.of(DeclarativeUiTestArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent").normalize();
        List<String> forbidden = List.of(
                "\"GET /public/v2/", "\"POST /public/v2/", "\"PUT /public/v2/",
                "\"UsersApiTests\"", "\"PostsApiTests\"", "\"RequestVariantsApiTests\"",
                "\"ApiHeadersAndEdgeCasesTests\"", "\"ApiRequestFlowsTests\"",
                "\"ApiNegativeMatrixTests\"", "\"LifecycleApiScenarios\"");
        try (Stream<Path> files = Files.walk(testRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureTest.java"))
                    .toList()) {
                String text = Files.readString(file);
                for (String token : forbidden) {
                    assertTrue(!text.contains(token),
                            () -> "UI test duplicates API catalog literal '" + token + "': " + file);
                }
            }
        }
    }

    private boolean isDeclarativeSource(Path file) {
        try {
            String text = Files.readString(file);
            String normalized = file.toString().replace('\\', '/');
            boolean declarativePackage = normalized.contains("/e2e/");
            return (declarativePackage && (text.contains("@ApiScenarioSetup")
                    || text.contains("@UiExtentSectionAudit")))
                    || file.getFileName().toString().equals("AbstractApiReportUiContractTest.java");
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot inspect declarative source " + file, failure);
        }
    }

}
