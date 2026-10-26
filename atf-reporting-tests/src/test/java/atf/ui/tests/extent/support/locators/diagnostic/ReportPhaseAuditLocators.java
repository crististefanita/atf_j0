package atf.ui.tests.extent.support.locators.diagnostic;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** DOM boundary for the phase/scenario diagnostic contract in both renderers. */
public final class ReportPhaseAuditLocators {
    private final ReportDisclosureLocators common = new ReportDisclosureLocators();

    public Locator firstOwner(Page page) { return common.firstOwner(page); }
    public Locator header(Locator node) { return common.header(node); }
    public Locator scenarios(Page page, boolean fallback) {
        return fallback ? page.locator("article[class~='scenario']") : common.scenarios(page);
    }
    public Locator phases(Locator scenario) { return scenario.locator("[data-atf-node-role='phase']"); }
    public Locator phaseLogTables(Locator phase) { return phaseLogBodies(phase).locator("table[class~='table']"); }
    public Locator phaseLogBodies(Locator phase) { return scenarioLogBodies(phase); }
    public Locator scenarioLogBodies(Locator scenario) {
        return scenario.locator(":scope > div:not([class~='card-header']):not([class~='accordion'])"
                + ":not([data-atf-node-role])");
    }
    public Locator auditBand(Locator scope) { return common.evidence(scope, CssClass.PHASE_DETAILS); }
    public Locator diagnostics(Locator scope) { return common.evidence(scope, CssClass.PHASE_DIAGNOSTICS); }
    public Locator diagnosticSummary(Locator disclosure) { return common.summary(disclosure); }
    public Locator requests(Locator phase, boolean fallback) {
        return fallback ? phase.locator(":scope > [data-atf-node-role='request']") : common.requests(phase);
    }
    public Locator evidence(Locator scope, CssClass kind) { return common.evidence(scope, kind); }
    public Locator testsTab(Page page, boolean fallback) {
        return fallback ? common.fallbackTestsTab(page) : page.locator("[id='nav-tests']");
    }
    public Locator failsTab(Page page, boolean fallback) {
        return page.locator(fallback ? "button[data-tab='fails']" : "[id='nav-fails']");
    }
    public Locator fails(Page page, boolean fallback) {
        return page.locator(fallback ? "[id='fails']" : "[id='atf-fails-view']");
    }
    public Locator body(Page page) { return page.locator("body"); }
}
