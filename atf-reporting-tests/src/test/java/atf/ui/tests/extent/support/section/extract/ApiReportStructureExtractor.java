package atf.ui.tests.extent.support.section.extract;

import atf.ui.tests.extent.support.uireport.browser.UiComputedStyleColorEvidence;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.reporting.audit.ReportAuditSnapshot;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Browser adapter for Tests, Categories and Fails facts; it never emits assertions. */
public final class ApiReportStructureExtractor {
    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;
    private final OverviewSectionExtractor overviewExtractor;

    public ApiReportStructureExtractor(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = Objects.requireNonNull(auditFlow, "auditFlow");
        this.locators = Objects.requireNonNull(locators, "locators");
        this.overviewExtractor = new OverviewSectionExtractor(locators);
    }

    public ReportAuditSnapshot.Tests extractTests(Page page) {
        Locator tests = locators.testsView(page);
        Locator owners = locators.testOwners(tests);
        List<String> names = new ArrayList<>();
        List<String> scenarioTitles = new ArrayList<>();
        List<AtfReportStatus> scenarioStatuses = new ArrayList<>();
        int scenarios = 0;
        int phases = 0;
        int requests = 0;
        int validations = 0;
        for (int index = 0; index < owners.count(); index++) {
            Locator owner = locators.testOwnerItem(page, index);
            names.add(auditFlow.actions().getText(locators.testOwnerName(owner),
                    ReportUiContract.TEST_OWNER_SELECTOR + " .name", "class owner name is readable"));
            auditFlow.actions().click(locators.testOwnerItemAction(page, index), "class detail opens");
            Locator detail = locators.visibleTestDetail(page);
            auditFlow.actions().waitForVisible(detail, ReportUiContract.VISIBLE_DETAIL_SELECTOR);
            Locator scenarioCards = locators.scenarios(detail);
            scenarios += scenarioCards.count();
            for (int scenarioIndex = 0; scenarioIndex < scenarioCards.count(); scenarioIndex++) {
                Locator scenario = scenarioCards.nth(scenarioIndex);
                scenarioTitles.add(locators.nodeTitle(scenario));
                Locator status = locators.nodeStatusBadge(scenario);
                scenarioStatuses.add(AtfReportStatus.fromDisplayName(
                        status.count() == 0 ? "" : status.innerText()));
            }
            phases += locators.phases(detail).count();
            requests += locators.requests(detail).count();
            validations += locators.validationTables(detail).count();
        }
        return new ReportAuditSnapshot.Tests(names.size(), scenarios, phases, requests,
                validations, names, scenarioTitles, scenarioStatuses);
    }

    public int ownerCount(Page page) { return locators.testOwners(page).count(); }

    public ReportAuditSnapshot.Annotations extractAnnotations(Page page) {
        Locator view = locators.categoryView(page);
        Locator items = locators.categoryItems(page);
        if (items.count() == 0) return new ReportAuditSnapshot.Annotations(0, 0, "missing", false, List.of());
        List<Integer> cardinalities = new ArrayList<>();
        for (int index = 0; index < items.count(); index++) {
            auditFlow.actions().click(locators.categoryItemAction(page, index), "annotation filter opens");
            auditFlow.actions().waitForVisible(locators.visibleCategoryDetailAnchor(page));
            cardinalities.add(locators.categoryLinkedTests(locators.categoryContentDetail(page)).count());
        }
        auditFlow.actions().getText(locators.categoryList(view), ReportUiContract.TEST_OWNER_SELECTOR,
                "annotation filter is readable");
        String surface = UiComputedStyleColorEvidence.surface(locators.categoryList(view));
        boolean styled = !surface.equalsIgnoreCase("#ffffff / #ffffff")
                && locators.categoryContent(view).isVisible();
        return new ReportAuditSnapshot.Annotations(items.count(), cardinalities.stream()
                .mapToInt(Integer::intValue).sum(), surface, styled, cardinalities);
    }

    public ReportAuditSnapshot.Failures extractFailures(Page page) {
        Locator view = locators.failsView(page);
        int failureCount = locators.failureNames(view).count();
        int primaryCauseCount = 0;
        var scenarios = new java.util.HashSet<List<String>>();
        Locator rows = locators.failureItems(view);
        for (int index = 0; index < rows.count(); index++) {
            String owner = rows.nth(index).getAttribute("data-atf-test-id");
            String scenario = rows.nth(index).getAttribute("data-atf-scenario-index");
            if (owner == null || owner.isBlank() || scenario == null || scenario.isBlank())
                throw new IllegalStateException("Fails row has no typed owner/scenario identity: " + index);
            scenarios.add(List.of(owner, scenario));
            auditFlow.actions().click(locators.failureItemAction(page, index),
                    "inspect primary cause for Fails row " + (index + 1));
            primaryCauseCount += locators.primaryFailureDetails(view).count();
        }
        return new ReportAuditSnapshot.Failures(failureCount,
                primaryCauseCount,
                locators.failureOpenLinks(view).count(),
                failureCount == 0 && locators.failureEmptyState(view).count() == 1, scenarios.size());
    }

    public ReportAuditSnapshot.Overview extractOverview(Page page, boolean includeHierarchy) {
        return overviewExtractor.inspect(page, includeHierarchy);
    }
}
