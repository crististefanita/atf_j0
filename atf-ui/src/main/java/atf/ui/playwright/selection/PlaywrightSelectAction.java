package atf.ui.playwright.selection;

import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence.ObservationBudget;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import java.util.Objects;

/** Read-only native readiness precedes one selection; an uncertain native result is never replayed. */
public final class PlaywrightSelectAction {
    private static final String OPTION_READY = """
            (node, label) => !(node instanceof HTMLSelectElement) || label === null
              || Array.from(node.options).some(option => option.label === label)
            """;
    private final AtfLocator element;
    private final ObservationBudget budget;

    public PlaywrightSelectAction(AtfLocator element, ObservationBudget budget) {
        this.element = Objects.requireNonNull(element, "element");
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public void select(String label) {
        Locator target = element.locator();
        target.page().waitForCondition(() -> ready(target, label),
                new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
        // Native strictness and selection remain authoritative. No exception or empty result permits another call.
        var selected = target.selectOption(new SelectOption().setLabel(label),
                new Locator.SelectOptionOptions().setTimeout(budget.remainingMilliseconds()));
        if (selected.isEmpty()) throw new IllegalStateException(
                "Select option returned no confirmed selected value: " + element.selector());
        budget.remainingMilliseconds();
    }

    private boolean ready(Locator target, String label) {
        boolean option = Boolean.TRUE.equals(target.evaluate(OPTION_READY, label,
                new Locator.EvaluateOptions().setTimeout(budget.remainingMilliseconds())));
        return option && target.isVisible()
                && target.isEnabled(new Locator.IsEnabledOptions().setTimeout(budget.remainingMilliseconds()));
    }
}
