package atf.reporting.artifact.contract;

import java.util.Map;

final class ReportArtifactNavigationContracts {
    private ReportArtifactNavigationContracts() { }

    static void evaluate(String html, Map<String, Boolean> checks) {
        checks.put("tab organization contract", html.contains("id=\"nav-test\" class=\"dropdown-toggle\" href=\"#test-view\"")
                && html.contains("id=\"nav-category\" class=\"dropdown-toggle\" href=\"#category-view\"")
                && html.contains("id=\"nav-fails\" class=\"dropdown-toggle\" href=\"#atf-fails-view\"")
                && html.contains("id=\"nav-overview\" class=\"dropdown-toggle\" href=\"#dashboard-view\"")
                && html.contains("data-atf-view=\"test-view\"")
                && html.contains("data-atf-view=\"category-view\"")
                && html.contains("data-atf-view=\"atf-fails-view\"")
                && html.contains("data-atf-view=\"dashboard-view\"")
                && html.contains("data-atf-tab=\"overview\"")
                && html.contains("data-atf-tab=\"fails\"")
                && html.contains("function activateFromHash()")
                && html.contains("window.addEventListener('hashchange',activateFromHash)"));
        int testsTab = html.indexOf("href=\"#test-view\"");
        int categoryTab = html.indexOf("href=\"#category-view\"");
        int failsTab = html.indexOf("href=\"#atf-fails-view\"");
        int overviewTab = html.indexOf("href=\"#dashboard-view\"");
        checks.put("tab order contract", testsTab >= 0 && testsTab < categoryTab
                && categoryTab < failsTab && failsTab < overviewTab);
        checks.put("filter preserves active tab", html.contains("e.preventDefault();e.stopPropagation()")
                && html.contains("location.hash")
                && html.contains("setTestFilter('status',status)")
                && html.contains("setTestFilter('tag',text(item).toLowerCase())"));
        checks.put("scenario filtering scope", html.contains("function directScenarioCards(root)")
                && html.contains("function isClassRoot(item)")
                && html.contains("function testTags(item)")
                && html.contains("classItems().forEach(function(root)")
                && html.contains("root.setAttribute('data-atf-filter-hidden'"));
        checks.put("fails navigation", html.contains("id=\"nav-fails\"")
                && html.contains("failure-id"));
        checks.put("category deep links", html.contains("normalizeCategoryLinks")
                && html.contains("data-atf-test-id")
                && html.contains("atfOpenTest(id"));
        checks.put("category selection projection", html.contains("function categoryItems()")
                && html.contains("function renderCategoryDetail(item)")
                && html.contains("function bindCategorySelection()")
                && html.contains("function ensureCategoryDetail()")
                && html.contains("function normalizeCategorySummaryLabels()")
                && html.contains("if(id==='category-view'){ensureCategoryFilters();ensureCategoryDetail();normalizeCategorySummaryLabels();normalizeCategoryLinks();}"));
        checks.put("category surface and link contract", html.contains(
                ".category-view,.category-view .test-list,.category-view .test-list-tools,")
                && html.contains(".category-view .test-content,.category-view .test-content-tools,")
                && html.contains("background:var(--atf-dark)!important;color:var(--atf-text)!important")
                && html.contains(".category-view a.linked{color:var(--atf-accent)!important")
                && html.contains(".category-view .test-list-wrapper>.test-list-item>.test-item"));
        checks.put("runtime navigation", html.contains("window.atfActivate=function")
                && (html.contains("function renderFails()") || html.contains("renderFails=function()"))
                && html.contains(":'0%']")
                && html.contains("v.classList.toggle('d-none',!active)")
                && html.contains("function synchronizeNavigationSelection(id)")
                && html.contains("aria-current")
                && html.contains("function bindNavigationSelectionLinks()"));
        checks.put("hash navigation", html.contains("function activateFromHash()")
                && html.contains("window.addEventListener('hashchange',activateFromHash)")
                && html.contains("activateFromHash();},0)"));
        checks.put("offline tab fallback", html.contains("href=\"#test-view\"")
                && html.contains("href=\"#category-view\"")
                && html.contains("href=\"#atf-fails-view\"")
                && html.contains(":has(#category-view:target)"));
    }
}
