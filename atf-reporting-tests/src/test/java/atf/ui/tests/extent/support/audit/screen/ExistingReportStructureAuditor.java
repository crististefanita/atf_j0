package atf.ui.tests.extent.support.audit.screen;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.ui.tests.extent.support.section.extract.OverviewSectionExtractor;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Files;
import java.nio.file.Path;

/** Read-only structural auditor for an existing Extent report. */
public final class ExistingReportStructureAuditor {
    private final UiAuditFlow auditFlow;
    private final Path inputReport;
    private final ReportUiLocators locators;
    private final OverviewSectionExtractor overviewExtractor;

    public ExistingReportStructureAuditor(UiAuditFlow auditFlow, Path inputReport) {
        this(auditFlow, inputReport, new ReportUiLocators());
    }

    public ExistingReportStructureAuditor(UiAuditFlow auditFlow, Path inputReport,
                                          ReportUiLocators locators) {
        this.auditFlow = auditFlow;
        this.inputReport = inputReport;
        this.locators = locators;
        this.overviewExtractor = new OverviewSectionExtractor(locators);
    }

    public TestsSnapshot discoverTests(Page page) {
        Locator tests = locators.testsView(page);
        Locator owners = locators.testOwners(tests);
        int scenarios = 0;
        int phases = 0;
        int validations = 0;
        int disclosures = 0;
        for (int index = 0; index < owners.count(); index++) {
            auditFlow.actions().click(locators.testOwnerItemAction(page, index), "class detail opens");
            Locator detail = locators.detailForOwner(locators.testOwnerItem(page, index));
            if (detail.count() == 0) detail = locators.visibleTestDetail(page);
            auditFlow.actions().waitForVisible(
                    new AtfLocator(detail, ReportUiContract.VISIBLE_DETAIL_SELECTOR));
            scenarios += locators.scenarios(detail).count();
            phases += locators.phases(detail).count();
            validations += locators.validationTables(detail).count();
            disclosures += locators.evidenceDisclosures(detail).count()
                    + locators.nodeHeaders(detail).count();
        }
        return new TestsSnapshot(owners.count(), scenarios, phases, validations, disclosures);
    }

    public ExpansionSnapshot expandEveryOwnerScenarioAndPhase(Page page) {
        Locator owners = locators.testOwners(page);
        int ownerCount = owners.count();
        int scenarioCount = 0;
        int phaseCount = 0;
        int expandedScenarioCount = 0;
        int expandedPhaseCount = 0;
        for (int ownerIndex = 0; ownerIndex < ownerCount; ownerIndex++) {
            auditFlow.actions().click(locators.testOwnerItemAction(page, ownerIndex), "class detail opens");
            Locator detail = locators.visibleTestDetail(page);
            auditFlow.actions().waitForVisible(
                    new AtfLocator(detail, ReportUiContract.VISIBLE_DETAIL_SELECTOR));
            Locator scenarios = locators.scenarios(detail);
            scenarioCount += scenarios.count();
            for (int scenarioIndex = 0; scenarioIndex < scenarios.count(); scenarioIndex++) {
                Locator scenario = scenarios.nth(scenarioIndex);
                expandScenario(scenario);
                if ("true".equals(scenario.getAttribute("data-atf-scenario-open"))) expandedScenarioCount++;
                Locator phases = locators.phases(scenario);
                phaseCount += phases.count();
                for (int phaseIndex = 0; phaseIndex < phases.count(); phaseIndex++) {
                    Locator phase = phases.nth(phaseIndex);
                    expandPhase(phase);
                    if ("true".equals(phase.getAttribute("data-atf-phase-open"))) expandedPhaseCount++;
                }
            }
        }
        return new ExpansionSnapshot(ownerCount, scenarioCount, phaseCount,
                expandedScenarioCount, expandedPhaseCount);
    }

    public ReportAuditSnapshot.Overview inspectOverviewSnapshot(Page page, boolean includeHierarchy) {
        return overviewExtractor.inspect(page, includeHierarchy);
    }

    public boolean hasResponsiveActionTableCss() {
        try {
            String html = Files.readString(inputReport);
            return html.contains(".atf-ui-action-table{width:100%!important;min-width:0!important;max-width:100%!important;")
                    && html.contains("table-layout:fixed!important")
                    && html.contains("white-space:pre-wrap!important")
                    && html.contains(".atf-ui-action-table-wrap{width:100%!important;max-width:100%!important;min-width:0!important;")
                    && html.contains(".atf-ui-action-identity{display:table-cell!important;vertical-align:top!important;")
                    && html.contains(".atf-ui-action-screenshot{display:flex!important;")
                    && html.contains("margin:12px 0 8px!important")
                    && html.contains(".test-view .test-content-detail .card-body>.table{width:100%!important;max-width:100%!important;min-width:0!important;table-layout:fixed!important}")
                    && html.contains(".atf-ui-action-sequence{width:calc(100vw - 30px)!important;max-width:calc(100vw - 30px)!important;min-width:0!important;")
                    && !html.contains("min-width:720px!important")
                    && !html.contains("min-width:452px!important")
                    && !html.contains("min-width:428px!important")
                    && !html.contains("width:132px!important;min-width:132px!important");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void expandScenario(Locator scenario) {
        if ("true".equals(scenario.getAttribute("data-atf-scenario-open"))) return;
        auditFlow.actions().click(locators.scenarioNodeAction(scenario), "scenario is expanded");
    }

    private void expandPhase(Locator phase) {
        if ("true".equals(phase.getAttribute("data-atf-phase-open"))) return;
        auditFlow.actions().click(locators.phaseNodeAction(phase), "phase is expanded");
    }

    public record TestsSnapshot(int ownerCount, int scenarioCount, int phaseCount,
                                int validationCount, int disclosureCount) { }

    public record ExpansionSnapshot(int ownerCount, int scenarioCount, int phaseCount,
                                    int expandedScenarioCount, int expandedPhaseCount) { }
}
