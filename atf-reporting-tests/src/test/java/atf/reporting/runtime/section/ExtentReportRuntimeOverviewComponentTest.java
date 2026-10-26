package atf.reporting.runtime.section;

import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component contract for the internal Overview fragment graph. */
@AtfUnitTest
class ExtentReportRuntimeOverviewComponentTest {
    private static final Pattern FUNCTION = Pattern.compile("function\\s+([A-Za-z_$][\\w$]*)\\s*\\(");

    @Test
    void facadePublishesStableTypedFragments() {
        assertEquals(
                java.util.List.of("AUDIT", "LABELS", "CHARTS", "CLEANUP"),
                ExtentReportRuntimeOverview.fragments().stream().map(Enum::name).toList());

        assertAll(ExtentReportRuntimeOverview.fragments().stream()
                .map(fragment -> () -> assertFalse(fragment.script().isBlank(),
                        fragment.name() + " must publish a non-empty fragment")));
    }

    @Test
    void eachFragmentOwnsUniqueFunctionDeclarations() {
        var declarations = new HashSet<String>();

        for (ExtentReportRuntimeOverview.Fragment fragment : ExtentReportRuntimeOverview.fragments()) {
            Matcher matcher = FUNCTION.matcher(fragment.script());
            while (matcher.find()) {
                assertTrue(declarations.add(matcher.group(1)),
                        () -> "Duplicate Overview function owner: " + matcher.group(1));
            }
        }

        assertEquals(12, declarations.size(), "Overview function count must remain explicit and reviewable");
    }

    @Test
    void auditFragmentKeepsTheTotalDenominatorOutsideTheRowHelper() {
        String script = ExtentReportRuntimeOverview.script();

        assertAll(
                () -> assertTrue(script.contains("function auditRows(id,data,share,total,keepOrder)")),
                () -> assertTrue(script.contains("auditRows('atf-audit-scenarios',scenarios,true,total,true)")),
                () -> assertTrue(script.contains("auditRows('atf-audit-packages',packages,true,total,false)")),
                () -> assertTrue(script.contains("auditRows('atf-audit-tags',tags,false,total,false)")),
                () -> assertFalse(script.contains("function rows("),
                        "the local rows helper must become an explicit Overview boundary"),
                () -> assertTrue(script.contains("function populateAudit(")),
                () -> assertTrue(script.contains("function removeUnsupportedOverviewCards(")));
    }

    @Test
    void facadePreservesEveryVisibleOverviewResponsibility() {
        String script = ExtentReportRuntimeOverview.script();

        assertAll(
                () -> assertNotNull(script),
                () -> assertTrue(script.contains("function countStatuses(")),
                () -> assertTrue(script.contains("function normalizeTagTableHeaders(")),
                () -> assertTrue(script.contains("function normalizeNativeSummaryLabels(")),
                () -> assertTrue(script.contains("function normalizeCategorySummaryLabels(")),
                () -> assertTrue(script.contains("function normalizeChartPalettes(")),
                () -> assertTrue(script.contains("function normalizePercentages(")),
                () -> assertTrue(script.contains("function removeUnsupportedOverviewCards(")));
    }

    @Test
    void warningIsPassLikeForTotalsButRemainsVisibleAsAWarning() {
        String script = ExtentReportRuntimeOverview.script();

        assertAll(
                () -> assertTrue(script.contains("function passLikeCount(statuses)")),
                () -> assertTrue(script.contains("return statuses.pass+statuses.warning")),
                () -> assertTrue(script.contains("statuses.warning")),
                () -> assertTrue(script.contains("Scenario warnings")),
                () -> assertTrue(script.contains("updateOverviewSummaryFooter(")),
                () -> assertTrue(script.contains("overviewChartCard('parent-analysis')")),
                () -> assertTrue(script.contains("overviewChartCard('child-analysis')")));
    }
}
