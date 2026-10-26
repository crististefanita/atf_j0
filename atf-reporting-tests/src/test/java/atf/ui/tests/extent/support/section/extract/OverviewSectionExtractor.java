package atf.ui.tests.extent.support.section.extract;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.ui.contract.ReportUiContract;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Browser-only Overview adapter; it produces facts and never asserts. */
public final class OverviewSectionExtractor {
    private final ReportUiLocators locators;

    public OverviewSectionExtractor(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ReportAuditSnapshot.Overview inspect(Page page, boolean includeHierarchy) {
        Objects.requireNonNull(page, "page");
        Locator overview = locators.overviewView(page);
        List<String> selectors = ReportUiContract.overviewSelectors(includeHierarchy);
        int sectionCount = 0;
        for (String selector : selectors) {
            if (locators.overviewSection(overview, selector).count() > 0) sectionCount++;
        }
        Locator metrics = locators.overviewMetrics(overview);
        int classes = metric(metrics, "Test classes in this run");
        int scenarios = metric(metrics, "Scenarios in this run");
        Locator rows = locators.overviewAuditRows(overview);
        int auditTotal = 0;
        for (int index = 0; index < rows.count(); index++) {
            Locator cells = locators.tableCells(rows.nth(index));
            if (cells.count() >= 2) auditTotal += ReportAuditChecks.count(cells.nth(1).textContent());
        }
        List<String> violations = new ArrayList<>();
        if (sectionCount != selectors.size()) violations.add("Overview characteristic sections are incomplete");
        if (classes < 0 || scenarios < 0) violations.add("Overview execution metrics are missing");
        if (rows.count() == 0) violations.add("Overview scenario audit table is missing");
        return new ReportAuditSnapshot.Overview(sectionCount, selectors.size(),
                Math.max(0, classes), Math.max(0, scenarios), auditTotal, rows.count(),
                violations.isEmpty(), violations);
    }

    private int metric(Locator metrics, String label) {
        for (int index = 0; index < metrics.count(); index++) {
            Locator metric = metrics.nth(index);
            String actual = locators.metricLabel(metric).textContent();
            if (label.equals(actual == null ? "" : actual.trim())) {
                String value = locators.metricValue(metric).textContent();
                return ReportAuditChecks.parseCount(value).orElse(-1);
            }
        }
        return -1;
    }
}
