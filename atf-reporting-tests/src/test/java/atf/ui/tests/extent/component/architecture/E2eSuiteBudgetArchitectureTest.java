package atf.ui.tests.extent.component.architecture;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Freezes browser proof at the deliberate reporting-suite budget. */
@AtfComponentTest
class E2eSuiteBudgetArchitectureTest {
    private static final int E2E_BUDGET = 8;

    @Test
    void reportingBrowserProofStaysAtTheApprovedBudget() throws Exception {
        Path sourceRoot = sourceRoot();
        List<Path> e2eTests;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            e2eTests = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(E2eSuiteBudgetArchitectureTest::containsE2eAnnotation)
                    .toList();
        }

        assertEquals(E2E_BUDGET, e2eTests.size(),
                "a new reporting E2E requires an explicit feedback entry and budget review");
        assertTrue(e2eTests.stream().allMatch(path -> normalized(path).contains("/e2e/")),
                "browser proof must be owned by an e2e package");
        assertTrue(e2eTests.stream().noneMatch(path -> normalized(path).contains("/support/")),
                "support and fixture classes must remain non-executable");
    }

    private static Path sourceRoot() throws URISyntaxException {
        return Path.of(E2eSuiteBudgetArchitectureTest.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent")
                .normalize();
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static boolean containsE2eAnnotation(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .anyMatch(line -> line.equals("@AtfE2eTest"));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot inspect E2E source: " + path, error);
        }
    }
}
