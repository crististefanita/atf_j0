package atf.reporting.runtime.core;

import atf.core.files.AtfPathLocator;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the duration runtime seams without requiring a browser or a generated report. */
@AtfUnitTest
class ExtentReportDurationRuntimeComponentTest {
    private static final List<String> FUNCTION_NAMES = List.of(
            "atfDurationValue",
            "atfFormatDuration",
            "atfNormalizeDuration",
            "normalizeTestNavigatorSeparators",
            "normalizeHeaderMetadata",
            "normalizeDurations",
            "directCardHeader",
            "directNode",
            "durationBadge",
            "nodeLabel",
            "nodePathKey",
            "nodeIdentity",
            "rememberMeasuredDurations",
            "measuredNodeDuration",
            "durationChildVisible",
            "isStructuralNode",
            "directStructuralChildren",
            "directActionDuration",
            "directTransportEvidence",
            "setNodeDuration",
            "aggregateChildDurations",
            "durationDecision",
            "aggregateNodeDuration",
            "allScenarioCards",
            "aggregateVisibleNodeDurations",
            "aggregateNodeDurations",
            "scenarioCardFor",
            "normalizeCategoryDurations",
            "overviewTimelineEntries",
            "publishOverviewTimeline",
            "normalizeOverviewDuration",
            "normalizeFailsDurations",
            "normalizeProjectedDurations",
            "markDuration",
            "publishDuration",
            "classDurationSummary",
            "publishClassDuration",
            "aggregateClassDurations",
            "scheduleDurationAggregation",
            "scheduleDurationAggregationAt",
            "scheduleDurationAggregationAfterClassClick",
            "bindDurationClassClicks",
            "hasRelevantDurationNodes",
            "watchDurationNodes");

    @Test
    void facadeKeepsSevenRuntimeSeamsInExecutionOrder() {
        String parsing = ExtentReportDurationRuntime.Fragment.PARSING.source();
        String dom = ExtentReportDurationRuntime.Fragment.DOM.source();
        String memory = ExtentReportDurationRuntime.Fragment.MEMORY.source();
        String aggregation = ExtentReportDurationRuntime.Fragment.AGGREGATION.source();
        String projection = ExtentReportDurationRuntime.Fragment.PROJECTION.source();
        String scheduling = ExtentReportDurationRuntime.Fragment.SCHEDULING.source();
        String lifecycle = ExtentReportDurationRuntime.Fragment.LIFECYCLE.source();
        String script = ExtentReportDurationRuntime.script();

        assertAll(
                () -> assertFalse(parsing.isBlank()),
                () -> assertFalse(dom.isBlank()),
                () -> assertFalse(memory.isBlank()),
                () -> assertFalse(aggregation.isBlank()),
                () -> assertFalse(projection.isBlank()),
                () -> assertFalse(scheduling.isBlank()),
                () -> assertFalse(lifecycle.isBlank()),
                () -> assertEquals(
                        parsing + dom + memory + aggregation + projection + scheduling + lifecycle,
                        script),
                () -> assertTrue(parsing.contains("function atfDurationValue(")),
                () -> assertTrue(dom.contains("function directCardHeader(")),
                () -> assertTrue(memory.contains("function rememberMeasuredDurations(")),
                () -> assertTrue(aggregation.contains("function aggregateNodeDuration(")),
                () -> assertTrue(projection.contains("function aggregateClassDurations(")),
                () -> assertTrue(scheduling.contains("function scheduleDurationAggregation(")),
                () -> assertTrue(scheduling.contains("function scheduleDurationAggregationAt(")),
                () -> assertTrue(lifecycle.contains("function watchDurationNodes(")));
    }

    @Test
    void runtimeDeclaresEachDurationFunctionExactlyOnce() {
        String script = ExtentReportDurationRuntime.script();

        assertAll(FUNCTION_NAMES.stream()
                .map(name -> (org.junit.jupiter.api.function.Executable) () -> assertEquals(
                        1,
                        occurrences(script, "function " + name + "("),
                        "Expected one declaration for " + name))
                .toList());
    }

    @Test
    void boundariesKeepCrossCuttingResponsibilitiesSeparated() {
        String parsing = ExtentReportDurationRuntime.Fragment.PARSING.source();
        String dom = ExtentReportDurationRuntime.Fragment.DOM.source();
        String memory = ExtentReportDurationRuntime.Fragment.MEMORY.source();
        String aggregation = ExtentReportDurationRuntime.Fragment.AGGREGATION.source();
        String projection = ExtentReportDurationRuntime.Fragment.PROJECTION.source();
        String scheduling = ExtentReportDurationRuntime.Fragment.SCHEDULING.source();
        String lifecycle = ExtentReportDurationRuntime.Fragment.LIFECYCLE.source();

        assertAll(
                () -> assertFalse(parsing.contains("MutationObserver")),
                () -> assertFalse(dom.contains("aggregateNodeDuration")),
                () -> assertFalse(memory.contains("MutationObserver")),
                () -> assertFalse(aggregation.contains("normalizeCategoryDurations")),
                () -> assertFalse(projection.contains("MutationObserver")),
                () -> assertFalse(scheduling.contains("MutationObserver")),
                () -> assertTrue(scheduling.contains("atfDurationAggregationTimers")),
                () -> assertTrue(scheduling.contains("atfDurationAggregationTimers[key]")),
                () -> assertTrue(lifecycle.contains("function hasRelevantDurationNodes(")),
                () -> assertTrue(lifecycle.contains(".test-item,.test-detail,[data-duration-ms],[data-duration]")),
                () -> assertTrue(lifecycle.contains("n.querySelector&&n.querySelector(s)")),
                () -> assertFalse(lifecycle.contains("return record.addedNodes && record.addedNodes.length > 0;")),
                () -> assertFalse(lifecycle.contains("q('.test-contents > .mt-4 > .accordion > .card').length")),
                () -> assertFalse(lifecycle.contains("function aggregateNodeDuration(")),
                () -> assertTrue(
                        ExtentReportDurationRuntime.script().indexOf("function atfDurationValue(")
                                < ExtentReportDurationRuntime.script().indexOf("function aggregateNodeDuration(")),
                () -> assertTrue(
                        ExtentReportDurationRuntime.script().indexOf("function aggregateNodeDuration(")
                                < ExtentReportDurationRuntime.script().indexOf("function aggregateClassDurations(")),
                () -> assertTrue(
                        ExtentReportDurationRuntime.script().indexOf("function aggregateClassDurations(")
                                < ExtentReportDurationRuntime.script().indexOf("function watchDurationNodes(")));
    }

    @Test
    void typedDurationVocabularyOwnsBrowserContractTokens() {
        assertAll(
                () -> assertEquals("data-duration-ms",
                        ExtentReportDurationVocabulary.Attribute.DURATION_MS.value()),
                () -> assertEquals("#test-view",
                        ExtentReportDurationVocabulary.Selector.TEST_VIEW.value()),
                () -> assertEquals("aggregateClassDurations",
                        ExtentReportDurationVocabulary.Function.AGGREGATE_CLASS_DURATIONS.value()),
                () -> assertTrue(ExtentReportDurationVocabulary.jsString("#test-view").equals("'#test-view'")));
    }

    @Test
    void durationSeamsAreNativeAssetFacades() throws Exception {
        assertAll(
                () -> assertNativeFacade("ExtentReportDurationParsing.java", "durationParsingScript()"),
                () -> assertNativeFacade("ExtentReportDurationDom.java", "durationDomScript()"),
                () -> assertNativeFacade("ExtentReportDurationMemory.java", "durationMemoryScript()"),
                () -> assertNativeFacade("ExtentReportDurationAggregation.java", "durationAggregationScript()"),
                () -> assertNativeFacade("ExtentReportDurationProjectionLookup.java", "durationProjectionLookupScript()"),
                () -> assertNativeFacade("ExtentReportDurationProjectionViews.java", "durationProjectionViewsScript()"),
                () -> assertNativeFacade("ExtentReportDurationProjectionPublication.java",
                        "durationProjectionPublicationScript()"),
                () -> assertNativeFacade("ExtentReportDurationScheduling.java", "durationSchedulingScript()"),
                () -> assertNativeFacade("ExtentReportDurationLifecycle.java", "durationLifecycleScript()"));
    }

    private static void assertNativeFacade(String fileName, String composerCall) throws Exception {
        String source = Files.readString(sourceFile(fileName));
        assertAll(fileName,
                () -> assertTrue(source.contains("ReportAssetComposer." + composerCall)),
                () -> assertFalse(source.contains("function ")),
                () -> assertFalse(source.contains("String.join")),
                () -> assertFalse(source.contains("+ \"")));
    }

    private static Path sourceFile(String fileName) {
        return AtfPathLocator.locateSourceFile(Path.of(System.getProperty("user.dir", ".")),
                Path.of("atf/reporting/runtime/core", fileName),
                List.of(Path.of("src/main/java"), Path.of("atf-reporting/src/main/java")));
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(token, from)) >= 0) {
            count++;
            from += token.length();
        }
        return count;
    }
}
