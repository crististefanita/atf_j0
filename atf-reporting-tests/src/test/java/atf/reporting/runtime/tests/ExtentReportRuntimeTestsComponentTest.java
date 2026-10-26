package atf.reporting.runtime.tests;

import atf.reporting.runtime.section.ExtentReportRuntimeTests;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportRuntimeTestsComponentTest {
    private static final Pattern FUNCTION = Pattern.compile("function\\s+([A-Za-z0-9_]+)\\(");
    private static final String DIAGNOSTIC_NORMALIZER = "function normalizeScenarioDiagnostics(";

    @Test
    void facadePreservesTypedTestsFragmentOrder() {
        String expected = String.join("",
                ExtentReportRuntimeTestFilters.script(),
                ExtentReportRuntimeCategories.script(),
                ExtentReportRuntimeScenarios.script());

        assertEquals(expected, ExtentReportRuntimeTests.script());
        assertTrue(expected.indexOf("function applyTestFilters(")
                < expected.indexOf("function ensureCategoryFilters("));
        assertTrue(expected.indexOf("function ensureCategoryDetail(")
                < expected.indexOf("function normalizeScenarioAnnotations("));
    }

    @Test
    void eachTestsFunctionHasOneOwnerAndTheOwnersStayCohesive() {
        String script = ExtentReportRuntimeTests.script();
        Matcher matcher = FUNCTION.matcher(script);
        Set<String> names = new HashSet<>();

        while (matcher.find()) {
            assertTrue(names.add(matcher.group(1)),
                    () -> "Duplicate Tests runtime function: " + matcher.group(1));
        }

        assertEquals(28, names.size());
        assertTrue(ExtentReportRuntimeTestFilters.script().contains("function bindTestFilters("));
        assertTrue(ExtentReportRuntimeCategories.script().contains("function bindCategoryFilters("));
        assertTrue(ExtentReportRuntimeScenarios.script().contains("function normalizeScenarioAnnotations("));
        assertTrue(ExtentReportRuntimeScenarios.script().contains(DIAGNOSTIC_NORMALIZER));
        assertFalse(ExtentReportRuntimeTestFilters.script().contains(DIAGNOSTIC_NORMALIZER));
        assertFalse(ExtentReportRuntimeCategories.script().contains(DIAGNOSTIC_NORMALIZER));
        assertFalse(ExtentReportRuntimeTestFilters.script().contains("function selectCategory("));
        assertFalse(ExtentReportRuntimeCategories.script().contains("function applyTestFilters("));
        assertFalse(ExtentReportRuntimeScenarios.script().contains("function categoryFilterMarkup("));
    }
}
