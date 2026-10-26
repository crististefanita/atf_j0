package atf.api.tests.governance.layers;

import atf.testkit.AtfIntegrationTest;
import atf.api.tests.governance.AtfGovernanceTestSupport;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves complete, disjoint source selection without launching external consumers. */
@AtfIntegrationTest
public class ConsumerSuiteCoverageGovernanceTest {
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+([\\w.]+)\\s*;");
    private static final Pattern SELECTED_PACKAGE = Pattern.compile("\"([\\w.]+)\"");
    private static final Pattern SELECTED_CLASS = Pattern.compile("([\\w.]+)\\.class");
    private static final Map<String, Set<String>> PRIMARY = Map.of(
            "atf-test-practice-shop", Set.of("ShopComponentSuite", "ShopE2eSuite", "ShopApiLiveSuite"),
            "atf-test-auto-ex", Set.of("AutoExComponentSuite", "AutoExE2eSuite", "AutoExApiLiveSuite"));

    @Test
    void everyConsumerExecutableHasExactlyOnePrimaryGateWithTheRightLayer() throws Exception {
        Path root = AtfGovernanceTestSupport.locateProjectRoot();
        var annotations = AtfTestLayerArchitectureTest.annotationSources(root);
        for (var module : PRIMARY.entrySet()) {
            var tests = new ArrayList<TestSource>();
            var suites = new ArrayList<Selection>();
            for (Path file : AtfTestLayerArchitectureTest.testSources(root.resolve(module.getKey()))) {
                String source = Files.readString(file);
                if (!AtfTestLayerArchitectureTest.isConcreteExecutable(source)) continue;
                String name = file.getFileName().toString().replace(".java", "");
                if (source.contains("@Suite")) {
                    assertTrue(module.getValue().contains(name), "Redundant consumer suite: " + file);
                    suites.add(selection(name, source));
                } else {
                    var layers = AtfTestLayerArchitectureTest.declaredLayers(source, annotations);
                    assertEquals(1, layers.size(), "Declare exactly one layer before selecting " + file);
                    tests.add(new TestSource(packageName(source) + "." + name, layers.getFirst()));
                }
            }
            assertEquals(module.getValue(), suites.stream().map(Selection::name)
                    .collect(java.util.stream.Collectors.toSet()), "Missing primary gate: " + module.getKey());
            assertTrue(!tests.isEmpty(), "Consumer inventory must not be empty: " + module.getKey());
            List<String> violations = violations(tests, suites);
            assertTrue(violations.isEmpty(), () -> module.getKey()
                    + " suite coverage: add missing selections; remove overlap, never tests: " + violations);
        }
    }

    @Test
    void coverageRejectsOmissionsOverlapAndWrongExecutionLayers() {
        var local = new TestSource("fixture.support.api.ContractTest", "@AtfComponentTest");
        var live = new TestSource("fixture.api.LiveTest", "@AtfIntegrationTest");
        var component = new Selection("ComponentSuite", List.of("fixture.component"), List.of());
        assertEquals(2, violations(List.of(local), List.of(component)).size());
        var complete = new Selection("ComponentSuite", List.of("fixture"), List.of());
        assertEquals(1, violations(List.of(local), List.of(complete, complete)).size());
        assertEquals(1, violations(List.of(live), List.of(complete)).size());
        assertTrue(violations(List.of(local), List.of(
                new Selection("ComponentSuite", List.of("fixture.support"), List.of()))).isEmpty());
    }

    @Test
    void selectorParserResolvesImportsAndRejectsNestedOrFilteredGates() {
        var parsed = selection("E2eSuite", """
                package fixture.suites;
                import fixture.journey.PurchaseTest;
                @SelectClasses(PurchaseTest.class)
                """);
        assertTrue(parsed.selects("fixture.journey.PurchaseTest"));
        assertTrue(!parsed.selects("fixture.other.PurchaseTest"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> selection("ComponentSuite", "@ExcludeTags(\"slow\") @SelectPackages(\"fixture\")"));
    }

    private static List<String> violations(List<TestSource> tests, List<Selection> suites) {
        var result = new ArrayList<String>();
        for (TestSource test : tests) {
            List<Selection> selected = suites.stream().filter(suite -> suite.selects(test.name())).toList();
            if (selected.size() != 1) {
                result.add(test.name() + " belongs to " + selected.size() + " gates: " + selected);
            } else if (!selected.getFirst().accepts(test.layer())) {
                result.add(test.name() + " has " + test.layer() + " inside " + selected.getFirst().name());
            }
        }
        for (Selection suite : suites) {
            if (tests.stream().noneMatch(test -> suite.selects(test.name()))) {
                result.add("Empty gate: " + suite.name());
            }
        }
        return result;
    }

    private static Selection selection(String name, String source) {
        if (source.contains("@Exclude") || source.contains("@IncludeTags")
                || source.contains("@IncludeClassNamePatterns")) {
            throw new IllegalArgumentException("Primary gates must be complete, unfiltered selections: " + name);
        }
        var packages = SELECTED_PACKAGE.matcher(arguments(source, "SelectPackages")).results()
                .map(match -> match.group(1)).toList();
        var imports = new java.util.HashMap<String, String>();
        IMPORT.matcher(source).results().forEach(match -> {
            String full = match.group(1);
            imports.put(full.substring(full.lastIndexOf('.') + 1), full);
        });
        var classes = SELECTED_CLASS.matcher(arguments(source, "SelectClasses")).results()
                .map(match -> imports.getOrDefault(match.group(1),
                        match.group(1).contains(".") ? match.group(1)
                                : packageName(source) + "." + match.group(1))).toList();
        if (packages.isEmpty() && classes.isEmpty()) {
            throw new IllegalArgumentException("Primary gate has no explicit selectors: " + name);
        }
        return new Selection(name, packages, classes);
    }

    private static String arguments(String source, String annotation) {
        var matcher = Pattern.compile("@" + annotation + "\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String packageName(String source) {
        var matcher = PACKAGE.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private record TestSource(String name, String layer) { }

    private record Selection(String name, List<String> packages, List<String> classes) {
        boolean selects(String test) {
            return classes.contains(test) || packages.stream().anyMatch(prefix -> test.startsWith(prefix + "."));
        }

        boolean accepts(String layer) {
            if (name.endsWith("E2eSuite")) return layer.equals("@AtfE2eTest");
            if (name.endsWith("ApiLiveSuite")) return layer.equals("@AtfIntegrationTest");
            return layer.equals("@AtfUnitTest") || layer.equals("@AtfComponentTest");
        }
    }
}
