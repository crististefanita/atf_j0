package atf.reporting.runtime.section;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportRuntimeNavigationComponentTest {
    private static final List<String> FUNCTIONS = List.of(
            "normalizeCodeBlocks", "normalizeDisclosureState", "captureDomContract",
            "openScenarioCard", "setDisclosureState", "bindDisclosureKeyboard", "openScenarioFailurePath", "openScenarioAuditPath",
            "revealScenarioEvidence",
            "collapseDirectScenarioCards", "collapseScenarioCards",
            "toggleScenario", "initializeScenarioCard", "bindScenarioCollapse",
            "hasRelevantScenarioNodes", "watchScenarioCards", "phaseDirectBody", "applyPhaseBodyState",
            "bindPhaseCollapse", "nodePanel", "nodePanels", "bindNodeCollapse", "activateFromHash",
            "normalizeNavigationLinks", "normalizeTestViewContent",
            "scheduleTestViewNormalization", "scheduleTestViewNormalizationAt");

    @Test
    void navigationFacadePublishesOneStableDeclarationPerFunction() {
        String script = ExtentReportRuntimeNavigation.script();

        assertAll(
                () -> assertEquals(1, count(script, "window.atfOpenTest=function")),
                () -> assertEquals(1, count(script, "window.atfActivate=function")),
                () -> assertEquals(1, count(script, "document.addEventListener('DOMContentLoaded'")),
                () -> assertEquals(1, count(script, "window.addEventListener('load'")),
                () -> FUNCTIONS.forEach(function -> assertEquals(1,
                        count(script, "function " + function + "("),
                        () -> "Navigation function must have one declaration: " + function)));
    }

    @Test
    void navigationResponsibilitiesRemainSeparatedByComponent() {
        String dom = ExtentReportRuntimeNavigationDom.script();
        String disclosure = ExtentReportRuntimeNavigationDisclosure.script();
        String performance = ExtentReportRuntimeNavigationPerformance.script();
        String controller = ExtentReportRuntimeNavigationController.script();

        assertAll(
                () -> assertTrue(dom.contains("function captureDomContract(")),
                () -> assertTrue(dom.contains("window.__atfDomContract")),
                () -> assertTrue(disclosure.contains("function bindScenarioCollapse(")),
                 () -> assertTrue(disclosure.contains("function setDisclosureState(")),
                () -> assertTrue(disclosure.contains("function collapseScenarioCards(force,root,open)")),
                () -> assertTrue(disclosure.contains("!force&&card.getAttribute(attribute)==='true'")),
                () -> assertTrue(disclosure.contains("collapseScenarioCards(true,card)")),
                () -> assertFalse(disclosure.contains("collapseScenarioCards(true)"), "a local toggle cannot reset every owner"),
                () -> assertTrue(disclosure.contains("root.matches(atfScenarioCards)?[root]:q(atfScenarioCards,root)")),
                () -> assertTrue(disclosure.contains("function bindNodeCollapse(")),
                () -> assertTrue(disclosure.contains("function nodePanel(")),
                () -> assertTrue(disclosure.contains("function nodePanels(")),
                () -> assertTrue(disclosure.contains("attribute==='data-atf-node-open'?nodePanels(card):[panel]")),
                () -> assertTrue(disclosure.contains("child.style.setProperty('display',open?'block':'none','important')")),
                () -> assertTrue(disclosure.contains("function bindDisclosureKeyboard(")),
                () -> assertTrue(controller.contains("window.atfOpenTest=function")),
                () -> assertTrue(performance.contains("window.__atfReportPerformance")),
                () -> assertTrue(performance.contains("function atfPerformanceSnapshot(")),
                () -> assertTrue(performance.contains("ready:false")),
                () -> assertTrue(performance.contains("atfReportPerformanceState.ready=true")),
                () -> assertTrue(performance.contains("atfReportPerformanceState.ready=false")),
                () -> assertTrue(performance.contains("atfReportPerformanceState.ready?'true':'false'")),
                () -> assertTrue(performance.contains("function atfPerformanceRecordDisclosureToggle(")),
                () -> assertTrue(performance.contains("data-atf-performance-ready")),
                () -> assertTrue(performance.contains("pageLoadMs")),
                () -> assertTrue(performance.contains("longtask")),
                () -> assertTrue(performance.contains("data-atf-test-view-ready")),
                () -> assertTrue(performance.contains("atfPerformanceMarkReady")),
                () -> assertTrue(controller.contains("atfPerformanceBeginReadiness()")),
                () -> assertTrue(controller.contains("atfPerformanceEnd(startedAt,'normalizationDurationMs')")),
                () -> assertTrue(disclosure.contains("atfPerformanceCount('relevantMutationBatches')")),
                () -> assertTrue(disclosure.contains("atfPerformanceCount('scenarioToggles')")),
                () -> assertTrue(disclosure.contains("atfPerformanceCount(phase?'phaseToggles':'nodeToggles')")),
                () -> assertTrue(disclosure.contains("if(phase)applyPhaseBodyState(card,next)")),
                () -> assertTrue(disclosure.contains("atfPerformanceRecordDisclosureToggle(startedAt)")),
                () -> assertTrue(ExtentReportRuntimeNavigation.script().indexOf("window.__atfReportPerformance")
                        < ExtentReportRuntimeNavigation.script().indexOf("function normalizeTestViewContent(")),
                () -> assertTrue(controller.contains("function activateFromHash(")),
                () -> assertTrue(controller.contains("ui-validation")),
                () -> assertTrue(controller.contains("atfTestViewReadinessTimers")),
                () -> assertTrue(controller.contains("requestAnimationFrame")),
                () -> assertTrue(disclosure.contains("function hasRelevantScenarioNodes(")),
                () -> assertTrue(disclosure.contains(".test-contents > .mt-4")),
                () -> assertTrue(disclosure.contains("n.querySelector&&n.querySelector(s)")),
                () -> assertFalse(disclosure.contains("return record.addedNodes && record.addedNodes.length > 0;")),
                () -> assertFalse(disclosure.contains("q('.test-contents > .mt-4 > .accordion > .card').length")),
                () -> assertTrue(disclosure.contains("function openScenarioAuditPath(")),
                () -> assertTrue(controller.contains("function normalizeNavigationLinks(")),
                () -> assertFalse(dom.contains("window.atfActivate=function")),
                () -> assertFalse(disclosure.contains("window.atfActivate=function")));
    }

    private static int count(String source, String token) {
        int count = 0;
        int start = 0;
        while ((start = source.indexOf(token, start)) >= 0) {
            count++;
            start += token.length();
        }
        return count;
    }
}
