package atf.ui.tests.extent.support.navigation;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.LinkedHashSet;
import java.util.Set;

/** End-to-end semantic flow for Tests filters, Categories and Overview. */
public final class ReportNavigationFlow {
    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;

    public ReportNavigationFlow(UiAuditFlow auditFlow) {
        this(auditFlow, new ReportUiLocators());
    }

    public ReportNavigationFlow(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = auditFlow;
        this.locators = locators;
    }

    public NavigationSnapshot traverse() {
        Page tests = auditFlow.openTab(ReportUiContract.Tab.TESTS.hash());
        // Capture the report-wide baseline before selecting an owner. The
        // owner click is an exploration step and must not redefine what
        // Clear and Overview mean for the complete input artifact.
        ReportAuditSnapshot.Tests baseline = inspectTests(tests);

        AtfLocator statusToggle = locators.testStatusTriggerAction(tests);
        auditFlow.actions().click(statusToggle, "status filter opens");
        AtfLocator passFilter = locators.statusFilterAction(
                tests, AtfReportStatus.PASS.displayValue());
        auditFlow.actions().waitForVisible(passFilter);
        auditFlow.actions().click(passFilter, "Pass filter selects");
        int passScenarioCount = visibleScenarioCount(tests);
        boolean passSelected = "true".equals(passFilter.locator().getAttribute("aria-pressed"));
        auditFlow.actions().observation("Status filter result", passFilter.selector(),
                "visible scenarios are reconciled", passScenarioCount + " visible scenarios");

        AtfLocator tagToggle = locators.testTagTriggerAction(tests);
        Locator tagFilters = locators.tagFilters(tests);
        int tagCount = tagFilters.count();
        int tagSelectionSteps = 0;
        int selectedTagIndex = -1;
        String firstTagName = "";
        String secondTagName = "";
        int firstTagScenarioCount = 0;
        boolean firstTagReplaced = tagCount <= 1;
        if (tagCount > 0) {
            ensureMenuOpen(tagToggle, locators.testTagMenu(tests),
                    locators.tagFilterAction(tests, 0));
            AtfLocator firstTag = locators.tagFilterAction(tests, 0);
            firstTagName = firstTag.locator().innerText().trim();
            auditFlow.actions().click(firstTag, "first dynamic tag selects");
            waitForTagState(tests, tagToggle, firstTag, "true",
                    "first selected tag remains active");
            tagSelectionSteps++;
            selectedTagIndex = 0;
            firstTagScenarioCount = visibleScenarioCount(tests);
            auditFlow.actions().observation("First tag filter result", firstTag.selector(),
                    "selected tag remains active while Pass stays combined",
                    "tag='" + firstTagName + "', active="
                            + activeFilterCount(locators.tagFilters(tests))
                            + ", visible scenarios=" + firstTagScenarioCount);
        }
        if (tagCount > 1) {
            ensureMenuOpen(tagToggle, locators.testTagMenu(tests),
                    locators.tagFilterAction(tests, 1));
            AtfLocator secondTag = locators.tagFilterAction(tests, 1);
            secondTagName = secondTag.locator().innerText().trim();
            auditFlow.actions().click(secondTag, "second dynamic tag replaces selection");
            waitForTagState(tests, tagToggle, secondTag, "true",
                    "second selected tag replaces the first");
            tagSelectionSteps++;
            selectedTagIndex = 1;
            firstTagReplaced = !"true".equals(firstTagAttribute(tests, 0))
                    && "true".equals(secondTag.locator().getAttribute("aria-pressed"));
        }
        int combinedScenarioCount = visibleScenarioCount(tests);
        int activeTagCount = activeFilterCount(locators.tagFilters(tests));
        boolean tagCombinationValid = tagCount == 0
                ? activeTagCount == 0
                : activeTagCount == 1 && firstTagReplaced;
        auditFlow.actions().observation("Combined tag filter result", ReportUiContract.TAG_FILTER_MENU,
                "the second tag replaces the first while Pass stays combined",
                "tag='" + firstTagName + "' → '" + secondTagName + "', active="
                        + activeTagCount + " after " + tagSelectionSteps
                        + " selections / " + tagCount + " available, visible scenarios="
                        + combinedScenarioCount);

        clearStatusAndTags(tests, selectedTagIndex);
        int clearedScenarioCount = scenarioIdCount(tests);
        int clearedOwnerCount = locators.testOwners(tests).count();
        int activeTagCountAfterClear = activeFilterCount(locators.tagFilters(tests));
        boolean statusClearSelected = "true".equals(
                locators.clearStatusFilterAction(tests).locator().getAttribute("aria-pressed"));
        boolean clearRestoresBaseline = clearedOwnerCount == baseline.classCount()
                && activeTagCountAfterClear == 0 && statusClearSelected;
        auditFlow.actions().observation("Clear filter result", locators.clearStatusFilterAction(tests).selector(),
                "status=Clear, tags=0, owner list returns to the unfiltered baseline",
                "owners=" + clearedOwnerCount + ", tags=" + activeTagCountAfterClear
                        + ", selected-owner scenarios=" + clearedScenarioCount);

        Page categories = auditFlow.openTabContinuing(ReportUiContract.Tab.CATEGORIES.hash());
        CategorySnapshot category = inspectFirstCategory(categories);
        Page overview = auditFlow.openTabContinuing(ReportUiContract.Tab.OVERVIEW.hash());
        ReportAuditSnapshot.Overview overviewSnapshot =
                new atf.ui.tests.extent.support.section.extract.OverviewSectionExtractor(locators)
                        .inspect(overview, true);
        return new NavigationSnapshot(overview, baseline, passSelected, passScenarioCount,
                combinedScenarioCount, activeTagCount, tagSelectionSteps, tagCombinationValid,
                firstTagName, secondTagName, firstTagScenarioCount, firstTagReplaced,
                clearedScenarioCount, clearedOwnerCount, activeTagCountAfterClear,
                statusClearSelected, clearRestoresBaseline, category, overviewSnapshot);
    }

    private void clearStatusAndTags(Page page, int selectedTagIndex) {
        auditFlow.actions().pressKey(page, "Escape", ReportUiContract.BODY_SELECTOR,
                "all transient filter menus are closed before Clear");
        AtfLocator statusToggle = locators.testStatusTriggerAction(page);
        AtfLocator clear = locators.clearStatusFilterAction(page);
        ensureMenuOpen(statusToggle, locators.testStatusMenu(page), clear);
        auditFlow.actions().click(clear, "status filter clears");

        AtfLocator tagToggle = locators.testTagTriggerAction(page);
        Locator tagMenu = locators.testTagMenu(page);
        Locator tagFilters = locators.tagFilters(page);
        if (selectedTagIndex >= 0 && selectedTagIndex < tagFilters.count()) {
            AtfLocator selectedTag = locators.tagFilterAction(page, selectedTagIndex);
            ensureMenuOpen(tagToggle, tagMenu, selectedTag);
            auditFlow.actions().click(selectedTag, "selected tag filter clears");
            waitForTagState(page, tagToggle, selectedTag, "false", "selected tag is cleared");
        }
    }

    private void waitForTagState(Page page, AtfLocator toggle, AtfLocator option,
                                 String value, String expected) {
        if (!locators.testTagMenu(page).isVisible()) {
            auditFlow.actions().click(toggle, "tag menu opens for state verification");
        }
        auditFlow.actions().waitForAttribute(page, option, "aria-pressed", value, expected);
    }

    private String firstTagAttribute(Page page, int index) {
        Locator filters = locators.tagFilters(page);
        return index < filters.count() ? filters.nth(index).getAttribute("aria-pressed") : null;
    }

    private void ensureMenuOpen(AtfLocator toggle, Locator menu, AtfLocator requiredItem) {
        if (!menu.isVisible()) auditFlow.actions().click(toggle, "filter menu opens");
        if (requiredItem != null && requiredItem.locator().count() > 0) {
            auditFlow.actions().waitForVisible(requiredItem);
        }
    }

    public ReportAuditSnapshot.Tests inspectTests(Page page) {
        Locator owners = locators.testOwners(page);
        int ownerCount = owners.count();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int index = 0; index < owners.count(); index++) {
            names.add(locators.testOwnerName(locators.testOwnerItem(page, index)).innerText().trim());
        }
        Set<String> reportScenarioIds = new LinkedHashSet<>();
        for (int index = 0; index < ownerCount; index++) {
            String ownerName = names.get(index);
            auditFlow.actions().click(locators.testOwnerItemAction(page, index),
                    "owner '" + ownerName + "' opens for baseline discovery");
            auditFlow.actions().waitForVisible(locators.testContentDetail(page),
                    ReportUiContract.VISIBLE_DETAIL_SELECTOR);
            reportScenarioIds.addAll(scenarioIds(page));
        }
        if (ownerCount > 0) {
            auditFlow.actions().click(locators.firstTestOwnerAction(page),
                    "first test owner opens");
            auditFlow.actions().waitForVisible(locators.testContentDetail(page),
                    ReportUiContract.VISIBLE_DETAIL_SELECTOR);
        }
        int reportScenarioCount = reportScenarioIds.size();
        return new ReportAuditSnapshot.Tests(ownerCount, reportScenarioCount,
                0, 0, 0, names);
    }

    public CategorySnapshot inspectFirstCategory(Page page) {
        Locator items = locators.categoryItems(page);
        if (items.count() == 0) return new CategorySnapshot(false, 0, 0, 0);
        Locator first = locators.categoryItem(items, 0);
        auditFlow.actions().click(locators.firstCategoryItemAction(page),
                "first category filter selects");
        AtfLocator detailAnchor = locators.visibleCategoryDetailAnchor(page);
        auditFlow.actions().waitForVisible(detailAnchor);
        Locator detail = locators.visibleCategoryDetail(page);
        int links = locators.categoryLinkedTests(detail).count();
        int rows = locators.categoryStatusRows(detail).count();
        int declared = ReportAuditChecks.count(locators.categoryItemDuration(first).textContent());
        String className = first.getAttribute("class");
        boolean valid = className != null && className.contains("active")
                && declared == links && declared == rows;
        return new CategorySnapshot(valid, declared, links, rows);
    }

    private int visibleScenarioCount(Page page) {
        Locator scenarios = locators.scenarioCards(page);
        int visible = 0;
        for (int index = 0; index < scenarios.count(); index++) {
            if (scenarios.nth(index).isVisible()) visible++;
        }
        return visible;
    }

    private int scenarioIdCount(Page page) {
        return scenarioIds(page).size();
    }

    private Set<String> scenarioIds(Page page) {
        Locator scenarios = locators.scenarioCards(page);
        Set<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < scenarios.count(); index++) {
            String id = scenarios.nth(index).getAttribute("data-atf-scenario-id");
            if (id != null && !id.isBlank()) ids.add(id);
        }
        return ids;
    }

    private int activeFilterCount(Locator filters) {
        int active = 0;
        for (int index = 0; index < filters.count(); index++) {
            if ("true".equals(filters.nth(index).getAttribute("aria-pressed"))) active++;
        }
        return active;
    }

    public record CategorySnapshot(boolean valid, int declaredCount, int linkCount, int rowCount) { }

    public record NavigationSnapshot(Page overviewPage, ReportAuditSnapshot.Tests baseline,
                                     boolean passSelected, int passScenarioCount, int combinedScenarioCount,
                                     int activeTagCount, int tagSelectionSteps, boolean tagCombinationValid,
                                     String firstTagName, String secondTagName, int firstTagScenarioCount,
                                     boolean firstTagReplaced,
                                     int clearedScenarioCount, int clearedOwnerCount,
                                     int activeTagCountAfterClear, boolean statusClearSelected,
                                     boolean clearRestoresBaseline,
                                     CategorySnapshot category, ReportAuditSnapshot.Overview overview) { }
}
