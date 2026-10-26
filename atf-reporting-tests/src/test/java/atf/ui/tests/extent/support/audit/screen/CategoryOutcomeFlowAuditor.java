package atf.ui.tests.extent.support.audit.screen;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Audits the Categories status projections used by warning and skipped API
 * flows. The browser mechanics stay here; the section module receives only
 * immutable observations and a renderer-neutral validation result.
 */
public final class CategoryOutcomeFlowAuditor {
    private static final List<CategoryFilter> STATUS_FILTERS = List.of(
            CategoryFilter.PASS, CategoryFilter.FAIL, CategoryFilter.WARNING, CategoryFilter.SKIP);

    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;

    public CategoryOutcomeFlowAuditor(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = Objects.requireNonNull(auditFlow, "auditFlow");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Observation inspect(Page page) {
        Objects.requireNonNull(page, "page");
        Locator categoryView = locators.categoryView(page);
        int baselineCount = locators.categoryItems(page).count();
        List<StatusSelection> selections = new ArrayList<>();
        for (CategoryFilter status : STATUS_FILTERS) {
            selections.add(selectStatus(page, categoryView, status));
        }
        StatusSelection clear = selectStatus(page, categoryView, CategoryFilter.CLEAR);
        boolean clearRestoresBaseline = clear.visibleCategoryCount() == baselineCount
                && clear.filterSelected()
                && clear.detailRemainsReadable();
        auditFlow.actions().observation("Categories status flow",
                ReportUiContract.Tab.CATEGORIES.hash() + " / Pass → Fail → Warning → Skip → Clear",
                "all status projections remain selectable and Clear restores all categories",
                selections + "; clear=" + clear + "; baseline=" + baselineCount);
        return new Observation(baselineCount, selections, clear, clearRestoresBaseline);
    }

    private StatusSelection selectStatus(Page page, Locator categoryView, CategoryFilter status) {
        AtfLocator option = locators.categoryStatusOptionAction(page, status.value());
        if (option.locator().count() == 0) {
            return StatusSelection.unsupported(status);
        }
        AtfLocator trigger = locators.categoryStatusFilterTriggerAction(page);
        if (!option.locator().isVisible()) {
            auditFlow.actions().click(trigger, "Categories status filter opens for " + status.value());
            auditFlow.actions().waitForVisible(option);
        }
        auditFlow.actions().click(option, "Categories status filter selects " + status.value());
        Locator visible = locators.categoryVisibleItems(categoryView);
        List<String> names = new ArrayList<>();
        int rowEvidence = 0;
        for (int index = 0; index < visible.count(); index++) {
            Locator item = visible.nth(index);
            names.add(locators.categoryItemName(item).innerText().trim());
            rowEvidence += locators.categoryStatusRows(item).count();
        }
        Locator detail = locators.visibleCategoryDetail(page);
        boolean readable = visible.count() == 0 || (detail.count() > 0 && detail.isVisible()
                && locators.categoryTitle(detail).count() > 0
                && locators.categoryLinkedTests(detail).count() == locators.categoryStatusRows(detail).count());
        boolean selected = "true".equals(option.locator().getAttribute("aria-pressed"));
        return new StatusSelection(status, true, visible.count(), rowEvidence, selected,
                readable, List.copyOf(names));
    }

    public ValidationResult validate(Observation observation) {
        return validateObservation(observation);
    }

    /**
     * Renderer-neutral validation for the complete Categories status matrix.
     * Keeping this pure makes the combinations testable without launching a
     * browser while inspect remains the single DOM adapter.
     */
    public static ValidationResult validateObservation(Observation observation) {
        Objects.requireNonNull(observation, "observation");
        List<AtfAssertion> assertions = new ArrayList<>();
        for (StatusSelection selection : observation.selections()) {
            String label = selection.filter().label();
            assertions.add(ReportAuditChecks.check(
                    "Categories " + label + " filter",
                    "supported, selected and renders category and row evidence",
                    selection.toString(),
                    selection.supported() && selection.filterSelected()
                            && selection.visibleCategoryCount() > 0
                            && selection.visibleRowEvidence() > 0
                            && selection.detailRemainsReadable()));
        }
        assertions.add(ReportAuditChecks.check(
                "Categories Clear after status exploration",
                "all categories are visible again",
                observation.clear().toString(), observation.clearRestoresBaseline()));
        return ValidationResult.of(assertions);
    }

    public record Observation(int baselineCategoryCount, List<StatusSelection> selections,
                              StatusSelection clear, boolean clearRestoresBaseline) {
        public Observation {
            if (baselineCategoryCount < 0) throw new IllegalArgumentException("baselineCategoryCount must not be negative");
            selections = List.copyOf(Objects.requireNonNull(selections, "selections"));
            clear = Objects.requireNonNull(clear, "clear");
        }
    }

    public record StatusSelection(CategoryFilter filter, boolean supported, int visibleCategoryCount,
                                  int visibleRowEvidence, boolean filterSelected,
                                  boolean detailRemainsReadable, List<String> visibleCategoryNames) {
        public StatusSelection {
            filter = Objects.requireNonNull(filter, "filter");
            if (visibleCategoryCount < 0 || visibleRowEvidence < 0) {
                throw new IllegalArgumentException("category status counts must be non-negative");
            }
            visibleCategoryNames = List.copyOf(Objects.requireNonNull(visibleCategoryNames,
                    "visibleCategoryNames"));
        }

        static StatusSelection unsupported(CategoryFilter filter) {
            return new StatusSelection(filter, false, 0, 0, false, false, List.of());
        }
    }

    public enum CategoryFilter {
        PASS(AtfReportStatus.PASS),
        FAIL(AtfReportStatus.FAIL),
        WARNING(AtfReportStatus.WARNING),
        SKIP(AtfReportStatus.SKIP),
        CLEAR(null, "clear", "Clear");

        private final AtfReportStatus status;
        private final String value;
        private final String label;

        CategoryFilter(AtfReportStatus status) {
            this(status, status.displayValue(), status.displayLabel());
        }

        CategoryFilter(AtfReportStatus status, String value, String label) {
            this.status = status;
            this.value = value;
            this.label = label;
        }

        public AtfReportStatus status() {
            return status;
        }

        public String value() {
            return value;
        }

        public String label() {
            return label;
        }

        public static CategoryFilter fromStatus(AtfReportStatus status) {
            return switch (Objects.requireNonNull(status, "status")) {
                case PASS -> PASS;
                case FAIL -> FAIL;
                case WARNING -> WARNING;
                case SKIP -> SKIP;
                case INFO -> throw new IllegalArgumentException(
                        "INFO is not a selectable Categories status filter");
            };
        }
    }
}
