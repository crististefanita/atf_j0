package atf.autoex.runtime.account;

import atf.autoex.locator.AutoExSelectors;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.Objects;
import java.util.regex.Pattern;

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
            actions.awaitVisible(new AtfLocator(identity, AutoExSelectors.LOGGED_IN_NAME), budget -> {
                identity.filter(new Locator.FilterOptions().setHasText(Pattern.compile("\\S")))
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(budget.remainingMilliseconds()));
                int count = identity.count();
                if (count != 1) throw new IllegalStateException("Customer identity must match exactly one element");
                observed = identity.innerText(new Locator.InnerTextOptions().setTimeout(budget.remainingMilliseconds())).trim();
                if (observed.isBlank()) throw new IllegalStateException("Customer identity changed to an empty value");
            });
            return observed;
        } catch (RuntimeException failure) {
            try {
                int count = identity.count();
                boolean visible = count == 1 && identity.isVisible();
                state = "url=" + page.url() + "; matches=" + count + "; visible=" + visible
                        + "; text=" + (observed.isBlank() ? "<not observed>" : observed);
            } catch (RuntimeException diagnostic) {
                if (diagnostic != failure) failure.addSuppressed(diagnostic);
            }
            throw new IllegalStateException("Signed-in customer observation was not confirmed; " + state, failure);
        }
    }
}
