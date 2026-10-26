package atf.ui.tests.extent.component.architecture;
import atf.core.files.AtfPathLocator;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardrail for the UI test layer: selector knowledge belongs to locators and
 * semantic flows, never to a JUnit intent class. Browser calls are expected in
 * the test-kit's flow/audit/extraction seams; this guard therefore scans only
 * executable intent and fixture classes.
 */
@AtfComponentTest
class UiTestLayerArchitectureTest {
    private static final Path TEST_SOURCE_ROOT = AtfPathLocator.locateAtfTestSourceRoot(
            Path.of(System.getProperty("user.dir", ".")), Path.of("atf", "ui", "tests"));

    @Test
    void uiTestsDoNotContainDirectPlaywrightLocators() throws Exception {
        Path root = TEST_SOURCE_ROOT.resolve(Path.of("atf", "ui", "tests"));
        List<String> forbidden = List.of(
                "import com.microsoft.playwright.Locator",
                "com.microsoft.playwright.Locator",
                ".locator(", "getByRole(", "getByText(", "page.locator(");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("\\support\\"))
                    .filter(path -> !path.toString().contains("\\e2e\\"))
                    // Selector knowledge is intentionally centralized in the dedicated
                    // locators package; this guard protects all other UI layers from
                    // bypassing that seam.
                    .filter(path -> !path.toString().contains("\\locators\\"))
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureTest.java"))
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureGuardTest.java"))
                    .filter(path -> !path.getFileName().toString().equals("ReportUiLocatorsTest.java"))
                    .toList()) {
                String text = Files.readString(file);
                for (String token : forbidden) {
                    assertTrue(!text.contains(token),
                            () -> "UI test layer contains direct locator token '" + token + "': " + file);
                }
            }
        }
    }

}
