package atf.reporting.artifact.contract;

import java.util.Map;

final class ReportArtifactOverviewContracts {
    private ReportArtifactOverviewContracts() { }

    static void evaluate(String html, Map<String, Boolean> checks) {
        checks.put("overview metadata", html.contains("ATF execution metadata (secondary)")
                && html.contains("ATF tag audit (secondary navigation metadata)"));
        checks.put("overview audit tables", html.contains("ATF scenario identities by test class")
                && html.contains("Test packages") && html.contains("Other annotations / tags"));
        checks.put("reference overview table structure", html.contains("atf-tag-card atf-tag-card-classes")
                && html.contains("atf-tag-card atf-tag-card-packages")
                && html.contains("atf-tag-card atf-tag-card-other")
                && html.contains("atf-tag-scroll") && html.contains("atf-tag-table"));
        checks.put("scenario identity projection", html.contains("var identity=c+' · '+testName(item)")
                && html.contains("auditRows('atf-audit-scenarios',scenarios,true,total,true)"));
        checks.put("overview omits warning and skip cards", !html.contains("function ensureStatusSummaryCard(")
                && !html.contains("Tests Warning") && !html.contains("Tests Skipped")
                && !html.contains("normalizeStatusSummaryCards(scenarioCounts)")
                && !html.contains("atf-status-summary-card"));
        checks.put("overview shared table contract", html.contains("--atf-secondary-heading-bg")
                && html.contains(".dashboard-view .category-container>.card>.card-header")
                && html.contains(".dashboard-view .sysenv-container>.card>.card-header")
                && html.contains("position:sticky!important;top:0!important"));
        boolean badgeTypography = html.contains("font-size:9.52px!important;line-height:14.756px!important")
                || (html.contains("--atf-font-badge-size:9.52px")
                && html.contains("font-size:var(--atf-font-badge-size)!important")
                && html.contains("line-height:1.12!important"));
        checks.put("overview summary typography contract", html.contains(".dashboard-view .card-body p.m-b-0")
                && html.contains(".dashboard-view .card-footer,.dashboard-view .card-footer *")
                && html.contains("font-size:14px!important;line-height:1.55!important;font-weight:400!important")
                && html.contains(".dashboard-view{padding:21px!important}")
                && html.contains(".dashboard-view .card-body h3{margin-bottom:7px!important}")
                && badgeTypography
                && html.contains(".dashboard-view .card:has(>.card-body #timeline)>.card-header>p")
                && html.contains(".atf-secondary-overview-title{")
                && html.contains("font-size:15.4px!important")
                && html.contains("line-height:21.56px!important")
                && html.contains("font-weight:400!important")
                && containsGlyphRule(html, ".atf-secondary-overview>summary:before", "＋")
                && containsGlyphRule(html, ".atf-secondary-overview[open]>summary:before", "−"));
        checks.put("overview excludes log events chart", html.contains("function removeUnsupportedOverviewCards()")
                && html.contains("#events-analysis")
                && html.contains("closest('.col-md-3')")
                && html.contains("==='log events'")
                && html.contains("(column||card||canvas).remove()"));
        checks.put("reference overview shape", html.contains("#grandchild-analysis")
                && html.contains(".card:has(>.card-body #grandchild-analysis)"));
        int timeline = html.indexOf("<p>Timeline</p>");
        int metadata = html.indexOf("ATF execution metadata (secondary)");
        int tagAudit = html.indexOf("ATF tag audit (secondary navigation metadata)");
        int nativeTags = html.indexOf("<p>Tags</p>");
        checks.put("overview order", timeline >= 0 && metadata > timeline
                && tagAudit > metadata && nativeTags > tagAudit);
    }

    private static boolean containsGlyphRule(String html, String selector, String glyph) {
        return html.contains(selector + "{content:'" + glyph + "'!important")
                || html.contains(selector + "{content:\"" + glyph + "\"!important");
    }
}
