package atf.autoex.runtime.account;

import atf.autoex.locator.AutoExSelectors;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.Page;
import java.util.Objects;

/** Read-only readiness of the visible customer, independent of the business owner comparison. */
public final class AutoExCustomerIdentityObservation {
    private final Page page;
    private final PlaywrightActionSequence actions;
    private String observed = "";
    private String state = "not observed";

    public AutoExCustomerIdentityObservation(Page page, PlaywrightActionSequence actions) {
        this.page = Objects.requireNonNull(page);
        this.actions = Objects.requireNonNull(actions);
    }

    public String read() {
        observed = "";
        state = "not observed";
        var identity = page.locator(AutoExSelectors.LOGGED_IN_NAME);
        try {
            actions.awaitVisible(new AtfLocator(identity, AutoExSelectors.LOGGED_IN_NAME), budget ->
                    page.waitForCondition(() -> {
                        int count = identity.count();
                        boolean visible = count == 1 && identity.isVisible();
                        observed = visible ? identity.innerText(new com.microsoft.playwright.Locator.InnerTextOptions()
                                .setTimeout(budget.remainingMilliseconds())).trim() : "";
                        state = "url=" + page.url() + "; matches=" + count + "; visible=" + visible
                                + "; text=" + (observed.isBlank() ? "<not observed>" : observed);
                        if (count > 1) throw new IllegalStateException("Ambiguous customer identity; " + state);
                        return visible && !observed.isBlank();
                    }, new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds())));
            return observed;
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Signed-in customer observation was not confirmed; " + state, failure);
        }
    }
}
