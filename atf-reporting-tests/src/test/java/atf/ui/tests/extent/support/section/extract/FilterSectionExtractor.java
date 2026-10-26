package atf.ui.tests.extent.support.section.extract;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Page;

import java.util.Objects;

/** Browser adapter for Categories/Fails navigation with a neutral filter snapshot. */
public final class FilterSectionExtractor {
    private final UiAuditFlow auditFlow;
    private final ApiReportStructureExtractor structureExtractor;
    private final ReportUiLocators locators;

    public FilterSectionExtractor(UiAuditFlow auditFlow,
                                  ApiReportStructureExtractor structureExtractor,
                                  ReportUiLocators locators) {
        this.auditFlow = Objects.requireNonNull(auditFlow, "auditFlow");
        this.structureExtractor = Objects.requireNonNull(structureExtractor, "structureExtractor");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Extraction extract() {
        Page category = auditFlow.openTab(ReportUiContract.Tab.CATEGORIES.hash());
        ReportAuditSnapshot.Annotations annotations = structureExtractor.extractAnnotations(category);
        boolean tagLinks = annotations.categoryCount() > 0 && annotations.linkedScenarioCount() > 0;
        if (tagLinks) {
            auditFlow.actions().click(locators.firstCategoryLinkAction(category),
                    "Tests tab opens for selected annotation");
            auditFlow.actions().waitForVisible(locators.testsView(category), ReportUiContract.Tab.TESTS.hash());
        }
        Page fails = auditFlow.openTab(ReportUiContract.Tab.FAILS.hash());
        ReportAuditSnapshot.Failures failures = structureExtractor.extractFailures(fails);
        boolean failureLinks = failures.failureCount() > 0
                && failures.primaryCauseCount() > 0 && failures.openLinkCount() > 0;
        return new Extraction(category, fails,
                new ReportAuditSnapshot.Filters(annotations, failures, tagLinks, failureLinks));
    }

    public boolean openFirstFailureInTests(Extraction extraction) {
        Objects.requireNonNull(extraction, "extraction");
        if (!extraction.snapshot().failureLinks()) return false;
        auditFlow.actions().click(locators.firstFailureOpenAction(extraction.failsPage()),
                "selected failure opens in Tests");
        return locators.testsView(extraction.failsPage()).isVisible();
    }

    public record Extraction(Page categoryPage, Page failsPage, ReportAuditSnapshot.Filters snapshot) {
        public Extraction {
            categoryPage = Objects.requireNonNull(categoryPage, "categoryPage");
            failsPage = Objects.requireNonNull(failsPage, "failsPage");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
        }
    }
}
