package atf.ui.tests.extent.component.architecture;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures flow/auditor classes consume the locator facade instead of composing selectors. */
@AtfComponentTest
class UiSelectorBoundaryArchitectureTest {
    @Test
    void flowAndAuditorLayersDoNotComposePlaywrightSelectors() throws Exception {
        Path root = Path.of(UiSelectorBoundaryArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests").normalize();
        List<String> forbidden = List.of(
                "page.locator(", "categoryPage.locator(", "failsPage.locator(",
                "locator(\"", "getByRole(", "getByText(", "getByLabel(");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        String relative = root.relativize(path).toString().replace('\\', '/');
                        return relative.contains("/support/")
                                && !relative.contains("/support/locators/")
                                && !relative.contains("/support/section/extract/");
                    })
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureTest.java"))
                    .filter(path -> !path.getFileName().toString().endsWith("ArchitectureGuardTest.java"))
                    .toList()) {
                String text = Files.readString(file);
                for (String token : forbidden) {
                    assertTrue(!text.contains(token),
                            () -> "Selector token '" + token + "' escaped the locator boundary: " + file);
                }
            }
        }
    }

    @Test
    void locatorBoundaryPrefersAttributeFormForClassSelectors() throws Exception {
        Path root = Path.of(UiSelectorBoundaryArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent/support/locators")
                .normalize();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertTrue(!source.matches("(?s).*locator\\(\\\"[^\\\"]*\\.[A-Za-z][A-Za-z0-9_-]*.*"),
                        () -> "Locator must use element[class~=...] or data-atf-* attributes: " + file);
            }
        }
    }
}
