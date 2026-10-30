package atf.autoex.runtime.catalog;

import atf.autoex.locator.AutoExRoutes;
import atf.autoex.support.browser.AutoExVignetteHandler;
import atf.ui.recovery.UiRecoveryOperation;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.runtime.navigation.AutoExReadOnlyNavigation;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** One pointer activation; an owned GET document and the configured product confirm the transition. */
public final class AutoExProductTransition {
    private final Page page;
    private final PlaywrightActionSequence actions;
    private final URI origin;
    private final AutoExReadOnlyNavigation navigation;

    public AutoExProductTransition(Page page, PlaywrightActionSequence actions, URI origin, AutoExReadOnlyNavigation navigation) {
        this.page = Objects.requireNonNull(page); this.actions = Objects.requireNonNull(actions);
        this.origin = Objects.requireNonNull(origin); this.navigation = Objects.requireNonNull(navigation);
    }

    public void open(String expectedProduct) {
        if (expectedProduct == null || expectedProduct.isBlank()) throw new IllegalArgumentException("Expected product must not be blank");
        Locator link = page.locator(AutoExSelectors.PRODUCT_DETAILS_LINK).first();
        URI target = URI.create(page.url()).resolve(Objects.requireNonNull(link.getAttribute("href"), "product href"));
        if (!ownedProduct(target)) throw new IllegalStateException("Product target is not an owned read-only details route");
        try {
            actions.clickAndAwait(new AtfLocator(link, AutoExSelectors.PRODUCT_DETAILS_LINK),
                    "first matching product details open and the configured product is confirmed", budget -> {
                page.waitForURL(url -> target.toString().equals(url) || url.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT),
                        new Page.WaitForURLOptions().setWaitUntil(WaitUntilState.COMMIT)
                                .setTimeout(budget.remainingMilliseconds()));
                new AutoExVignetteHandler().dismissIfPresent(page, actions, budget);
                if (!target.toString().equals(page.url())) {
                    page.waitForURL(target.toString(), new Page.WaitForURLOptions().setWaitUntil(WaitUntilState.COMMIT)
                            .setTimeout(budget.remainingMilliseconds()));
                }
                var recovery = navigation.within(budget);
                var name = page.locator(AutoExSelectors.PRODUCT_INFORMATION_NAME);
                var ready = name.or(page.locator(AutoExSelectors.STORE_BUSY_MESSAGE_LOCATOR));
                while (true) {
                    recovery.recoverIfBusy();
                    navigation.requireProductDocument(target);
                    ready.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(budget.remainingMilliseconds()));
                    var observed = observe(ready, budget);
                    if (!observed.product()) continue;
                    String actual = observed.text().trim();
                    navigation.requireProductDocument(target);
                    if (!expectedProduct.equals(actual)) throw new IllegalStateException("Configured product mismatch; expected="
                            + expectedProduct + "; actual=" + actual);
                    return;
                }
            });
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Product transition not confirmed; expected path=" + target.getPath()
                    + "; " + navigation.description(), failure);
        }
    }

    /** One native locator resolution reads both identity and text; no expected value enters the observation. */
    private static ProductContent observe(Locator ready, PlaywrightActionSequence.ObservationBudget budget) {
        var result = (Map<?, ?>) ready.evaluate("""
                (element, selector) => ({product: element.matches(selector), text: element.innerText})
                """, AutoExSelectors.PRODUCT_INFORMATION_NAME,
                new Locator.EvaluateOptions().setTimeout(budget.remainingMilliseconds()));
        return new ProductContent((Boolean) result.get("product"), (String) result.get("text"));
    }

    private record ProductContent(boolean product, String text) { }

    private boolean ownedProduct(URI target) {
        return target.getRawUserInfo() == null
                && target.getRawQuery() == null && target.getRawFragment() == null
                && target.getRawPath().matches(AutoExRoutes.PRODUCT_DETAILS.path() + "[0-9]+")
                && UiRecoveryOperation.origin(origin).equals(UiRecoveryOperation.origin(target));
    }
}
