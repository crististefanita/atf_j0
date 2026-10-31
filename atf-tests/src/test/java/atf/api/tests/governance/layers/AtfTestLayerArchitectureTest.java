package atf.api.tests.governance.layers;

import atf.api.tests.governance.distribution.ReportingGovernanceSurface;

import atf.testkit.AtfIntegrationTest;
import atf.api.tests.governance.AtfGovernanceTestSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enforces one executable test hierarchy across every ATF module. */
@AtfIntegrationTest
public class AtfTestLayerArchitectureTest {
    private static final Set<String> LAYER_ANNOTATIONS = Set.of(
            "@AtfUnitTest", "@AtfComponentTest", "@AtfIntegrationTest", "@AtfE2eTest");
    private static final Pattern TEST_DECLARATION = Pattern.compile(
            "(?m)^\\s*@(Test|ParameterizedTest|TestFactory|TestTemplate|RepeatedTest|Suite|SelectPackages)\\b");
    private static final Pattern ANNOTATION = Pattern.compile("(?m)^\\s*@([\\w.]+)\\b");
    private static final Pattern ANNOTATION_TYPE = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?@interface\\s+(\\w+)\\b");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?m)^\\s*(?:(?:public|protected|private|static|final|abstract|sealed|non-sealed)\\s+)*"
                    + "(?:class|interface|enum|record|@interface)\\s+\\w+");
    private static final String STANDALONE_REPORTING_LAYER = "standalone-independent";
    private static final Set<String> STANDALONE_REPORTING_TEST_NAMES = Set.of(
            "ReportingStandaloneContractTest.java", "ReportingStandaloneDependencyTest.java",
            "ReportingStandaloneEvidenceLayoutTest.java",
            "ReportingStandaloneEnvironmentSnapshotTest.java",
            "ReportingStandaloneVerdictComponentTest.java",
            "StandaloneBoundaryRules.java", "StandaloneBoundaryRulesTest.java",
            "ReportingModuleSource.java", "ReportingSourceArchitectureTest.java");

    @Test
    void testInventoryUsesConfiguredAndDefaultMavenRoots(@TempDir Path root) throws Exception {
        Path customModule = root.resolve("custom-module");
        Path defaultModule = root.resolve("default-module");
        Path customTest = customModule.resolve("src/test/fixture/CustomRootTest.java");
        Path customLayer = customModule.resolve("src/test/fixture/CustomLayer.java");
        Path defaultTest = defaultModule.resolve("src/test/java/fixture/DefaultRootTest.java");
        Files.createDirectories(customTest.getParent());
        Files.createDirectories(defaultTest.getParent());
        Files.writeString(customModule.resolve("pom.xml"), """
                <project><build><testSourceDirectory>src/test</testSourceDirectory></build></project>
                """);
        Files.writeString(defaultModule.resolve("pom.xml"), "<project/>\n");
        Files.writeString(customTest, "package fixture; class CustomRootTest {}\n");
        Files.writeString(customLayer, """
                package fixture;
                import atf.testkit.AtfComponentTest;
                @AtfComponentTest
                public @interface CustomLayer { }
                """);
        Files.writeString(defaultTest, "package fixture; class DefaultRootTest {}\n");

        assertAll(
                () -> assertEquals(Set.of(customTest, customLayer, defaultTest),
                        Set.copyOf(testSources(root))),
                () -> assertEquals(List.of("@AtfComponentTest"), declaredLayers(
                        """
                        package fixture;
                        import fixture.CustomLayer;
                        @CustomLayer
                        class Example {}
                        """,
                        annotationSources(root))));
    }

    @Test
    void everyConcreteExecutableTestDeclaresExactlyOneLayer() throws Exception {
        Path projectRoot = locateProjectRoot();
        Map<String, String> annotations = annotationSources(projectRoot);
        for (Path file : testSources(projectRoot)) {
            String source = Files.readString(file);
            if (isStandaloneReportingTest(file, projectRoot)) continue;
            if (!isConcreteExecutable(source)) continue;

            List<String> declared = declaredLayers(source, annotations);
            assertEquals(1, declared.size(),
                    () -> "Executable test must declare exactly one layer: " + file);
            assertNoForbiddenImports(file, projectRoot, source, declared.getFirst());
        }
    }

    @Test
    void supportDoesNotBecomeAHiddenExecutionLayer() throws Exception {
        Path projectRoot = locateProjectRoot();
        for (Path file : testSources(projectRoot)) {
            if (!file.toString().replace('\\', '/').contains("/support/")) continue;
            String source = Files.readString(file);
            if (!isConcreteExecutable(source)) continue;
            assertFalse(source.contains("@AtfE2eTest")
                            || source.contains("@AtfIntegrationTest"),
                    () -> "Support must not own browser/module execution: " + file);
        }
    }

    @Test
    void standaloneReportingUsesAFormalIndependentPolicy() throws Exception {
        Path projectRoot = locateProjectRoot();
        var surface = ReportingGovernanceSurface.read(projectRoot);
        String pom = surface.pom();
        List<Path> standaloneTests = testSources(projectRoot).stream()
                .filter(file -> isStandaloneReportingTest(file, projectRoot))
                .toList();

        assertAll(
                () -> assertFalse(pom.contains("<parent>"),
                        "standalone reporting must not inherit the whole reactor"),
                () -> assertFalse(pom.matches("(?s).*<artifactId>atf-(core|mock|execution|validation|console|ui|debug-ui|test-kit)</artifactId>.*"),
                        "standalone reporting must not depend on an ATF module"),
                () -> assertEquals(STANDALONE_REPORTING_TEST_NAMES,
                        surface.source() ? standaloneTests.stream().map(path -> path.getFileName().toString())
                                .collect(java.util.stream.Collectors.toSet()) : surface.nativeTestManifest(),
                        "standalone reporting tests must remain an explicit, finite exception"));

        if (!surface.source()) {
            surface.assertBinaryLayout();
            assertTrue(surface.forbiddenReferences(List.of("atf.core", "atf.validation", "atf.testkit", "org.junit")).isEmpty(),
                    "The loaded library must retain the standalone binary boundary");
        }
        for (Path file : standaloneTests) {
            String source = Files.readString(file);
            assertFalse(LAYER_ANNOTATIONS.stream().anyMatch(source::contains),
                    () -> "standalone reporting exception must not silently claim a reactor layer: " + file);
            assertTrue(source.contains("atf.reporting"),
                    () -> "standalone reporting test must stay within the reporting boundary: " + file);
            assertFalse(source.lines().anyMatch(line -> line.matches(
                            "\\s*import\\s+atf\\.api\\.(core|execution|validation|mock|console|ui|debugui|testkit)\\..*")),
                    () -> "Standalone reporting test imports another ATF module: " + file);
        }
    }

    @Test
    void moduleLayerMatrixIsDerivedFromDeclaredAnnotations() throws Exception {
        Path projectRoot = locateProjectRoot();
        Map<String, String> annotations = annotationSources(projectRoot);
        Map<String, Set<String>> matrix = new TreeMap<>();
        for (Path file : testSources(projectRoot)) {
            String source = Files.readString(file);
            if (!isConcreteExecutable(source)) continue;
            String relative = projectRoot.relativize(file).toString().replace('\\', '/');
            String module = relative.substring(0, relative.indexOf('/'));
            List<String> declared = isStandaloneReportingTest(file, projectRoot)
                    ? List.of(STANDALONE_REPORTING_LAYER)
                    : declaredLayers(source, annotations);
            assertEquals(1, declared.size(),
                    () -> "Executable test must declare exactly one layer: " + file);
            String layer = declared.getFirst();
            matrix.computeIfAbsent(module, ignored -> new TreeSet<>()).add(layer);
        }

        assertFalse(matrix.isEmpty(), "the live reactor must expose an executable test matrix");
        var reporting = ReportingGovernanceSurface.read(projectRoot);
        if (reporting.source()) assertEquals(Set.of(STANDALONE_REPORTING_LAYER), matrix.get("atf-reporting"),
                () -> "atf-reporting must be represented as an independent test surface: " + matrix);
        else {
            reporting.assertBinaryLayout();
            assertFalse(matrix.containsKey("atf-reporting"), "A binary dependency cannot masquerade as a local test source");
        }
        assertTrue(matrix.values().stream().allMatch(layers -> !layers.isEmpty()),
                () -> "matrix contains an empty module layer: " + matrix);
    }

    public static boolean isConcreteExecutable(String source) {
        return TEST_DECLARATION.matcher(source).find()
                && !source.matches("(?s).*\\babstract\\s+class\\s+\\w+.*");
    }

    @Test
    void composedLayerPreservesTheCanonicalLayerAndRejectsAmbiguousDeclarations() {
        var annotations = Map.of("fixture.Component", "@AtfComponentTest\npublic @interface Component {}");
        assertAll(
                () -> assertEquals(List.of("@AtfComponentTest"), declaredLayers(
                        "import fixture.Component;\n@Component\nclass Example {}", annotations)),
                () -> assertEquals(List.of("@AtfUnitTest", "@AtfComponentTest"), declaredLayers(
                        "import fixture.Component;\n@AtfUnitTest\n@Component\nclass Example {}", annotations)),
                () -> assertTrue(declaredLayers("@Unknown\nclass Example {}", annotations).isEmpty()));
    }

    @Test
    void nestedAnnotationResolutionTerminatesCyclesWithoutInventingALayer() {
        var annotations = Map.of(
                "fixture.Outer", "import fixture.Inner;\n@Inner\npublic @interface Outer {}",
                "fixture.Inner", "@AtfComponentTest\npublic @interface Inner {}",
                "fixture.Loop", "import fixture.Loop;\n@Loop\npublic @interface Loop {}");
        assertAll(
                () -> assertEquals(List.of("@AtfComponentTest"), declaredLayers(
                        "import fixture.Outer;\n@Outer\nclass Example {}", annotations)),
                () -> assertTrue(declaredLayers(
                        "import fixture.Loop;\n@Loop\nclass Example {}", annotations).isEmpty()));
    }

    @Test
    void nestedFixtureAnnotationsDoNotChangeTheEnclosingExecutableLayer() {
        assertEquals(List.of("@AtfComponentTest"), declaredLayers("""
                @AtfComponentTest
                class Executable {
                    @AtfUnitTest
                    static class Fixture { }
                }
                """, Map.of()));
    }

    public static List<String> declaredLayers(String source, Map<String, String> annotations) {
        return declaredAnnotations(source, annotations, LAYER_ANNOTATIONS);
    }

    /** Shared annotation graph for layer, reporting-marker and suite governance. */
    public static List<String> declaredAnnotations(String source, Map<String, String> annotations, Set<String> terminals) {
        return declaredAnnotations(source, annotations, terminals, new HashSet<>());
    }

    private static List<String> declaredAnnotations(String source, Map<String, String> annotations,
                                                   Set<String> terminals, Set<String> resolving) {
        source = atf.core.files.AtfJavaSourceReferences.codeOnly(source);
        var declaration = TYPE_DECLARATION.matcher(source);
        if (declaration.find()) source = source.substring(0, declaration.start());
        var imports = new HashMap<String, String>();
        IMPORT.matcher(source).results().forEach(match -> {
            String name = match.group(1);
            imports.put(name.substring(name.lastIndexOf('.') + 1), name);
        });
        var packageMatcher = PACKAGE.matcher(source);
        String packagePrefix = packageMatcher.find() ? packageMatcher.group(1) + "." : "";
        var result = new ArrayList<String>();
        ANNOTATION.matcher(source).results().forEach(match -> {
            String name = match.group(1);
            String layer = "@" + name.substring(name.lastIndexOf('.') + 1);
            if (terminals.contains(layer)) {
                result.add(layer);
                return;
            }
            String type = imports.getOrDefault(name, name.contains(".") ? name : packagePrefix + name);
            String composed = annotations.get(type);
            if (composed != null && resolving.add(type)) {
                result.addAll(declaredAnnotations(composed, annotations, terminals, resolving));
                resolving.remove(type);
            }
        });
        return List.copyOf(result);
    }

    public static Map<String, String> annotationSources(Path root) throws Exception {
        var result = new HashMap<String, String>();
        Set<Path> sources = new LinkedHashSet<>(AtfJavaSourceInventory.mainSources(root));
        sources.addAll(testSources(root));
        for (Path file : sources) {
            String source = Files.readString(file);
            var owner = PACKAGE.matcher(source);
            if (!owner.find()) continue;
            var type = ANNOTATION_TYPE.matcher(source);
            while (type.find()) {
                String prefix = source.substring(0, type.start()).replace("\r\n", "\n");
                int block = prefix.lastIndexOf("\n\n");
                String header = "package " + owner.group(1) + ";\n"
                        + IMPORT.matcher(source).results()
                                .map(match -> "import " + match.group(1) + ";\n")
                                .collect(java.util.stream.Collectors.joining());
                result.put(owner.group(1) + "." + type.group(1),
                        header + prefix.substring(block < 0 ? 0 : block));
            }
        }
        return Map.copyOf(result);
    }

    private static void assertNoForbiddenImports(Path file, Path projectRoot,
                                                  String source, String layer) {
        if ("@AtfUnitTest".equals(layer)) {
            assertFalse(source.lines().anyMatch(line -> line.matches(
                            "\\s*import\\s+com\\.microsoft\\.playwright\\..*")),
                    () -> "Unit test imports Playwright: " + file);
        }
        if ("@AtfComponentTest".equals(layer)) {
            String relative = projectRoot.relativize(file).toString().replace('\\', '/');
            boolean browserOwner = relative.startsWith("atf-ui/")
                    || relative.startsWith("atf-test-practice-shop/")
                    || relative.contains("/atf/ui/tests/extent/");
            if (!browserOwner) {
                assertFalse(source.lines().anyMatch(line -> line.matches(
                                "\\s*import\\s+com\\.microsoft\\.playwright\\..*")),
                        () -> "Non-UI component test imports Playwright: " + file);
            }
        }
    }

    static List<Path> testSources(Path projectRoot) throws Exception {
        return AtfJavaSourceInventory.testSources(projectRoot);
    }

    private static boolean isStandaloneReportingTest(Path file, Path projectRoot) {
        String relative = projectRoot.relativize(file).toString().replace('\\', '/');
        return relative.startsWith("atf-reporting/src/test/java/atf/reporting/standalone/");
    }

    private static Path locateProjectRoot() {
        return AtfGovernanceTestSupport.locateProjectRoot();
    }
}
