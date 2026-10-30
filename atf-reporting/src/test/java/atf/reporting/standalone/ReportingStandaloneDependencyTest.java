package atf.reporting.standalone;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fail-closed guard for the standalone reporting artifact boundary. */
class ReportingStandaloneDependencyTest {
    @Test
    void productionSourceDoesNotImportAnotherAtfModule() throws Exception {
        Path root = moduleRoot();
        List<String> violations;
        try (var files = Files.walk(root.resolve("src/main/java"))) {
            violations = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> read(path).stream()
                            .filter(StandaloneBoundaryRules::externalAtfImport)
                            .map(line -> root.relativize(path) + ":" + line.trim()))
                    .toList();
        }
        assertTrue(violations.isEmpty(), () ->
                "Standalone reporting production imports external ATF modules: " + violations);
    }

    @Test
    void reportingPomDoesNotDeclareAtfModuleDependencies() throws Exception {
        String pom = Files.readString(moduleRoot().resolve("pom.xml"));
        assertTrue(!StandaloneBoundaryRules.hasAtfDependency(pom),
                "atf-reporting production POM must not declare ATF module dependencies");
    }

    @Test
    void reportingPomDoesNotInheritTheWholeAtfReactor() throws Exception {
        String pom = Files.readString(moduleRoot().resolve("pom.xml"));
        assertTrue(!StandaloneBoundaryRules.hasParent(pom),
                "atf-reporting must remain buildable without inheriting the whole ATF reactor");
    }

    private static List<String> read(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot inspect " + file, error);
        }
    }

    private static Path moduleRoot() throws Exception {
        Path current = Path.of(ReportingStandaloneDependencyTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java/atf/reporting"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate reporting module from its test CodeSource");
    }
}
