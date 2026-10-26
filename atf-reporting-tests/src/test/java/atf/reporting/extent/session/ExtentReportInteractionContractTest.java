package atf.reporting.extent.session;

import atf.testkit.AtfUnitTest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.support.TestReportPaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the semantic interaction/evidence tree.
 *
 * <p>Failure projection and tab navigation live in dedicated classes so this
 * class protects only the request/phase/evidence interaction boundary.</p>
 */
@AtfUnitTest
class ExtentReportInteractionContractTest {
    private static final String USERS = MockRoutePaths.USERS;
    private static final String USER = MockRoutePaths.userById("1");
    private static final String PROFILE = MockRoutePaths.profileById("1");
    private static final String USERS_OPERATION = ApiReportTestUrls.operation(HttpMethod.GET, USERS, "collection contract");
    private static final String USERS_REQUEST = ApiReportTestUrls.operation(HttpMethod.GET, USERS, null);
    private static final String PROFILE_OPERATION = ApiReportTestUrls.operation(HttpMethod.PUT, PROFILE, "failed transport contract");
    private static final String PROFILE_REQUEST = ApiReportTestUrls.operation(HttpMethod.PUT, PROFILE, null);

    @Test
    void testTreePublishesCollapsibleScenarioPhaseAndEvidenceContracts() throws Exception {
        Path report = TestReportPaths.report("extent-interaction-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", USERS_OPERATION,
                    List.of("api", "users", "contract"));

            var setup = scenario.startPhase("SETUP");
            setup.startRequest(USERS_REQUEST, HttpMethod.GET.value(), USERS)
                    .complete(AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);

            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest(USERS_REQUEST, HttpMethod.GET.value(), USERS);
            request.detail("RESPONSE DURATION (ms)", "12 ms");
            request.detail("URL", ApiReportTestUrls.example(USERS));
            request.detail("HTTP STATUS", "200");
            request.detail("Content-Type", "application/json");
            request.execution(new ApiExecutionResult(
                    new ApiResponse(200, "OK", Map.of("Content-Type", "application/json"),
                            Duration.ofMillis(12), ApiReportTestUrls.example(USERS)),
                    1, Duration.ofMillis(12), List.of(), List.of()));
            request.json("REQUEST JSON", "{\"limit\":10}");
            request.json("RESPONSE JSON", "[{\"id\":1,\"name\":\"ATF\"}]");
            var validation = request.startValidation("Validate: user collection contract");
            validation.assertions(List.of(
                    new AtfAssertion("HTTP status", "200", "200", true),
                    new AtfAssertion("JSON path exists: $[0].id", "present", "1", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);

            var teardown = scenario.startPhase("TEARDOWN");
            teardown.startRequest(ApiReportTestUrls.operation(HttpMethod.DELETE, USER, null),
                    HttpMethod.DELETE.value(), USER)
                    .complete(AtfReportStatus.PASS);
            teardown.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int validation = html.indexOf("<span>Validate: user collection contract</span>");
        int request = html.lastIndexOf(
                "<span>" + USERS_OPERATION + "</span>", validation);

        assertAll(
                () -> assertTrue(request >= 0 && validation > request,
                        "request must precede its nested validation substep"),
                () -> assertTrue(divDepthAt(html, validation) > divDepthAt(html, request),
                        "validation must be rendered inside the Gherkin request node, never as a phase sibling"),
                () -> assertTrue(html.contains("function collapseDirectScenarioCards()")),
                () -> assertTrue(containsJavaScriptSignature(html,
                                "function collapseScenarioCards(force,root,open)")),
                 () -> assertTrue(html.contains("function setDisclosureState(card,panel,open,attribute)"),
                         "all card transitions must update panel, data state, aria and glyph through one helper"),
                () -> assertTrue(html.contains("function bindScenarioCollapse()")),
                () -> assertTrue(html.contains("function bindPhaseCollapse()"),
                        "lifecycle phase headers must have an explicit collapse controller"),
                () -> assertTrue(html.contains("function toggleScenario(card,panel,e)")),
                () -> assertTrue(html.contains("function initializeScenarioCard(card,panel)")),
                () -> assertTrue(html.contains("data-atf-scenario-node-collapse-bound")),
                () -> assertTrue(html.contains("data-atf-scenario-open")),
                () -> assertTrue(html.contains("data-atf-phase-open")),
                () -> assertTrue(html.contains("data-atf-phase-header-bound")),
                () -> assertTrue(html.contains("e.stopPropagation()")),
                () -> assertTrue(html.contains("e.stopImmediatePropagation()")),
                () -> assertTrue(html.contains("data-atf-node-role=\"phase\"")),
                () -> assertTrue(html.contains("data-atf-node-role=\"request\"")),
                () -> assertTrue(html.contains("data-atf-node-role=\"validation\"")),
                () -> assertTrue(html.contains("function normalizeEvidenceDisclosures()")),
                () -> assertTrue(html.contains("function nodeEventTable(card)"),
                        "evidence normalization must support the live vendor panel class"),
                () -> assertTrue(html.contains("card.querySelector(':scope>.show')"),
                        "evidence normalization must handle Extent panels rendered as .show"),
                () -> assertTrue(html.contains("normalizeCodeBlocks();assignNodeRoles();aggregateClassDurations();normalizeEvidenceDisclosures();bindPhaseCollapse();bindNodeCollapse();if(card.scrollIntoView"),
                        "opening a lazily projected scenario must normalize its evidence after cloning"),
                () -> assertTrue(html.contains("function normalizeCategorySummaryLabels(){q('.category-view .test-contents .info')"),
                        "every category projection must normalize the native 'others' label before selection"),
                () -> assertTrue(html.contains("function normalizeChartPalettes()"),
                        "overview charts must use the same semantic palette as status badges"),
                () -> assertTrue(html.contains("['--atf-pass','--atf-fail','--atf-skip','--atf-skip','--atf-info']"),
                        "chart slices must map pass, fail, warning, skip and info to shared tokens"),
                () -> assertTrue(html.contains("<td>URL</td><td class=\"atf-evidence-url-value\">"
                                + ApiReportTestUrls.example(USERS) + "</td>"),
                        "technical trace must expose the request URL"),
                () -> assertTrue(html.contains("<td>RESPONSE URL</td><td class=\"atf-evidence-url-value\">"
                                + ApiReportTestUrls.example(USERS) + "</td>"),
                        "execution evidence must retain the effective URL"),
                () -> assertTrue(html.contains("<td>HTTP STATUS</td><td>200</td>"),
                        "technical trace must expose the response status"),
                () -> assertTrue(html.contains("Request details · 1 field"),
                        "request context must have its own disclosure"),
                () -> assertTrue(html.contains("Response details · 2 fields"),
                        "response metadata must have its own disclosure"),
                () -> assertTrue(html.contains("Execution evidence · 1 attempt"),
                        "execution evidence must have its own semantic label"),
                () -> assertTrue(html.contains(
                                "<details class=\"step-detail execution-evidence\"><summary aria-expanded=\"false\">"),
                        "execution evidence must be emitted as one closed disclosure, not loose Extent rows"),
                () -> assertFalse(html.contains("<details class=\"step-detail execution-evidence\" open"),
                        "execution evidence must be collapsed by default"),
                () -> assertTrue(html.contains("execution-table"),
                        "execution evidence must retain response execution fields"),
                () -> assertTrue(html.contains("execution-evidence"),
                        "execution evidence must be normalized into its own disclosure"),
                () -> assertTrue(html.contains("helm-request-details-content"),
                        "request details must have a dedicated semantic container"),
                () -> assertTrue(html.contains("helm-response-details-content"),
                        "response details must have a dedicated semantic container"),
                () -> assertTrue(html.contains("request-json-body"),
                        "request JSON must remain inside request details"),
                () -> assertTrue(html.contains("response-json-body"),
                        "response JSON must remain inside response details"),
                () -> assertFalse(html.contains("ROUTE TEMPLATE"),
                        "request evidence must not regress to the relative route label"),
                () -> assertTrue(html.contains("function normalizeDisclosureState()"),
                        "all card disclosures must share one accessible state synchronizer"),
                () -> assertTrue(html.contains("atf-disclosure-glyph"),
                        "disclosures must use one compact visual glyph"),
                () -> assertTrue(html.contains("aria-expanded"),
                        "card and evidence disclosure state must be keyboard-readable"),
                () -> assertTrue(html.contains("content:'＋'"),
                        "collapsed disclosure must render the typographic plus glyph"),
                () -> assertTrue(html.contains(">.card-header{display:flex!important;align-items:center!important"),
                        "card disclosure glyph, title and metadata must share one row"),
                () -> assertTrue(html.contains(">.card-header>.card-toolbar{order:2!important"),
                        "duration and status must be ordered after the title"),
                () -> assertTrue(html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header{align-items:center!important}")
                                && html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-toolbar{align-self:center!important}")
                                && html.contains("card[data-atf-scenario-id]>.card-header>.card-toolbar>ul,.test-view .test-content-detail .card[data-atf-node-role]>.card-header>.card-toolbar>ul{display:flex!important;align-items:center!important")
                                && html.contains("position:static!important;top:auto!important;float:none!important;display:flex!important;align-items:center!important"),
                        "scenario status and duration must be centered on the first title row, not on the tag row"),
                () -> assertTrue(html.contains("margin-left:auto!important;white-space:nowrap!important"),
                        "duration and status must stay on the right without wrapping"),
                () -> assertFalse(html.contains("[+] expand"),
                        "verbose expand text must not be rendered"),
                () -> assertFalse(html.contains("[-] collapse"),
                        "verbose collapse text must not be rendered"),
                () -> assertTrue(html.contains("data-atf-scenario-id")),
                () -> assertTrue(html.contains("function normalizeScenarioAnnotations()")),
                () -> assertTrue(html.contains(".card[data-atf-scenario-id]")),
                () -> assertTrue(html.contains("annotation.setAttribute('role','list')"),
                        "scenario tags must be exposed as an accessible inline list"),
                () -> assertTrue(html.contains("annotation.setAttribute('aria-label','Scenario annotations')")),
                () -> assertTrue(html.contains("atf-tag-annotation-selection")),
                () -> assertTrue(html.contains("title.querySelectorAll(':scope > .node-attr')"),
                        "native Extent tags must be replaced by one inline ATF tag list"),
                () -> assertTrue(html.contains("card.querySelectorAll(':scope > .accordion .node-attr')")),
                () -> assertTrue(html.contains(".atf-scenario-annotations{display:flex"),
                        "scenario tags must be styled inline like the .NET reference"),
                () -> assertTrue(html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header{align-items:center!important}")
                                && html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-toolbar{align-self:center!important}")
                                && html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-title{display:grid!important;grid-template-columns:minmax(0,1fr)!important;grid-template-rows:auto auto!important")
                                && html.contains("align-items:center!important;justify-items:start!important;row-gap:0!important;min-width:0!important}")
                                && html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-title>a.node{grid-column:1!important;grid-row:1!important;width:100%!important;min-width:0!important;text-align:left!important;justify-content:flex-start!important}")
                                && html.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-title>.atf-scenario-annotations{grid-column:1!important;grid-row:2!important;justify-self:start!important;align-self:center!important"),
                        "scenario title and annotations must stay left aligned, with the complete group vertically centered"),
                () -> assertTrue(html.contains("margin-top:0!important")
                                && html.contains("padding:0 3px!important;border:1px solid var(--atf-accent14)!important")
                                && html.contains("font-size:7.5px!important;line-height:9px!important")
                                && html.contains("color:var(--atf-annotation-muted)!important")
                                && html.contains(".test-view .test-content-detail .card-title>.atf-scenario-annotations>.atf-tag-annotation"),
                        "scenario annotations must remain compact, close to the title and softly muted"),
                () -> assertTrue(html.contains("line-height:18.2px!important;font-weight:400!important")
                                && html.contains("display:inline-block!important;padding:6px 10.5px!important")
                                && html.contains("height:18.2px!important;min-height:18.2px!important;padding:0 5px!important")
                                && html.contains("display:flex!important;align-items:center!important;gap:4px!important;height:var(--atf-badge-height)!important")
                                && html.contains("min-height:38.5px!important;height:auto!important"),
                        "request, validation and phase rows must use the compact vertical rhythm"),
                () -> assertTrue(html.contains("height:var(--atf-badge-height)!important;min-height:var(--atf-badge-height)!important;margin:0!important;padding:var(--atf-badge-padding)!important;line-height:var(--atf-badge-line-height)!important"),
                        "all status and duration badges in the test tree must reuse the shared geometry"),
                () -> assertTrue(html.contains("margin:4px 0!important;padding:0!important;border:1px solid var(--atf-border)!important")
                                && html.contains("border-left:1px solid var(--atf-border)!important;border-radius:6px!important")
                                && html.contains("background:var(--atf-panel-2)!important;box-shadow:none!important"),
                        "request and validation substeps must follow the restrained .NET card treatment"),
                () -> assertTrue(html.contains("data-atf-node-role=\"request\"]>.card-header{background:#7AA2FF0B!important}")
                                && html.contains("data-atf-node-role=\"validation\"]>.card-header{background:#FFFFFF06!important}"),
                        "request and validation substeps must remain visually distinct without overpowering the phase"),
                () -> assertTrue(html.contains("function bindNodeCollapse(role)")
                                && html.contains("data-atf-node-open")
                                && html.contains("bindNodeCollapse()"),
                        "request and validation disclosures must switch from plus to minus when opened"),
                () -> assertFalse(html.contains("details.atf-scenario-annotations"),
                        "the generic Scenario annotations disclosure must not replace the visible tags"),
                () -> assertTrue(html.contains("response-evidence")),
                () -> assertTrue(html.contains("execution-evidence")),
                () -> assertTrue(html.contains("Execution evidence")),
                () -> assertTrue(html.contains("kind==='response-evidence'"),
                        "response details must retain the TECHNICAL TRACE classification after DOM normalization"),
                () -> assertTrue(html.contains("json-evidence")),
                () -> assertTrue(html.contains("assertion-evidence")),
                () -> assertTrue(html.contains("helm-validation-table")),
                 () -> assertTrue(html.contains("attribute==='data-atf-node-open'?nodePanels(card):[panel]")
                                 && html.contains("panels.forEach(function(child){child.classList.toggle('show',open);")
                                 && html.contains("child.style.setProperty('display',open?'block':'none','important')"),
                         "the shared disclosure helper must own every direct panel's visible state"),
                 () -> assertTrue(html.contains("attribute=phase?'data-atf-phase-open':'data-atf-node-open'")
                                 && html.contains("setDisclosureState(card,panel,next,attribute)")),
                () -> assertFalse(html.contains("class=\"collapse show\""),
                        "evidence must start collapsed in the artifact")
        );
    }

    @Test
    void failedTransportRequestKeepsEvidenceZonesClosedAndSemanticallyDistinct() throws Exception {
        Path report = TestReportPaths.report("extent-failed-transport-disclosure-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests",
                    PROFILE_OPERATION,
                    List.of("api", "profile", "failure"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest(PROFILE_REQUEST,
                    HttpMethod.PUT.value(), PROFILE);
            request.detail("METHOD", "PUT");
            request.detail("URL", ApiReportTestUrls.example(PROFILE));
            request.detail("REQUEST HEADERS", "Content-Type: application/json");
            request.detail("HTTP STATUS", "200");
            request.detail("Content-Type", "application/json");
            request.json("REQUEST JSON", "{\"name\":\"ATF updated\"}");
            request.json("RESPONSE JSON", "{\"id\":1,\"name\":\"ATF updated\"}");
            request.execution(new ApiExecutionResult(
                    new ApiResponse(200, "OK", Map.of("Content-Type", "application/json"),
                            Duration.ofMillis(16), ApiReportTestUrls.example(PROFILE)),
                    1, Duration.ofMillis(16), List.of(), List.of()));

            var validation = request.startValidation("Contract validation");
            validation.assertions(List.of(
                    new AtfAssertion("HTTP status", "204", "200", false),
                    new AtfAssertion("JSON path equals: $.name", "ATF persisted", "ATF updated", false)));
            validation.complete(AtfReportStatus.FAIL);
            request.complete(AtfReportStatus.FAIL);
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Request details · 3 fields"),
                        "failed PUT must keep request context in its own zone"),
                () -> assertTrue(html.contains("Response details · 2 fields"),
                        "failed PUT must keep response metadata in its own zone"),
                () -> assertTrue(html.contains("Execution evidence · 1 attempt"),
                        "failed PUT must keep execution facts in its own zone"),
                () -> assertTrue(html.contains("request-json-body") && html.contains("response-json-body"),
                        "failed PUT must preserve both JSON bodies"),
                () -> assertTrue(html.contains("helm-validation-table"),
                        "failed PUT must preserve the validation assertion table"),
                () -> assertFalse(html.contains("<details class=\"step-detail request-evidence\" open")),
                () -> assertFalse(html.contains("<details class=\"step-detail response-evidence\" open")),
                () -> assertFalse(html.contains("<details class=\"step-detail execution-evidence\" open")),
                () -> assertTrue(html.indexOf("Request details · 3 fields")
                                < html.indexOf("Response details · 2 fields")
                                && html.indexOf("Response details · 2 fields")
                                < html.indexOf("Execution evidence · 1 attempt"),
                        "the failed transport evidence must retain request → response → execution order")
        );
    }

    @Test
    void overviewCountsWarningsAsPassLikeWithoutHidingTheWarningBucket() throws Exception {
        Path report = TestReportPaths.report("extent-overview-warning-pass-like-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("WarningTests", "warning remains actionable",
                    List.of("reporting", "warning"));
            scenario.complete(AtfReportStatus.WARNING);
        }

        String html = Files.readString(report);
        String javascript = compactJavaScript(html);
        assertAll(
                () -> assertTrue(javascript.contains(
                                "functionpassLikeCount(statuses){returnstatuses.pass+statuses.warning;}"),
                        "Overview must define one explicit pass-like policy for warning outcomes"),
                () -> assertTrue(javascript.contains(
                                "if(label==='TestsPassed')h.textContent=passLikeCount(scenarioCounts);"),
                        "the headline must not report zero tests when the only outcome is a warning"),
                () -> assertTrue(javascript.contains(
                                "updateOverviewSummaryFooter(overviewChartCard('parent-analysis'),classCounts,'testclasses')"),
                        "the Tests card must include warning outcomes in its pass-like total"),
                () -> assertTrue(javascript.contains(
                                "updateOverviewSummaryFooter(overviewChartCard('child-analysis'),scenarioCounts,'steps')"),
                        "the Steps card must include warning outcomes in its pass-like total"),
                () -> assertTrue(javascript.contains("passLike=passLikeCount(statuses)"),
                        "the shared Overview footer helper must use the pass-like policy"),
                () -> assertTrue(javascript.contains("statuses.warning")
                                && javascript.contains("scenarioCounts.warning")
                                && javascript.contains("warnings"),
                        "warning counts must remain visible after the pass-like total is corrected"),
                () -> assertTrue(html.contains("status=\"warning\""),
                        "the scenario status must remain Warning in the Tests tree"));
    }

    private static int divDepthAt(String html, int index) {
        String prefix = html.substring(0, index);
        int depth = 0;
        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.startsWith("<div", i)) {
                depth++;
            } else if (prefix.startsWith("</div>", i)) {
                depth--;
            }
        }
        return depth;
    }

    private static boolean containsJavaScriptSignature(String html, String signature) {
        return compactJavaScript(html).contains(compactJavaScript(signature));
    }

    private static String compactJavaScript(String value) {
        return value.replaceAll("\\s+", "");
    }
}
