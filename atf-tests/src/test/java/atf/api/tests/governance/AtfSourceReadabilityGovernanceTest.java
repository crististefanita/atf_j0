package atf.api.tests.governance;

import atf.api.tests.governance.layers.AtfJavaSourceInventory;
import atf.core.files.AtfJavaSourceReferences;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;

/** Keeps source imports and reporting call sites readable across declared Maven layouts. */
@AtfComponentTest
public class AtfSourceReadabilityGovernanceTest {
    private static final Pattern FULLY_QUALIFIED_ATF_INVOCATION = Pattern.compile(
            "(?<![\\p{javaJavaIdentifierPart}.])atf\\s*\\.\\s*[\\w$]+"
                    + "(?:\\s*\\.\\s*[\\w$]+)+\\s*\\(");
    private static final Pattern IMPORT = Pattern.compile(
            "\\bimport\\s+(static\\s+)?([\\w$]+(?:\\s*\\.\\s*(?:[\\w$]+|\\*))*)\\s*;");
    private static final Set<String> REVIEWED_ADAPTERS = Set.of(
            "atf/reporting/support/CoreReportPortsAdapter.java",
            "atf/ui/tests/extent/support/audit/ReportAuditContext.java",
            "atf/ui/tests/extent/support/audit/ReportUiActionAdapters.java");

    @Test
    void reportingTestsDoNotUseFullyQualifiedAtfInvocationsInExpressions() throws Exception {
        var violations = reportingViolations(locateProjectRoot());
        assertTrue(violations.isEmpty(), () -> "Use normal or static imports for ATF invocations: " + violations);
    }

    @Test
    void projectJavaSourcesDoNotRepeatTheSameImport() throws Exception {
        var violations = duplicateImports(locateProjectRoot());
        assertTrue(violations.isEmpty(), () -> "Remove duplicate imports before promotion: " + violations);
    }

    @Test
    void sourceInventoryIncludesBothShopLayoutsDefaultsAndOverlappingRoots(@TempDir Path root) throws Exception {
        source(root, "shop/pom.xml", """
                <project><build><sourceDirectory>src/main</sourceDirectory>
                <testSourceDirectory>src/test</testSourceDirectory></build></project>
                """);
        source(root, "standard/pom.xml", "<project><!-- <sourceDirectory>ignored</sourceDirectory> --></project>");
        source(root, "overlap/pom.xml", """
                <project><build><sourceDirectory>src</sourceDirectory>
                <testSourceDirectory>src/test</testSourceDirectory></build></project>
                """);
        var expected = new HashSet<Path>();
        for (String relative : List.of("shop/src/main/A.java", "shop/src/test/B.java",
                "standard/src/main/java/C.java", "standard/src/test/java/D.java", "overlap/src/test/E.java")) {
            expected.add(source(root, relative, "import java.util.List;\nimport java . util . List;\nclass Example {}"));
        }
        Path valid = source(root, "shop/src/main/Valid.java", """
                // import java.util.List;
                class Valid { String example = "import java.util.List; import java.util.List;"; }
                """);
        source(root, "shop/target/src/main/java/Generated.java", "import a.B;\nimport a.B;");
        source(root, "shop/src/main/target/Generated.java", "import a.B;\nimport a.B;");
        source(root, "shop/unrelated/Outside.java", "import a.B;\nimport a.B;");
        assertEquals(expected.size(), duplicateImports(root).size());
        expected.add(valid);
        assertEquals(expected, Set.copyOf(AtfJavaSourceInventory.allSources(root)));
    }

    @Test
    void sourceInventoryRejectsEscapingAndEmptyConfiguredRoots(@TempDir Path root) throws Exception {
        Path pom = source(root, "module/pom.xml", """
                <project><build><sourceDirectory>../outside</sourceDirectory></build></project>
                """);
        assertTrue(assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root))
                .getMessage().contains("sourceDirectory"));
        Files.writeString(pom, "<project><build><sourceDirectory> </sourceDirectory></build></project>");
        assertTrue(assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root))
                .getMessage().contains("Empty sourceDirectory"));
    }

    @Test
    void qualifiedCallsIncludeCurrentLegacyConstructorsAndMultilineButExcludeExamples() {
        String sample = """
                atf.api.Legacy.run();
                new atf.reporting.contract.Sample();
                atf /* split */ .
                    reporting.contract.Sample.run
                    ();
                // atf.ui.Example.run();
                /* new atf.core.Example(); */
                String example = "atf.validation.Example.run()";
                import atf.reporting.contract.Imported;
                import static atf.reporting.contract.Imported.run;
                Imported.run();
                run();
                """;
        assertEquals(3, FULLY_QUALIFIED_ATF_INVOCATION.matcher(
                AtfJavaSourceReferences.codeOnly(sample)).results().count());
    }

    @Test
    void reportingBoundaryPreservesReviewedAdaptersAndIgnoresGeneratedOutput(@TempDir Path root) throws Exception {
        source(root, "atf-reporting-tests/pom.xml", "<project/>");
        for (String adapter : REVIEWED_ADAPTERS) {
            source(root, "atf-reporting-tests/src/test/java/" + adapter, "atf.reporting.Sample.run();");
        }
        source(root, "atf-reporting-tests/src/test/java/example/Bad.java", "new atf.reporting.Sample();");
        source(root, "atf-reporting-tests/src/test/java/target/Generated.java", "atf.reporting.Sample.run();");
        source(root, "other/pom.xml", "<project/>");
        source(root, "other/src/test/java/Business.java", "atf.reporting.Sample.run();");
        var violations = reportingViolations(root);
        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().replace('\\', '/').equals("example/Bad.java:1"));
    }

    private static List<String> reportingViolations(Path project) throws Exception {
        Path module = project.resolve("atf-reporting-tests");
        Path sourceRoot = module.resolve("src/test/java");
        var violations = new ArrayList<String>();
        if (!Files.isDirectory(module)) return violations;
        for (Path file : AtfJavaSourceInventory.testSources(module)) inspect(file, sourceRoot, violations);
        return violations;
    }

    private static List<String> duplicateImports(Path project) throws Exception {
        var violations = new ArrayList<String>();
        for (Path file : AtfJavaSourceInventory.allSources(project)) inspectDuplicateImports(file, project, violations);
        return violations;
    }

    private static void inspect(Path path, Path sourceRoot, List<String> violations) {
        try {
            if (REVIEWED_ADAPTERS.contains(sourceRoot.relativize(path).toString().replace('\\', '/'))) return;
            String code = AtfJavaSourceReferences.codeOnly(Files.readString(path));
            var calls = FULLY_QUALIFIED_ATF_INVOCATION.matcher(code);
            while (calls.find()) {
                long line = 1 + code.substring(0, calls.start()).chars().filter(value -> value == '\n').count();
                violations.add(sourceRoot.relativize(path) + ":" + line);
            }
        } catch (Exception error) {
            violations.add("cannot read " + sourceRoot.relativize(path) + ": " + error.getMessage());
        }
    }

    private static void inspectDuplicateImports(Path path, Path projectRoot, List<String> violations) {
        try {
            Set<String> imports = new HashSet<>();
            Set<String> duplicates = new LinkedHashSet<>();
            var declarations = IMPORT.matcher(AtfJavaSourceReferences.codeOnly(Files.readString(path)));
            while (declarations.find()) {
                String canonical = "import " + (declarations.group(1) == null ? "" : "static ")
                        + declarations.group(2).replaceAll("\\s+", "") + ";";
                if (!imports.add(canonical)) duplicates.add(canonical);
            }
            if (!duplicates.isEmpty()) violations.add(projectRoot.relativize(path) + " -> " + duplicates);
        } catch (Exception error) {
            violations.add("cannot read " + projectRoot.relativize(path) + ": " + error.getMessage());
        }
    }

    private static Path source(Path root, String relative, String contents) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
        return file;
    }
}

