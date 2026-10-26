package atf.reporting.runtime.performance;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component proof for the configurable performance snapshot/budget boundary. */
@AtfComponentTest
class ReportRuntimePerformanceComponentTest {
    private static final ReportRuntimePerformanceBudget BUDGET =
            ReportRuntimePerformanceBudget.componentDefaults();

    @Test
    void mapsThePublishedRuntimeVocabularyIntoAnImmutableSnapshot() {
        Map<String, Object> values = new HashMap<>(Map.ofEntries(
                Map.entry("pageLoadMs", 420.4),
                Map.entry("readinessMs", 18.2),
                Map.entry("firstDisclosureToggleMs", 7.8),
                Map.entry("disclosureToggleDurationMs", 21.1),
                Map.entry("normalizationDurationMs", 10.2),
                Map.entry("normalizationSchedules", 3),
                Map.entry("normalizationRuns", 2),
                Map.entry("relevantMutationBatches", 1),
                Map.entry("durationMutationBatches", 1),
                Map.entry("disclosureToggles", 4),
                Map.entry("scenarioToggles", 1),
                Map.entry("phaseToggles", 1),
                Map.entry("nodeToggles", 2),
                Map.entry("longTaskCount", 0),
                Map.entry("longTaskDurationMs", 0),
                Map.entry("longTaskObservationSupported", true),
                Map.entry("ready", true)));

        ReportRuntimePerformanceSnapshot snapshot =
                ReportRuntimePerformanceSnapshot.from(values);

        assertAll(
                () -> assertTrue(snapshot.readinessAvailable()),
                () -> assertTrue(snapshot.firstDisclosureToggleAvailable()),
                () -> assertTrue(BUDGET.accepts(snapshot)),
                () -> assertTrue(BUDGET.violations(snapshot).isEmpty()));
    }

    @Test
    void keepsAConfigurableSixSignatureComponentFixtureInsideTheStarterBudget() {
        ReportRuntimePerformanceSnapshot snapshot = fixture(6, 6);

        assertAll(
                () -> assertTrue(snapshot.disclosureToggles() >= 6),
                () -> assertTrue(BUDGET.accepts(snapshot),
                        () -> "performance fixture violations: " + BUDGET.violations(snapshot)));
    }

    @Test
    void keepsTheGeneratedBrowserAuditBudgetDistinctFromTheComponentFixture() {
        ReportRuntimePerformanceSnapshot snapshot = fixture(7, 43, 16, 42);

        assertAll(
                () -> assertFalse(BUDGET.accepts(snapshot)),
                () -> assertTrue(ReportRuntimePerformanceBudget.browserAuditDefaults()
                                .accepts(snapshot),
                        () -> "browser budget violations: "
                                + ReportRuntimePerformanceBudget.browserAuditDefaults()
                                .violations(snapshot)));
    }

    @Test
    void keepsTheBrowserAuditLongTaskAllowanceBounded() {
        ReportRuntimePerformanceBudget browserBudget =
                ReportRuntimePerformanceBudget.browserAuditDefaults();
        ReportRuntimePerformanceSnapshot observedSchedulerVariance =
                fixture(7, 43, 16, 42, 12, 982);
        ReportRuntimePerformanceSnapshot excessiveSchedulerVariance =
                fixture(7, 43, 16, 42, 17, 1_501);

        assertAll(
                () -> assertTrue(browserBudget.accepts(observedSchedulerVariance)),
                () -> assertFalse(browserBudget.accepts(excessiveSchedulerVariance)),
                () -> assertTrue(browserBudget.violations(excessiveSchedulerVariance).stream()
                        .anyMatch(value -> value.startsWith("longTaskCount"))),
                () -> assertTrue(browserBudget.violations(excessiveSchedulerVariance).stream()
                        .anyMatch(value -> value.startsWith("longTaskDurationMs"))));
    }

    @Test
    void exposesBudgetViolationsWithoutChangingTheReportVerdict() {
        ReportRuntimePerformanceSnapshot slow = fixture(6, 6, 3_000, 1_400);

        assertAll(
                () -> assertFalse(BUDGET.accepts(slow)),
                () -> assertTrue(BUDGET.violations(slow).stream()
                        .anyMatch(value -> value.startsWith("readinessMs"))),
                () -> assertTrue(BUDGET.violations(slow).stream()
                        .anyMatch(value -> value.startsWith("firstDisclosureToggleMs"))));
    }

    private static ReportRuntimePerformanceSnapshot fixture(int signatures, int toggles) {
        return fixture(signatures, toggles, 180, 40);
    }

    private static ReportRuntimePerformanceSnapshot fixture(int signatures, int toggles,
                                                            long readinessMs,
                                                            long firstToggleMs) {
        return fixture(signatures, toggles, readinessMs, firstToggleMs, 0, 0);
    }

    private static ReportRuntimePerformanceSnapshot fixture(int signatures, int toggles,
                                                            long readinessMs,
                                                            long firstToggleMs,
                                                            long longTaskCount,
                                                            long longTaskDurationMs) {
        return new ReportRuntimePerformanceSnapshot(
                400 + signatures * 40L,
                readinessMs,
                firstToggleMs,
                firstToggleMs + Math.max(0, toggles - 1) * 12L,
                80 + signatures * 20L,
                4,
                3,
                signatures,
                signatures,
                toggles,
                Math.min(2, toggles),
                Math.min(2, toggles),
                Math.max(0, toggles - 4),
                longTaskCount,
                longTaskDurationMs,
                true,
                true);
    }
}
