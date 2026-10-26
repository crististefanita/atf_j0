package atf.ui.tests.extent.support.audit.screen;
import atf.reporting.audit.ValidationResult;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Renderer-neutral checks for the visible report surface and important cards. */
public final class ReportStyleValidator {
    private final ReportUiLocators locators;

    public ReportStyleValidator(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Result inspect(Page page) {
        List<String> violations = new ArrayList<>();
        Locator tests = locators.testsView(page);
        Locator overview = locators.overviewView(page);
        Locator categories = locators.categoryView(page);
        Locator fails = locators.failsView(page);
        if (tests.isVisible()) {
            checkVisibleAndSized(tests, "Tests tab", violations);
            Locator owners = locators.testOwners(tests);
            if (owners.count() == 0) {
                violations.add("Tests tab has no owner cards");
            } else {
                checkCardSurface(owners.first(), "Tests owner card", violations);
                Locator owner = locators.testOwnerItem(page, 0);
                if (owner.count() > 0 && owner.isVisible()) {
                    Locator detail = locators.detailForOwner(owner);
                    if (detail.count() > 0) {
                        checkVisibleAndSized(detail, "Tests detail", violations);
                        Locator scenario = locators.scenarios(detail).first();
                        if (scenario.count() > 0) checkCardSurface(scenario, "Scenario card", violations);
                    }
                }
            }
        }
        if (overview.isVisible()) {
            checkVisibleAndSized(overview, "Overview tab", violations);
            Locator metrics = locators.overviewMetrics(overview);
            if (metrics.count() > 0) checkCardSurface(metrics.first(), "Overview metric", violations);
        }
        if (categories.isVisible()) {
            checkVisibleAndSized(categories, "Categories tab", violations);
            Locator list = locators.categoryList(page);
            if (list.count() == 0) violations.add("Categories tab has no category list");
        }
        if (fails.isVisible()) {
            checkVisibleAndSized(fails, "Fails tab", violations);
            checkFailsLayout(fails, violations);
        }
        if (!tests.isVisible() && !overview.isVisible()
                && !categories.isVisible() && !fails.isVisible()) {
            violations.add("no report tab is visible");
        }
        return new Result(violations);
    }

    private static void checkVisibleAndSized(Locator locator, String name,
                                              List<String> violations) {
        if (locator.count() == 0 || !locator.isVisible()) {
            violations.add(name + " is not visible");
            return;
        }
        var box = locator.boundingBox();
        if (box == null || box.width <= 0 || box.height <= 0) {
            violations.add(name + " has no rendered dimensions");
        }
    }

    private void checkFailsLayout(Locator fails, List<String> violations) {
        Locator list = locators.failureListSurface(fails);
        if (list.count() == 0) {
            violations.add("Fails tab has no failure list");
            return;
        }
        int failureCount = locators.failureItems(fails).count();
        if (failureCount == 0) {
            checkVisibleAndSized(list, "Fails failure list", violations);
            return;
        }

        Locator detail = locators.failureDetailSurface(fails);
        checkVisibleAndSized(list, "Fails failure list", violations);
        checkVisibleAndSized(detail, "Fails detail pane", violations);
        if (detail.count() == 0 || !detail.isVisible() || !list.isVisible()) return;

        String flexDirection = fails.evaluate("element => getComputedStyle(element).flexDirection").toString();
        String flexWrap = fails.evaluate("element => getComputedStyle(element).flexWrap").toString();
        if (!"row".equals(flexDirection)) {
            violations.add("Fails layout must use flex-direction: row but was " + flexDirection);
        }
        if (!"nowrap".equals(flexWrap)) {
            violations.add("Fails layout must use flex-wrap: nowrap but was " + flexWrap);
        }

        var listBox = list.boundingBox();
        var detailBox = detail.boundingBox();
        if (listBox == null || detailBox == null) return;
        if (detailBox.x <= listBox.x + Math.max(1d, listBox.width - 1d)) {
            violations.add("Fails detail pane must render to the right of the failure list");
        }
        if (Math.abs(detailBox.y - listBox.y) > MAX_FAILS_TOP_DRIFT_PX) {
            violations.add("Fails detail pane must start on the same row as the failure list: listY="
                    + format(listBox.y) + ", detailY=" + format(detailBox.y));
        }
    }

    private static final double MAX_FAILS_TOP_DRIFT_PX = 8d;

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void checkCardSurface(Locator locator, String name,
                                         List<String> violations) {
        checkVisibleAndSized(locator, name, violations);
        if (locator.count() == 0 || !locator.isVisible()) return;
        String display = locator.evaluate("element => getComputedStyle(element).display").toString();
        String visibility = locator.evaluate("element => getComputedStyle(element).visibility").toString();
        if ("none".equals(display)) violations.add(name + " has display:none");
        if (!"visible".equals(visibility)) violations.add(name + " is not visibility:visible");
    }

    public record Result(List<String> violations) {
        public Result {
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean valid() { return violations.isEmpty(); }

        public String summary() {
            return valid() ? "style surface is visible and sized"
                    : String.join("; ", violations);
        }

        public ValidationResult validation() {
            return ValidationResult.fromViolations("Report style", violations);
        }
    }
}
