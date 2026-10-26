package atf.api.tests.governance;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;

/** Keeps reporting tests readable at the call site and imports semantic vocabulary normally. */
@AtfComponentTest
public class AtfSourceReadabilityGovernanceTest {
    private static final Pattern FULLY_QUALIFIED_ATF_INVOCATION =
            Pattern.compile("\\batf\\.api\\.[A-Za-z0-9_.]+\\(");
    private static final Set<String> REVIEWED_ADAPTERS = Set.of(
            "atf/reporting/support/CoreReportPortsAdapter.java",
            "atf/ui/tests/extent/support/audit/ReportAuditContext.java",
            "atf/ui/tests/extent/support/audit/ReportUiActionAdapters.java");

    @Test
    void reportingTestsDoNotUseFullyQualifiedAtfInvocationsInExpressions() throws Exception {
        Path sourceRoot = locateProjectRoot().resolve("atf-reporting-tests/src/test/java");
        List<String> violations = new ArrayList<>();
        if (Files.isDirectory(sourceRoot)) {
            try (var files = Files.walk(sourceRoot)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> inspect(path, sourceRoot, violations));
            }
        }

        assertTrue(violations.isEmpty(), () ->
                "Use normal or static imports for ATF invocations: " + violations);
    }

    @Test
    void projectJavaSourcesDoNotRepeatTheSameImport() throws Exception {
        Path projectRoot = locateProjectRoot();
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(projectRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> isJavaSource(path))
                    .forEach(path -> inspectDuplicateImports(path, projectRoot, violations));
        }

        assertTrue(violations.isEmpty(), () ->
                "Remove duplicate imports before promotion: " + violations);
    }

    private static void inspect(Path path, Path sourceRoot, List<String> violations) {
        try {
            if (REVIEWED_ADAPTERS.contains(sourceRoot.relativize(path).toString().replace('\\', '/'))) return;
            int lineNumber = 0;
            for (String line : Files.readAllLines(path)) {
                lineNumber++;
                if (!line.trim().startsWith("import ")
                        && FULLY_QUALIFIED_ATF_INVOCATION.matcher(line).find()) {
                    violations.add(sourceRoot.relativize(path) + ":" + lineNumber);
                }
            }
        } catch (Exception error) {
            violations.add("cannot read " + sourceRoot.relativize(path) + ": " + error.getMessage());
        }
    }

    private static void inspectDuplicateImports(Path path, Path projectRoot,
                                                List<String> violations) {
        try {
            Set<String> imports = new HashSet<>();
            Set<String> duplicates = new LinkedHashSet<>();
            for (String line : Files.readAllLines(path)) {
                String importLine = line.trim();
                if (importLine.startsWith("import ") && !imports.add(importLine)) {
                    duplicates.add(importLine);
                }
            }
            if (!duplicates.isEmpty()) {
                violations.add(projectRoot.relativize(path) + " -> " + duplicates);
            }
        } catch (Exception error) {
            violations.add("cannot read " + projectRoot.relativize(path) + ": " + error.getMessage());
        }
    }

    private static boolean isJavaSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/src/main/java/")
                || normalized.contains("/src/test/java/");
    }

}
