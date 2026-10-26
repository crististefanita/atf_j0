package atf.reporting.theme.tokens;
import atf.testkit.AtfUnitTest;
import atf.reporting.theme.AtfReportTheme;
import atf.reporting.support.CssContractSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportThemeTokensTest {
    @Test
    void tokenProviderOwnsStableVisualContractWithoutRendererRules() {
        String tokens = AtfReportThemeTokens.cssVariables();

        assertAll(
                () -> assertTrue(tokens.startsWith(":root{")),
                () -> assertTrue(tokens.contains("--atf-dark:#101723")),
                () -> assertTrue(tokens.contains("--atf-bg:#101723")),
                () -> assertTrue(tokens.contains("--atf-surface:#101723")),
                () -> assertTrue(tokens.contains("--atf-panel:#101723")),
                () -> assertTrue(tokens.contains("--atf-panel-2:#101723")),
                () -> assertTrue(tokens.contains("--atf-border:#29364d")),
                () -> assertTrue(tokens.contains("--atf-text:#e5ecf7")),
                () -> assertTrue(tokens.contains("--atf-muted:#91a3bf")),
                () -> assertTrue(tokens.contains("--atf-accent:#7aa2ff")),
                () -> assertTrue(tokens.contains("--atf-info:#04a1f4")),
                () -> assertTrue(tokens.contains("--atf-code:#070a0e")),
                () -> assertTrue(tokens.contains("--atf-json-text:#e6edf3")),
                () -> assertTrue(tokens.contains("--atf-code-text:#d7e2f4")),
                () -> assertTrue(tokens.contains("--atf-table-text:#c8d3e2")),
                () -> assertTrue(tokens.contains("--atf-table-label:#aebdd1")),
                () -> assertTrue(tokens.contains("--atf-card-text:#ddd")),
                () -> assertTrue(tokens.contains("--atf-overview-muted:#8192ad")),
                () -> assertTrue(tokens.contains("--atf-annotation-muted:#8192ADFA")),
                () -> assertTrue(tokens.contains("--atf-pass:#9ccc65")),
                () -> assertTrue(tokens.contains("--atf-fail:#e57373")),
                () -> assertTrue(tokens.contains("--atf-warning:#ffc107")),
                () -> assertTrue(tokens.contains("--atf-pass-text:#fff")),
                () -> assertTrue(tokens.contains("--atf-fail-text:#fff")),
                () -> assertTrue(tokens.contains("--atf-warning-text:#f6f7f6")),
                () -> assertTrue(tokens.contains("--atf-skip-text:#2b2100")),
                () -> assertTrue(tokens.contains("--atf-info-text:#002238")),
                () -> assertTrue(tokens.contains("--atf-badge-height:18.2px")),
                () -> assertTrue(tokens.contains("--atf-badge-padding:0 5px")),
                () -> assertTrue(tokens.contains("--atf-badge-radius:4px")),
                () -> assertTrue(tokens.contains("--atf-badge-font-size:9.52px")),
                () -> assertTrue(tokens.contains("--atf-badge-line-height:18.2px")),
                () -> assertTrue(tokens.contains("--atf-meta-badge-height:22.4px")),
                () -> assertTrue(tokens.contains("--atf-meta-badge-padding:5px 7px")),
                () -> assertTrue(tokens.contains("--atf-meta-badge-radius:4px")),
                () -> assertTrue(tokens.contains("--atf-meta-badge-font-size:9.52px")),
                () -> assertTrue(tokens.contains("--atf-meta-badge-line-height:10.6624px")),
                () -> assertTrue(tokens.contains("--atf-evidence-status-column-width:52px")),
                () -> assertTrue(tokens.contains("--atf-evidence-summary-padding:9px 12px")),
                () -> assertTrue(tokens.contains("--atf-evidence-summary-height:34px")),
                () -> assertTrue(tokens.contains("--atf-evidence-aggregate-summary-height:68px")),
                () -> assertEquals(2 * Integer.parseInt(AtfReportThemeTokens.EVIDENCE_SUMMARY_HEIGHT
                                .replace("px", "")),
                        Integer.parseInt(AtfReportThemeTokens.EVIDENCE_AGGREGATE_SUMMARY_HEIGHT
                                .replace("px", "")),
                        "aggregate summaries must remain exactly twice the compact baseline"),
                () -> assertTrue(tokens.contains("--atf-table-scroll-height:500px")),
                () -> assertTrue(tokens.contains("--atf-evidence-table-scroll-height:500px")),
                () -> assertTrue(tokens.contains("--atf-audit-scroll-height:500px")),
                () -> assertEquals(AtfReportThemeTokens.TABLE_SCROLL_HEIGHT,
                        AtfReportThemeTokens.EVIDENCE_TABLE_SCROLL_HEIGHT),
                () -> assertEquals(AtfReportThemeTokens.TABLE_SCROLL_HEIGHT,
                        AtfReportThemeTokens.AUDIT_SCROLL_HEIGHT),
                () -> assertTrue(tokens.contains("--atf-evidence-radius:10px")),
                () -> assertTrue(tokens.contains("--atf-base-line-height:1.55")),
                () -> assertTrue(tokens.contains("--atf-scenario-line-height:1.55")),
                () -> assertTrue(tokens.contains("--atf-scenario-node-min-height:18.76px")),
                () -> assertTrue(tokens.contains("--atf-scenario-header-min-height:38.5px")),
                () -> assertFalse(tokens.contains("--atf-category-badge-"),
                        "all report tabs must consume the shared badge geometry"),
                () -> assertTrue(tokens.contains("--atf-accent12:#7AA2FF1F")),
                () -> assertTrue(tokens.contains("--atf-accent14:#7AA2FF24")),
                () -> assertTrue(tokens.contains("--atf-font-sans:" + AtfReportTypography.SANS_FONT_FAMILY)),
                () -> assertTrue(tokens.contains("--atf-good:#35c759")),
                () -> assertTrue(tokens.contains("--atf-bad:#ff6b81")),
                () -> assertTrue(!tokens.contains("--atf-panel:#172235")),
                () -> assertTrue(!tokens.contains(".card") && !tokens.contains("#test-view"),
                        "tokens must not absorb renderer selectors"));
    }

    @Test
    void executionThemeUsesReferenceCardAndTypographyContract() {
        CssContractSupport css = CssContractSupport.of(AtfReportTheme.css());

        assertAll(
                () -> assertTrue(css.contains(".test-contents>.mt-4>.accordion>.card{background:transparent")),
                () -> assertTrue(css.contains("border-color:var(--atf-border)!important")),
                () -> assertTrue(css.contains("display:inline-flex!important;align-items:center!important;justify-content:center!important;font-size:var(--atf-badge-font-size)!important")),
                () -> assertTrue(css.contains(".test-view .test-list .test-item .text-sm>.badge{display:inline-flex!important;align-items:center!important;justify-content:center!important")
                                && css.contains(".test-view .test-list .test-item .text-sm>.badge")
                                && css.contains("height:var(--atf-badge-height)!important")
                                && css.contains("padding:var(--atf-badge-padding)!important"),
                        "Tests navigation badges must use the shared inline-flex geometry"),
                () -> assertTrue(css.contains(".helm-evidence-table.helm-request-table,.helm-evidence-table.helm-trace-table,.helm-evidence-table.execution-table{table-layout:fixed!important}")),
                () -> assertTrue(css.contains("--atf-evidence-key-column-width:28%")),
                () -> assertTrue(css.contains("width:var(--atf-evidence-key-column-width)!important")),
                () -> assertTrue(css.contains("white-space:normal!important;overflow-wrap:anywhere!important;word-break:break-word!important}")),
                () -> assertTrue(css.contains(".helm-evidence-table.helm-trace-table th:nth-child(2),.helm-evidence-table.helm-trace-table td:nth-child(2)")),
                () -> assertTrue(css.contains(".test-view .test-list-wrapper>.test-list-item>.test-item{display:flex")),
                () -> assertTrue(css.contains(".test-view .test-list-wrapper>.test-list-item>.test-item{background:var(--atf-dark)")),
                () -> assertTrue(css.contains(".test-view .test-list-tools .dropdown-menu,.category-view .test-list-tools .dropdown-menu{background:var(--atf-dark)!important")),
                () -> assertTrue(css.contains(".test-view .test-list-tools .dropdown-menu .dropdown-item,.category-view .test-list-tools .dropdown-menu .dropdown-item{background:var(--atf-dark)!important")),
                () -> assertTrue(css.contains(".category-view .test-list-tools .dropdown-menu .dropdown-item:hover")),
                () -> assertTrue(css.contains(".test-view .test-content-detail .card>.card-header .card-title .node::after")),
                () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-node-expandable='false']>.card-header:before{display:none!important")),
                () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-node-role=\"phase\"]")),
                   () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header{align-items:center!important}"),
                           "scenario header content must be vertically centered"),
                   () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-toolbar{align-self:center!important}"),
                           "scenario status and duration must be vertically centered"),
                   () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-title{display:grid!important;grid-template-columns:minmax(0,1fr)!important;grid-template-rows:auto auto!important;align-items:center!important;justify-items:start!important"),
                           "scenario title content must remain left aligned and vertically centered"),
                   () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-title{flex-direction:column!important;align-items:center!important;justify-content:flex-start!important;gap:2px!important}"),
                           "scenario title layout must not carry a contradictory vertical alignment"),
                   () -> assertTrue(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-title>a.node{grid-column:1!important;grid-row:1!important;width:100%!important;min-width:0!important;text-align:left!important;justify-content:flex-start!important}"),
                           "scenario titles and tags must share the same left edge"),
                   () -> assertTrue(css.contains("justify-self:start!important;align-self:center!important;display:flex!important;align-items:center!important;justify-content:flex-start!important"),
                           "scenario annotation tags must align below the title and be vertically centered"),
                () -> assertFalse(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header{align-items:flex-start!important}"),
                        "scenario headers must not regress to top alignment"),
                () -> assertFalse(css.contains(".test-view .test-content-detail .card[data-atf-scenario-id]>.card-header>.card-toolbar{align-self:flex-start!important}"),
                        "scenario status and duration must not regress to top alignment"),
                () -> assertTrue(css.contains("height:var(--atf-badge-height)!important;padding:var(--atf-badge-padding)!important;border:0!important;border-radius:var(--atf-badge-radius)!important"),
                        "status geometry must come from shared badge tokens"),
                () -> assertTrue(css.contains(".badge.badge-default{height:var(--atf-meta-badge-height)!important;min-height:var(--atf-meta-badge-height)!important;"),
                        "duration and metadata badges must use the separate compact meta geometry"),
                () -> assertTrue(css.contains(".test-view .detail-body>table thead th:nth-child(2)")),
                () -> assertTrue(css.contains(".test-view .test-content-detail table.table>thead>tr>th.timestamp-col")),
                () -> assertTrue(css.contains(".atf-fails-view .test-content-detail table.table>thead>tr>th.timestamp-col")),
                () -> assertTrue(css.contains(".test-view .test-list .test-item .test-detail .text-sm>span:first-child{display:none!important}")),
                () -> assertTrue(css.contains(".test-view .test-content-detail .detail-head .info>span.badge-success")),
                () -> assertTrue(css.contains(
                                ".test-view .test-content-detail .detail-head .info>.uri-anchor{display:none!important}"),
                        "the machine deep-link must not render as an unexplained badge"),
                () -> assertFalse(css.contains(".detail-head .info>span.badge-default:not(.uri-anchor):not(.pointer)::after{content:\" ms\"}"),
                        "duration units must be rendered by the shared normalizer, not a CSS pseudo-element"),
                () -> assertTrue(css.contains(".test-content h5.test-status{color:var(--atf-text)")),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-list-wrapper>.test-list-item>.test-item{border-left-width:1px!important"),
                        "Fails rows must use the same thin navigation rail as Tests"),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-list-wrapper>.test-list-item>.test-item:hover,.exception-view.atf-fails-view .test-list-wrapper>.test-list-item>.test-item.active{background:var(--atf-panel)!important}"),
                        "Fails selection must use the same active-row surface as Tests"),
                () -> assertTrue(css.contains(".exception-view.atf-fails-view .test-status .badge{display:inline-flex!important;align-items:center!important;justify-content:center!important;min-width:0!important;width:auto!important;height:var(--atf-badge-height)!important")
                                && css.contains("padding:var(--atf-badge-padding)!important;border:0!important;border-radius:var(--atf-badge-radius)!important")
                                && css.contains("font-size:var(--atf-badge-font-size)!important;line-height:var(--atf-badge-line-height)!important;font-weight:400!important}"),
                        "Fails status badges must match the compact tag-status badge contract"),
                () -> assertTrue(css.contains("data-atf-node-role=\"phase\"]{margin:12px 0!important")),
                () -> assertTrue(css.contains("border-left:3px solid var(--atf-accent)!important")),
                () -> assertTrue(css.contains("border-radius:8px!important")),
                () -> assertTrue(css.contains("background:#7AA2FF0B!important")),
                () -> assertTrue(css.contains("box-shadow:0 10px 26px #0000002E!important")),
                () -> assertTrue(css.contains("data-atf-phase-name=\"setup\"]{border-left-color:var(--atf-setup)")),
                () -> assertTrue(css.contains("data-atf-phase-name=\"teardown\"]{border-left-color:var(--atf-good)")),
                () -> assertTrue(css.contains("data-atf-node-role=\"validation\"]{border-left-width:1px!important}")),
                () -> assertTrue(css.contains("data-atf-evidence-family=\"technical\"]{border-left:2px solid var(--atf-accent)!important}"),
                        "technical diagnostic and inspector cards must use the same restrained accent rail"),
                () -> assertTrue(css.contains("data-atf-evidence-family=\"technical\"]>.card-header{background:var(--atf-accent08)!important}"),
                        "technical diagnostic and inspector headers must use the same accent surface"),
                () -> assertTrue(css.contains(".atf-tag-table{width:100%!important")
                        && css.contains("background-color:var(--atf-dark)!important;background-color:#101723!important")),
                () -> assertTrue(css.contains(".dark .dashboard-view .atf-tag-table{background-color:#101723!important}")),
                () -> assertTrue(css.contains(".dashboard-view .category-container>.card>.card-body>table")
                        && css.contains("background-color:var(--atf-dark)!important;background-color:#101723!important")),
                () -> assertTrue(css.contains(".info-bg,.info-bg.badge{background:var(--atf-info)")),
                () -> assertFalse(css.contains("color:#ddd"), "renderer CSS must consume the card-text token"),
                () -> assertFalse(css.contains("background:#172235"), "renderer CSS must consume the panel token"),
                () -> assertFalse(css.contains("color:#39bdf8"), "renderer CSS must consume the info token"),
                () -> assertTrue(!css.contains("#172235")),
                () -> assertTrue(!css.contains("#39bdf8")));
    }

    @Test
    void fallbackThemeReusesTheSamePaletteAndBoundedEvidenceRules() {
        CssContractSupport css = CssContractSupport.of(AtfReportTheme.fallbackCss());

        assertAll(
                () -> assertTrue(css.contains("--atf-panel:#101723")),
                () -> assertTrue(css.contains("background:var(--atf-dark)")),
                () -> assertTrue(css.contains("border-left:3px solid var(--atf-accent)")),
                () -> assertTrue(css.contains("border-radius:8px")),
                () -> assertTrue(css.contains("box-shadow:0 10px 26px #0000002E")),
                () -> assertTrue(css.contains("max-height:360px;overflow:auto")),
                () -> assertTrue(css.contains("font:12.5px/18.75px ui-monospace,Consolas,monospace")),
                () -> assertTrue(css.contains(".status-info{color:var(--atf-info-text);background:var(--atf-info)")),
                () -> assertFalse(css.contains("color:#ddd"), "fallback CSS must consume the card-text token"),
                () -> assertFalse(css.contains("background:#172235"), "fallback CSS must consume the panel token"),
                () -> assertFalse(css.contains("#172235")),
                () -> assertFalse(css.contains("#39bdf8")));
    }
}
