package atf.ui.tests.extent.support.inspection.projection;
import atf.reporting.ui.contract.ReportUiContract;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/** Inspects the same rendered-surface contract across every report projection. */
public final class ReportProjectionMatrixInspector {
    private final ReportUiLocators locators;

    public ReportProjectionMatrixInspector() {
        this(new ReportUiLocators());
    }

    public ReportProjectionMatrixInspector(ReportUiLocators locators) {
        this.locators = locators;
    }

    public Observation inspect(Page page, ReportUiContract.Tab tab) {
        Locator view = locators.tabView(page, tab);
        List<String> violations = new ArrayList<>();
        boolean visible = view.isVisible();
        if (!visible) violations.add("tab view is not visible: " + tab);

        String signature = computedSurfaceSignature(view);
        boolean styled = !signature.isBlank() && !signature.startsWith("#00000000|");
        if (!styled) violations.add("tab view has no computed ATF surface: " + tab);

        int renderedContentCount = switch (tab) {
            case TESTS -> locators.testOwners(page).count();
            case OVERVIEW -> locators.overviewTables(view).count();
            case CATEGORIES -> locators.categoryItems(page).count();
            case FAILS -> locators.failureItems(view).count();
        };
        boolean explicitEmptyState = tab == ReportUiContract.Tab.FAILS
                && locators.failureEmptyState(view).count() > 0;
        int contentCount = renderedContentCount > 0 || explicitEmptyState ? 1 : 0;
        if (contentCount == 0) violations.add("tab has no dynamic content: " + tab);

        int filterControlCount = switch (tab) {
            case TESTS -> locators.testStatusTrigger(page).count() + locators.testTagTrigger(page).count();
            case CATEGORIES -> locators.categoryStatusFilterTrigger(view).count()
                    + locators.categoryTagFilterTrigger(view).count();
            case OVERVIEW, FAILS -> 0;
        };
        boolean filterContract = switch (tab) {
            case TESTS, CATEGORIES -> filterControlCount >= 2;
            case OVERVIEW, FAILS -> true;
        };
        if (!filterContract) violations.add("applicable filter controls are incomplete: " + tab);

        return new Observation(tab, visible, styled, contentCount, renderedContentCount,
                explicitEmptyState, filterControlCount, filterContract, signature, violations);
    }

    private static String computedSurfaceSignature(Locator view) {
        Object value = view.evaluate("element => { const s = getComputedStyle(element); "
                + "return [s.backgroundColor, s.color, s.fontFamily].join('|'); }");
        return value == null ? "" : String.valueOf(value);
    }

    public record Observation(ReportUiContract.Tab tab, boolean visible, boolean styled,
                              int contentCount, int renderedContentCount, boolean explicitEmptyState,
                              int filterControlCount, boolean filterContract,
                              String surfaceSignature, List<String> violations) {
        public Observation {
            violations = List.copyOf(violations);
        }

        public boolean valid() {
            return visible && styled && contentCount > 0 && filterContract && violations.isEmpty();
        }
    }
}
