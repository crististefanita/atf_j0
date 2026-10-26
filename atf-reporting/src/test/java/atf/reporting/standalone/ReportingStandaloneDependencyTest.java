package atf.reporting.standalone;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fail-closed guard for the standalone reporting artifact boundary. */
class ReportingStandaloneDependencyTest {
    private static final Pattern EXTERNAL_ATF_IMPORT = Pattern.compile(
            "\\bimport\\s+atf\\.(core|mock|execution|validation|console|ui|debugui|testkit)(?:\\.|;)");

    @Test
    void productionSourceDoesNotImportAnotherAtfModule() throws Exception {
        Path root = projectRoot();
        List<String> violations;
        try (var files = Files.walk(root.resolve("atf-reporting/src/main/java"))) {
            violations = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> read(path).stream()
                            .filter(line -> EXTERNAL_ATF_IMPORT.matcher(line).find())
                            .map(line -> root.relativize(path) + ":" + line.trim()))
                    .toList();
        }
        assertTrue(violations.isEmpty(), () ->
                "Standalone reporting production imports external ATF modules: " + violations);
    }

    @Test
    void reportingPomDoesNotDeclareAtfModuleDependencies() throws Exception {
        String pom = Files.readString(projectRoot().resolve("atf-reporting/pom.xml"));
        assertTrue(!pom.matches("(?s).*<artifactId>atf-(core|mock|execution|validation|console|ui|debug-ui|test-kit)</artifactId>.*"),
                "atf-reporting production POM must not declare ATF module dependencies");
    }

    @Test
    void reportingPomDoesNotInheritTheWholeAtfReactor() throws Exception {
        String pom = Files.readString(projectRoot().resolve("atf-reporting/pom.xml"));
        assertTrue(!pom.contains("<parent>"),
                "atf-reporting must remain buildable without inheriting the whole ATF reactor");
    }

    private static List<String> read(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot inspect " + file, error);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("atf-reporting"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate ATF project root");
    }
}
