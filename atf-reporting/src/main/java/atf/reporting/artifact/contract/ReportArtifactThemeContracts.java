package atf.reporting.artifact.contract;

import java.util.Map;

final class ReportArtifactThemeContracts {
    private ReportArtifactThemeContracts() { }

    static void evaluate(String html, Map<String, Boolean> checks) {
        checks.put("ATF dark theme", html.contains("id=\"atf-report-theme\"")
                && html.contains("--atf-dark:#101723"));
        checks.put("visual color contract", html.contains("--atf-panel:#101723")
                && html.contains("--atf-info:#04a1f4")
                && html.contains("--atf-json-text:#e6edf3")
                && !html.contains("--atf-panel:#172235")
                && !html.contains("#39bdf8"));
        checks.put("uniform report surfaces", html.contains("html,body,body.dark,.app,.layout")
                && html.contains(".main-content,.view,.test-content,.test-list")
                && html.contains("background:var(--atf-dark)!important"));
        checks.put("tests filter surface", html.contains(".test-view .test-list-tools .dropdown-menu,.category-view .test-list-tools .dropdown-menu{background:var(--atf-dark)!important")
                && html.contains(".test-view .test-list-tools .dropdown-menu .dropdown-item,.category-view .test-list-tools .dropdown-menu .dropdown-item{background:var(--atf-dark)!important")
                && html.contains(".category-view .test-list-tools .dropdown-menu .dropdown-item:hover"));
        checks.put("category filter surface", html.contains(".category-view .test-list-tools .dropdown-menu{background:var(--atf-dark)!important")
                && html.contains(".category-view .test-list-tools .dropdown-menu .dropdown-item"));
        checks.put("tests filter controls", html.contains("id=\"status-toggle\"")
                && html.contains("id=\"tag-toggle\"")
                && html.contains("class=\"dropdown-toggle\""));
        checks.put("tests filter behavior contract", html.contains("var atfFilterState={status:'',tag:''}")
                && html.contains("function filterMatches(item)")
                && html.contains("function applyTestFilters()")
                && html.contains("function setTestFilter(kind,value)")
                && html.contains("data-atf-filter-hidden")
                && html.contains("e.preventDefault();e.stopPropagation()")
                && html.contains("setTestFilter('status',status)")
                && html.contains("setTestFilter('tag',text(item).toLowerCase())")
                && html.contains("rootStatusMatches=!atfFilterState.status||statusOf(root)===atfFilterState.status")
                && html.contains("if(!selectedVisible&&firstVisible)"));
        checks.put("tests filter owner contract", html.contains(
                "testTags(item).some(function(tag){return tag.toLowerCase()===atfFilterState.tag;})")
                && html.contains("rootStatusMatches=!atfFilterState.status||statusOf(root)===atfFilterState.status")
                && html.contains("atfFilterState[kind]===normalized?'':normalized"));
        checks.put("tests filter state accessibility", html.contains("function refreshFilterSelection()")
                && html.contains("aria-pressed")
                && html.contains("data-atf-filter-bound")
                && html.contains("bindTestFilters()"));
        checks.put("tests filter vocabulary contract", html.contains("function normalizeTagFilter()")
                && html.contains("'failure':true")
                && html.contains("'reporting':true")
                && html.contains("'skip':true")
                && html.contains("'soft-assert':true")
                && html.contains("'teardown':true")
                && html.contains("'warning':true")
                && html.contains("normalizeTagFilter();bindTestFilters()"));
        checks.put("category filter contract", html.contains("var atfCategoryFilterState={status:'',tag:''}")
                && html.contains("category-status-toggle")
                && html.contains("category-tag-toggle")
                && html.contains("function applyCategoryFilters()")
                && html.contains("function setCategoryFilter(kind,value)")
                && html.contains("function ensureCategoryFilters()")
                && html.contains("data-atf-filter-hidden"));
        checks.put("phase shell contract", html.contains("data-atf-node-role=\"phase\"]{margin:12px 0!important")
                && html.contains("border-left:3px solid var(--atf-accent)!important")
                && html.contains("border-radius:8px!important")
                && html.contains("#7AA2FF0B")
                && html.contains("#0000002E"));
        checks.put("phase semantic accents", html.contains("data-atf-phase-name=\"setup\"]{border-left-color:var(--atf-setup)")
                && html.contains("data-atf-phase-name=\"test-body\"]{border-left-color:var(--atf-accent)")
                && html.contains("data-atf-phase-name=\"teardown\"]{border-left-color:var(--atf-good)"));
        checks.put("tests navigator contract", html.contains("test-view .test-list .test-item .test-detail .text-sm>span:first-child{display:none!important}")
                && html.contains("timestamp-col")
                && html.contains("data-atf-node-role=\"request\"]>.card-header .node-attr"));
        checks.put("category status palette", html.contains(".category-view .test-list .test-detail .meta .badge.log")
                && html.contains(".category-view .test-contents td .badge.log")
                && html.contains("height:var(--atf-badge-height)!important")
                && html.contains("padding:var(--atf-badge-padding)!important")
                && html.contains("border:0!important;border-radius:var(--atf-badge-radius)!important")
                && html.contains("font-size:var(--atf-badge-font-size)!important")
                && html.contains("min-width:0!important;width:auto!important")
                && !html.contains("--atf-category-badge-")
                && html.contains("badge.log.pass-bg,.category-view .test-contents .info .badge.log.pass-bg,.category-view .test-contents td .badge.log.pass-bg{background:var(--atf-pass)!important;color:var(--atf-pass-text)!important}")
                && html.contains("badge.log.badge-danger,.category-view .test-contents .info .badge.log.badge-danger,.category-view .test-contents td .badge.log.badge-danger{background:var(--atf-fail)!important;color:var(--atf-fail-text)!important}")
                && html.contains("badge.log.badge-skip,.category-view .test-contents .info .badge.log.badge-skip,.category-view .test-contents td .badge.log.badge-skip{background:var(--atf-skip)!important;color:var(--atf-skip-text)!important}")
                && html.contains("badge.log.badge-warning,.category-view .test-contents .info .badge.log.badge-warning,.category-view .test-contents td .badge.log.badge-warning{background:var(--atf-skip)!important;color:#fff!important}"));
        checks.put("status palette remains available for badges", html.contains("badge.log.badge-warning")
                && html.contains("badge.log.badge-skip"));
    }
}
