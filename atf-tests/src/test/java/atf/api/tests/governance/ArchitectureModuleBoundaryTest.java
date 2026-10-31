package atf.api.tests.governance;

import atf.api.tests.governance.distribution.ReportingHarnessOwnership;

import atf.core.config.module.AtfModuleIds;
import atf.api.tests.governance.distribution.GovernanceDeployment;
import atf.api.tests.governance.distribution.ReportingGovernanceSurface;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static atf.api.tests.governance.AtfGovernanceTestSupport.*;

/** Whole-ATF guard for source imports, Maven direction and process-state ownership. */
@AtfComponentTest
public class ArchitectureModuleBoundaryTest {
    @TempDir Path fixtureRoot;

    @Test
    void rejectsForbiddenNamespacesBeforeAndAfterSourceMoves() throws Exception {
        List<String> references = List.of(
                "import atf.reporting.Widget;", "import static atf.reporting.Widget.VALUE;",
                "import atf.reporting.*;", "package atf.reporting.misplaced;",
                "class Bad { atf.reporting.Widget value; }",
                "class Bad { Object value = atf . reporting . Widget.create(); }",
                "/* example only */ class Bad { atf.reporting.Widget real; }",
                "import " + "\\" + "u0061" + "tf.reporting.Widget;",
                "// example " + "\\" + "u000a" + " import atf.reporting.Widget;");
        for (String location : List.of("src/test/java/legacy/Example.java",
                "src/main/java/canonical/Example.java")) {
            Path file = fixtureRoot.resolve(location);
            Files.createDirectories(file.getParent());
            for (String source : references) {
                Files.writeString(file, source);
                var violations = new ArrayList<String>();
                assertNoForbidden(file.getParent(), List.of("atf.reporting"), violations);
                assertEquals(List.of(file + " contains atf.reporting"), violations, source);
            }
        }
    }

    @Test
    void permitsConfigurationAndDocumentationButNotAdjacentJavaReferences() throws Exception {
        Path file = fixtureRoot.resolve("Example.java");
        String source = "// import atf.reporting.Widget;\n/* atf.reporting.Widget */\n"
                + "class Good { String key = \"atf.reporting.extent.enabled\";"
                + " String escaped = \"" + "\\" + "u0061" + "tf.reporting.enabled\";"
                + " atf.reportingish.Widget allowed; }";
        Files.writeString(file, source);
        var violations = new ArrayList<String>();
        assertNoForbidden(fixtureRoot, List.of("atf.reporting"), violations);
        assertTrue(violations.isEmpty(), () -> violations.toString());
        Files.writeString(file, source + "\nclass Bad { atf.reporting.Widget actual; }");
        assertNoForbiddenReportingCore(fixtureRoot, List.of("atf.reporting"), violations);
        assertEquals(List.of(file + " contains atf.reporting"), violations);
    }
    @Test
    void rootPomExposesTheCanonicalReactorModuleCatalog() throws Exception {
        Path root = locateProjectRoot();
        var deployment = GovernanceDeployment.read(root);
        assertEquals(deployment.reportingSource() ? AtfModuleIds.REACTOR_MODULES : AtfModuleIds.REACTOR_MODULES.stream()
                        .filter(module -> !module.equals(AtfModuleIds.REPORTING))
                        .filter(module -> !module.equals(AtfModuleIds.REPORTING_TESTS)
                                || deployment.sourceModules().contains(AtfModuleIds.REPORTING_TESTS)).toList(), deployment.sourceModules());
    }

    @Test
    void productionModulesDoNotImportForbiddenLayersOrMarkup() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<String>();
        for (AtfGovernanceRules.SourceRule rule : AtfGovernanceRules.PRODUCTION_SOURCE_RULES) {
            if (rule.module().equals(AtfModuleIds.REPORTING) && !GovernanceDeployment.read(root).reportingSource())
                violations.addAll(ReportingGovernanceSurface.read(root).forbiddenReferences(rule.forbiddenTokens()));
            else assertNoForbidden(moduleSource(root, rule.module(), false), rule.forbiddenTokens(), violations);
        }
        assertTrue(violations.isEmpty(), () -> "Architecture boundary violations: " + violations);
    }

    @Test
    void modulePomsKeepRendererNeutralDependencyDirection() throws Exception {
        Path root = locateProjectRoot();
        assertAll(AtfGovernanceRules.MODULE_DEPENDENCY_RULES.stream()
                .map(rule -> () -> {
                    if (rule.required()) {
                        assertDependencyPresent(root, rule.module(), rule.dependency());
                    } else if (rule.module().equals(AtfModuleIds.REPORTING)) {
                        assertProductionDependencyAbsent(root, rule.module(), rule.dependency());
                    } else {
                        assertDependencyAbsent(root, rule.module(), rule.dependency());
                    }
                }));
    }

    @Test
    void reportingProductionCodeDoesNotReachIntoExecutionOrBusinessValidation() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<String>();
        if (GovernanceDeployment.read(root).reportingSource())
            assertNoForbiddenReportingCore(moduleSource(root, AtfModuleIds.REPORTING, false),
                    AtfGovernanceRules.REPORTING_FORBIDDEN_PRODUCTION_PACKAGES, violations);
        else violations.addAll(ReportingGovernanceSurface.read(root).forbiddenReferences(
                AtfGovernanceRules.REPORTING_FORBIDDEN_PRODUCTION_PACKAGES));
        assertNoForbidden(moduleSource(root, AtfModuleIds.CONSOLE, false),
                List.of("atf.reporting", "com.aventstack"), violations);
        assertTrue(violations.isEmpty(), () -> "Reporting boundary violations: " + violations);
    }

    @Test
    void concreteExecutionFixtureIsTestOnlyAndGenericExecutionIsCoreOwned() throws Exception {
        Path root = locateProjectRoot();
        assertTrue(Files.notExists(root.resolve("atf-execution")),
                "the retired concrete atf-execution module must not return");
        assertTrue(Files.isRegularFile(root.resolve(
                        "atf-core/src/main/java/atf/core/lifecycle/CleanupRegistry.java")),
                "generic lifecycle cleanup belongs to core");
        ReportingHarnessOwnership.read(root).requireFile(root.resolve(
                "atf-reporting-tests/src/test/java/atf/reporting/api/execution/ApiModule.java"));
    }

    @Test
    void everyAtfDependencyDeclaredByReportingIsTestOnly() throws Exception {
        Path root = locateProjectRoot();
        assertNoAtfDependencies(root, AtfModuleIds.REPORTING);
    }

    @Test
    void reportingCompositionIsExplicitlyTestOnlyAndConsumesStandaloneReporting() throws Exception {
        ReportingHarnessOwnership.read(locateProjectRoot()).inspect(() -> {
            Path root = locateProjectRoot();
            assertDependencyScope(root, AtfModuleIds.REPORTING_TESTS, AtfModuleIds.REPORTING, "test");
            assertAllAtfDependenciesAreTestScoped(root, AtfModuleIds.REPORTING_TESTS);
        });
    }

    @Test
    void testOnlyCompositionModulesConsumeFrameworkModulesOnlyInTestScope() throws Exception {
        Path root = locateProjectRoot();
        assertAll(AtfModuleIds.TEST_ONLY_COMPOSITION_MODULES.stream()
                .filter(GovernanceDeployment.read(root).sourceModules()::contains)
                .map(module -> () -> assertAllAtfDependenciesAreTestScoped(root, module)));
    }

    @Test
    void processStreamOutputBelongsOnlyToTheConsoleModule() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<String>();
        String stdoutToken = "System." + "out";
        String stderrToken = "System." + "err";
        String stackTraceToken = "print" + "StackTrace";
        for (String module : AtfModuleIds.SOURCE_GOVERNED_MODULES) {
            if (module.equals(AtfModuleIds.CONSOLE)) continue;
            for (Path sourceRoot : List.of(
                    sourceRoots(root, module, false).getFirst(),
                    sourceRoots(root, module, true).getFirst())) {
                if (!Files.isDirectory(sourceRoot)) continue;
                try (var files = Files.walk(sourceRoot)) {
                    files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            if (source.contains(stdoutToken)) violations.add(path + " owns direct stdout output");
                            if (source.contains(stderrToken)) violations.add(path + " owns direct stderr output");
                            if (source.contains(stackTraceToken)) violations.add(path + " leaks a direct stack trace");
                        } catch (Exception error) {
                            violations.add("cannot read " + path + ": " + error.getMessage());
                        }
                    });
                }
            }
        }
        assertTrue(violations.isEmpty(), () ->
                "Only atf-console may own process stream output: " + violations);
    }

    @Test
    void productionModulesDoNotDeclareMutableStaticFields() throws Exception {
        Path root = locateProjectRoot();
        var violations = new ArrayList<String>();
        for (String module : AtfModuleIds.SOURCE_GOVERNED_MODULES) {
            Path sourceRoot = sourceRoots(root, module, false).getFirst();
            if (!Files.isDirectory(sourceRoot)) continue;
            try (var files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                    try {
                        int lineNumber = 0;
                        for (String line : Files.readAllLines(path)) {
                            lineNumber++;
                            String trimmed = line.trim();
                            if (trimmed.startsWith("//") || trimmed.startsWith("*")
                                    || trimmed.contains(" static final ") || trimmed.startsWith("static final ")) continue;
                            if (trimmed.matches(".*\\bstatic\\s+(volatile\\s+)?[A-Za-z_$][\\w$<>?, .\\[\\]]*\\s+[A-Za-z_$][\\w$]*\\s*(=.*)?;.*")) {
                                violations.add(path + ":" + lineNumber + " mutable static field");
                            }
                        }
                    } catch (Exception error) {
                        violations.add("cannot read " + path + ": " + error.getMessage());
                    }
                });
            }
        }
        assertTrue(violations.isEmpty(), () -> "Mutable static state found: " + violations);
    }
}
