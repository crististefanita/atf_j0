package atf.reporting.runtime.core;

import atf.reporting.runtime.section.ExtentReportRuntimeEvidence;
import atf.reporting.runtime.section.ExtentReportRuntimeNavigation;
import atf.reporting.runtime.section.ExtentReportRuntimeOverview;
import atf.reporting.runtime.section.ExtentReportRuntimeTests;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the ownership boundary between the publication pipeline and its runtime controllers. */
@AtfUnitTest
class ExtentReportRuntimeScriptTest {
    private static final String EMPTY_HTML_DOCUMENT =
            "<html><head></head><body></body></html>";

    @Test
    void runtimeFacadeComposesControllersInStableOrder() {
        String html = ExtentReportRuntimeBehaviour.inject(EMPTY_HTML_DOCUMENT);

        int duration = html.indexOf("function aggregateClassDurations()");
        int failures = html.indexOf("function renderFails()");
        int causes = html.indexOf("function failureCauseChain");

        assertAll(
                () -> assertTrue(html.contains("id=\"atf-report-behaviour\"")),
                () -> assertTrue(html.contains("__atfRuntimeBoot")),
                () -> assertTrue(html.contains("__atfRuntimeReady")),
                () -> assertTrue(html.contains("__atfRuntimeError")),
                () -> assertTrue(duration >= 0),
                () -> assertTrue(failures > duration),
                () -> assertTrue(causes > failures));
    }

    @Test
    void semanticControllersKeepTheirOwnRuntimeBoundaries() {
        String core = ExtentReportRuntimeCore.script();
        String tests = ExtentReportRuntimeTests.script();
        String overview = ExtentReportRuntimeOverview.script();
        String evidence = ExtentReportRuntimeEvidence.script();
        String navigation = ExtentReportRuntimeNavigation.script();
        String failureRenderer = evidence.substring(evidence.indexOf("function appendUiFailureEvidence("),
                evidence.indexOf("function markLastSemanticEvidence("));

        assertAll(
                () -> assertTrue(core.contains("function q("),
                        "Core must own shared DOM/query primitives"),
                () -> assertTrue(core.contains("function testItems("),
                        "Core must own the canonical test projection"),
                () -> assertTrue(tests.contains("function refreshFilterSelection("),
                        "Tests must own filters and scenario projection"),
                () -> assertTrue(tests.contains("function bindTestFilters("),
                        "Tests must own Tests-tab filter interactions"),
                () -> assertTrue(tests.contains("data-filter-value=\"skip\""),
                        "Categories must expose the Skip status filter"),
                () -> assertTrue(tests.contains("function categoryHasStatus("),
                        "Categories must reconcile status filters from summary and rendered rows"),
                () -> assertTrue(tests.contains("wanted==='pass'"),
                        "Categories must expose the Pass status predicate"),
                () -> assertTrue(tests.contains("wanted==='fail'"),
                        "Categories must expose the Fail status predicate"),
                () -> assertTrue(tests.contains("wanted==='warning'"),
                        "Categories must expose the Warning status predicate"),
                () -> assertTrue(tests.contains("wanted==='skip'"),
                        "Categories must expose the Skip status predicate"),
                () -> assertTrue(tests.contains("categoryHasStatus(item,atfCategoryFilterState.status)"),
                        "Categories must apply the semantic status predicate to every category flow"),
                () -> assertTrue(overview.contains("function populateAudit("),
                        "Overview must own overview/audit projection"),
                () -> assertFalse(overview.contains("function ensureStatusSummaryCard("),
                        "Overview must not synthesize warning/skip summary cards"),
                () -> assertFalse(overview.contains("Tests Warning")
                                || overview.contains("Tests Skipped")
                                || overview.contains("normalizeStatusSummaryCards(scenarioCounts)"),
                        "Warning and skip outcomes must remain in test evidence, not Overview cards"),
                () -> assertTrue(overview.contains("h.textContent=passLikeCount(scenarioCounts)")
                                && overview.contains("h.textContent=scenarioCounts.fail"),
                        "pass-like and FAIL cards must use the same scenario-level projection"),
                () -> assertTrue(overview.contains("function normalizeChartPalettes("),
                        "Overview must own chart normalization"),
                () -> assertTrue(evidence.contains("function normalizeEvidenceDisclosures("),
                        "Evidence must own disclosure normalization"),
                () -> assertTrue(evidence.contains("||card.querySelector('table.table')"),
                        "Evidence normalization must support legacy card bodies without a collapse wrapper"),
                () -> assertTrue(evidence.contains(".helm-request-details-content,.helm-response-details-content"),
                        "Semantic evidence normalization must discover legacy Request/Response rows directly"),
                () -> assertTrue(evidence.contains("function markLastSemanticEvidence()"),
                        "Evidence normalization must mark the final disclosure so the frame can close dynamically"),
                () -> assertTrue(evidence.contains("function appendUiFailureEvidence("),
                        "Failed UI actions must expose one compact, explicit Fail evidence row"),
                () -> assertTrue(evidence.contains("atf-ui-failure-evidence-row"),
                        "The explicit UI failure row must have a stable semantic hook"),
                () -> assertTrue(evidence.contains("function assertionFailureTable(failure)"),
                        "Assertion failures must reuse the validation-table renderer"),
                () -> assertTrue(evidence.contains("<th>ASSERTION</th><th>EXPECTED</th><th>ACTUAL</th><th>RESULT</th>"),
                        "dynamic assertion failures must align with the canonical assertion columns"),
                () -> assertTrue(failureRenderer.contains("atf-ui-failure-evidence")
                                && failureRenderer.contains(" assertion-evidence"),
                        "dynamic assertion renderer must publish the canonical assertion-evidence hook"),
                () -> assertTrue(evidence.contains("details.step-detail.assertion-evidence .helm-validation-table"),
                        "Assertion failures must be discovered after legacy evidence is normalized"),
                () -> assertTrue(evidence.contains("atf-validation-table-wrap"),
                        "assertion normalization must preserve the bounded table wrapper"),
                () -> assertTrue(evidence.contains("assertionContainer||assertion"),
                        "assertion normalization must move the wrapper, not strip it from the disclosure"),
                () -> assertTrue(evidence.contains("function assignNodeRoles("),
                        "Evidence must own semantic node roles"),
                () -> assertTrue(evidence.contains("data-atf-network-aggregate"),
                        "Network aggregate cards must expose a stable responsive layout hook"),
                () -> assertTrue(evidence.contains("function structuralNodeRole(card)"),
                        "Node roles must follow the typed scenario/phase/request/validation hierarchy"),
                () -> assertTrue(evidence.contains("function isNetworkAggregateCard(card)"),
                        "Network aggregate layout must follow its evidence, not its title"),
                () -> assertTrue(navigation.contains("window.atfOpenTest=function"),
                        "Navigation must own deep links into Tests"),
                () -> assertTrue(navigation.contains("function bindNodeCollapse("),
                        "Navigation must own disclosure/collapse interaction"),
                () -> assertTrue(navigation.contains("function nodePanels(card)"),
                        "a request must toggle its own evidence body and nested validation accordion"));
    }

    @Test
    void coreUsesTypedFunctionAndSelectorVocabularyWithOneDeclarationPerOwner() {
        String script = ExtentReportRuntimeCore.script();

        for (ExtentReportRuntimeVocabulary.Function function : ExtentReportRuntimeVocabulary.Function.values()) {
            assertEquals(1, count(script, "function " + function.value() + "("),
                    () -> "Core function must have one declaration: " + function);
        }

        assertAll(
                () -> assertTrue(script.contains("'.test-item'"),
                        "Test ownership must use the canonical test-item selector"),
                () -> assertTrue(script.contains("'.card-title .node'"),
                        "Scenario identity must use the canonical scenario-title selector"),
                () -> assertTrue(script.contains("'skip'"),
                        "Status projection must keep SKIP as a semantic value"),
                () -> assertTrue(script.contains("'warn'"),
                        "Status projection must use the typed warning match token"),
                () -> assertTrue(script.contains("function testPackageNode(host)"),
                        "Package projection must have one DOM fallback seam"),
                () -> assertTrue(script.contains(".detail-head .p-v-10 > .m-t-10"),
                        "Package projection must read the package shown below the test name"),
                () -> assertTrue(script.contains("setAttribute('data-atf-test-package',packageName)"),
                        "Package projection must publish the resolved value on the test root"),
                () -> assertTrue(script.contains("className='atf-test-package'"),
                        "Generated package metadata must keep its semantic CSS class"),
                () -> assertTrue(script.contains("data-atf-scenario-id"),
                        "The canonical scenario identity must remain projected on the DOM"));
    }

    @Test
    void durationControllerOwnsAggregationAndNavigationHooks() {
        String script = ExtentReportDurationRuntime.script();

        assertAll(
                () -> assertTrue(script.contains("function aggregateClassDurations()")),
                () -> assertTrue(script.contains(
                                ".detail-head .info > .badge-default:not(.uri-anchor):not(.pointer)"),
                        "duration normalization must not rewrite the deep-link or expand/collapse controls"),
                () -> assertTrue(script.contains("function normalizeHeaderMetadata()"),
                        "header metadata cleanup must be explicit and isolated from duration normalization"),
                () -> assertTrue(script.contains("/^\\s*·\\s*$/"),
                        "the vendor separator must not remain after the semantic deep-link is hidden"),
                () -> assertFalse(script.contains(
                                "function normalizeDurations(){q('.node-info .badge-default,.test-list-item .text-sm span,.test-detail .text-sm span,.detail-head .info > .badge-default')"),
                        "the emitted runtime must not use the legacy broad badge selector"),
                () -> assertTrue(script.contains("function directStructuralChildren(card)")),
                () -> assertTrue(script.contains("function directCardHeader(card)"),
                        "duration badges must be read from the node's own header, never from a descendant clone"),
                () -> assertTrue(script.contains("function directNode(card)"),
                        "each projected card must retain the measurement on its own Extent node"),
                () -> assertTrue(script.contains("function nodeIdentity(card)"),
                        "projected cards must resolve their measured duration through the stable Extent node id"),
                () -> assertTrue(script.contains("function nodePathKey(card)"),
                        "projected cards without a preserved id must resolve through their structural title path"),
                () -> assertTrue(script.contains("atfMeasuredDurationsByNodeId=Object.create(null)"),
                        "source-tree measurements must survive vendor-generated detail clones"),
                () -> assertTrue(script.contains("atfMeasuredDurationsByPath=Object.create(null)"),
                        "structural-path measurements must survive id rewriting by the vendor renderer"),
                () -> assertTrue(script.contains("function rememberMeasuredDurations()"),
                        "original vendor durations must be captured before structural aggregation mutates containers"),
                () -> assertTrue(script.contains("#test-view .test-list-item .test-contents .card > .card-header"),
                        "the measurement map must be populated only from the immutable source tree"),
                () -> assertTrue(script.contains("data-atf-measured-duration-ms"),
                        "the original measurement must also travel with cloned badges when Extent clones markup"),
                () -> assertTrue(script.contains("[directNode(card),card]")
                                && script.contains(".find(v=>v!=null&&v!==\"\")"),
                        "cloned Extent nodes must prefer their own immutable measured duration"),
                () -> assertTrue(script.contains("#test-view .test-list-item .test-contents .card > .card-header"),
                        "measurement capture must read each card's own direct header, never a descendant header"),
                () -> assertTrue(script.contains("function measuredNodeDuration(card)"),
                        "leaf nodes must retain their own measured duration across repeated clone passes"),
                () -> assertTrue(script.contains("function(id){rememberMeasuredDurations();atfBaseActivate(id)"),
                        "original durations must be captured before the vendor activation mutates the tree"),
                () -> assertTrue(script.contains("function isStructuralNode(card)"),
                        "duration aggregation must remain correct while a projected clone is being assigned semantic roles"),
                () -> assertTrue(script.contains("function directActionDuration(card)")),
                () -> assertTrue(script.contains("function directTransportEvidence(card)"),
                        "HTTP request nodes must retain their own measured exchange duration when they contain validation children"),
                () -> assertTrue(script.contains("function aggregateChildDurations(card)"),
                        "child traversal must remain separate from the duration decision"),
                () -> assertTrue(script.contains("function durationDecision(card,children)"),
                        "duration precedence must be explicit and independently testable"),
                () -> assertTrue(script.contains("function aggregateNodeDuration(card)")),
                () -> assertTrue(script.contains("function aggregateNodeDurations()")),
                () -> assertTrue(script.contains("function aggregateVisibleNodeDurations()"),
                        "the visible detail clone must be recalculated independently of the hidden source tree"),
                () -> assertTrue(script.contains("function allScenarioCards()"),
                        "the source scenario tree and the visible detail clone must share duration aggregation"),
                () -> assertTrue(script.contains("function setNodeDuration(card,value,source)")),
                () -> assertTrue(script.contains("if(value===null)return") && script.contains("source==='measured'"),
                        "unknown timing must not fabricate a badge and source measurements must remain available"),
                () -> assertTrue(script.contains("function executionTiming(card)")
                                && script.contains("source:'execution'") && script.contains("\"publication\"")
                                && script.contains("phase&&executionTiming(phase)")
                                && script.contains("source:source"),
                        "caller execution and snapshot publication must retain separate sources; browser probes verify totals"),
                () -> assertTrue(script.contains("atfFormatDuration(value)"),
                        "aggregated values must use the common display formatter"),
                () -> assertTrue(script.contains("Math.max(children.total,ownAction)"),
                        "a vendor duration already inclusive of its child must not be added twice"),
                () -> assertTrue(script.contains("function classDurationSummary(root)"),
                        "class aggregation must separate source-card collection from projection"),
                () -> assertTrue(script.contains("function publishClassDuration(root,total)"),
                        "class aggregation must keep DOM publication in one focused helper"),
                () -> assertTrue(script.contains("filter(durationChildVisible)"),
                        "filtered-out scenarios must not contribute to the visible class total"),
                () -> assertTrue(script.contains("data-duration-source',source")),
                () -> assertTrue(script.contains("data-duration-source")),
                () -> assertTrue(script.contains("a.linked[test-id],a.linked[data-atf-test-id]"),
                        "category duration projection must read the immutable source links before the detail clone exists"),
                () -> assertTrue(script.contains("categoryScenarioIndex(link,id)"),
                        "category projection must reconcile each linked scenario with its exact scenario index"),
                () -> assertTrue(script.contains("document.querySelector('#dashboard-view .card:has(#timeline)')"),
                        "overview duration projection must remain observable when the chart canvas is created lazily"),
                () -> assertTrue(script.contains("window.atfActivate")),
                () -> assertTrue(script.contains("scheduleDurationAggregationAt(150)"),
                        "a deep-linked Tests view must be recalculated after asynchronous DOM activation"),
                () -> assertTrue(script.contains("scheduleDurationAggregationAt(500)"),
                        "class rendering may complete after the initial Extent activation callback"),
                () -> assertTrue(script.contains("function watchDurationNodes()"),
                        "late scenario insertion must trigger duration recalculation"),
                () -> assertTrue(script.contains("function bindDurationClassClicks()"),
                        "switching between API test classes must trigger a fresh class-total calculation"),
                () -> assertTrue(script.contains("function scheduleDurationAggregationAfterClassClick()"),
                        "class selection must be rechecked after the vendor finishes rendering the detail"),
                () -> assertTrue(script.contains("scheduleDurationAggregation()"),
                        "the mutation observer must coalesce recalculation requests"),
                () -> assertTrue(script.contains("testItems();aggregateNodeDurations()"),
                        "late aggregation must assign scenario identities before walking the tree"),
                () -> assertTrue(script.contains("DOMContentLoaded")));
    }

    @Test
    void filterRecalculationReusesTheStructuralDurationController() {
        String html = ExtentReportRuntimeBehaviour.inject(EMPTY_HTML_DOCUMENT);

        assertTrue(html.contains("refreshFilterSelection();aggregateClassDurations();captureDomContract();"),
                "changing a Tests filter must recalculate the visible structural totals");
    }

    @Test
    void failureControllerOwnsDiagnosticProjectionAndDeepLink() {
        String script = ExtentReportFailureRuntime.script();

        assertAll(
                () -> assertTrue(script.contains("failureContextFor")),
                () -> assertTrue(script.contains("atf-failure-index")),
                () -> assertTrue(script.contains("Open in Tests")),
                () -> assertTrue(script.contains("failureLink(") && script.contains("failure-ordinal")
                                && script.contains("failure-kind") && script.contains("href="),
                        "the Fails link must retain exact native hash identity without relying on inline handlers"),
                () -> assertTrue(script.contains("encodeURIComponent"),
                        "the direct Fails fallback must preserve the exact test and scenario identifiers"),
                () -> assertTrue(script.contains("function renderFails(")),
                 () -> assertTrue(script.contains("function collectFailureState(item)")),
                 () -> assertTrue(script.contains("function failureContextFromState(state)")),
                 () -> assertTrue(script.contains("function failureEvidenceCandidate(card)"),
                         "Fails must recognize the explicit runtime Failure evidence card"),
                 () -> assertTrue(script.contains("runtimeFailures"),
                         "runtime failure evidence must be kept separate from semantic validations"),
                 () -> assertTrue(script.contains("state.runtimeFailures[0]||state.uiFailures[0]"),
                         "runtime failure evidence must win over a later green page validation"),
                 () -> assertTrue(script.contains("ctx.step='Failure evidence'"),
                         "Fails must label the explicit runtime failure step"),
                 () -> assertTrue(script.contains("function failureRecords()")),
                 () -> assertTrue(script.contains("function failureRow(record,index)")),
                 () -> assertEquals(1, count(script, "function failureContext(item)")),
                 () -> assertEquals(1, count(script, "function renderFails()")),
                 () -> assertTrue(script.contains("function failurePhaseRank(phase)")));
    }

    @Test
    void failureCauseProjectionKeepsPrimaryAndSecondaryPhaseEvidence() {
        String script = ExtentReportFailureRuntime.causeChainScript();

        assertAll(
                () -> assertTrue(script.contains("failureCauseChain")),
                () -> assertTrue(script.contains("PRIMARY: ")),
                () -> assertTrue(script.contains("SECONDARY: ")),
                 () -> assertTrue(script.contains("scopeFailureContextToPhase")),
                 () -> assertFalse(script.contains("atfOriginalFailureContext")),
                 () -> assertFalse(script.contains("failureContext=function")));
    }

    @Test
    void teardownThenActionsReuseTheValidationDisclosureContract() {
        String html = ExtentReportRuntimeBehaviour.inject(EMPTY_HTML_DOCUMENT);

        assertAll(
                () -> assertTrue(html.contains("if(parentRole==='request')return isNetworkAggregateCard(card)?'request':'validation'"),
                        "validation cards must be classified by typed parent-child position"),
                () -> assertTrue(html.contains("data-atf-node-role=\"validation\"]"),
                        "the validation styling and disclosure selectors must apply to the THEN card"),
                () -> assertTrue(html.contains("function technicalEvidenceType(card)"),
                        "technical evidence cards must be classified from their typed evidence table"),
                () -> assertTrue(html.contains("data-atf-evidence-family=\"technical\""),
                        "API traces must retain their technical evidence family"),
                () -> assertTrue(html.contains("api-trace"),
                        "API traces must remain distinguishable by typed evidence metadata"),
                () -> assertFalse(html.contains("RUNTIME DIAGNOSTIC")
                                || html.contains("runtime-diagnostic"),
                        "runtime diagnostics are not a published report evidence type"),
                () -> assertTrue(html.contains("function isNetworkAggregateCard(card)"),
                        "the synthetic inspector must be identified by its evidence, not its title"),
                () -> assertTrue(html.contains("assignNodeRoles();aggregateClassDurations();normalizeEvidenceDisclosures()"),
                        "opening a scenario must recalculate the visible cloned phase tree"),
                () -> assertTrue(html.contains("if(next){assignNodeRoles();aggregateClassDurations();normalizeEvidenceDisclosures();bindPhaseCollapse();"),
                        "manual scenario expansion must normalize semantic evidence before the user inspects it"),
                () -> assertTrue(html.contains("function bindNodeCollapse(role)")
                                && html.contains("data-atf-node-open"),
                        "the THEN card must receive the same plus/minus interaction contract as UI validation"));
    }

    private static int count(String value, String token) {
        int occurrences = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            occurrences++;
            offset += token.length();
        }
        return occurrences;
    }
}
