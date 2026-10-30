package atf.autoex.support.browser;

import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

import java.util.Objects;
import java.util.Optional;

/** Opts into DOM activation only for the public site's visible Google vignette Close control. */
public final class AutoExVignetteHandler {
    private static final String CLOSE_EXPECTATION =
            "visible Google vignette is closed with its Close control";

    public boolean dismissIfPresent(Page page, PlaywrightActionSequence actions) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(actions, "actions");
        String currentUrl = page.url();
        if (!currentUrl.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT)) {
            return false;
        }

        try {
            page.waitForCondition(() -> !onVignette(page) || readyCloseControl(page).isPresent());
        } catch (TimeoutError error) {
            throw new IllegalStateException("Google vignette has no visible, enabled Close control within "
                    + "the configured browser timeout; the test will not bypass the overlay", error);
        }
        if (!onVignette(page)) return false;

        AtfLocator closeControl = readyCloseControl(page).orElseThrow(() -> new IllegalStateException(
                "Google vignette Close became unavailable; the test will not bypass the overlay"));
        String zoom = Objects.toString(page.evaluate(
                "() => getComputedStyle(document.documentElement).zoom"));
        actions.information("Observed Google vignette", currentUrl,
                "visible Close control in the site's displayed advert",
                "Close visible; document CSS zoom=" + zoom);
        actions.clickWithJavaScript(closeControl, CLOSE_EXPECTATION,
                () -> page.waitForURL(url -> !url.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT)));
        return true;
    }

    /** Dismisses an interception inside the enclosing product action, without another timeout or timing total. */
    public boolean dismissIfPresent(Page page, PlaywrightActionSequence actions,
                                    PlaywrightActionSequence.ObservationBudget budget) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(budget, "budget");
        if (!onVignette(page)) return false;
        try {
            page.waitForCondition(() -> !onVignette(page) || readyCloseControl(page).isPresent(),
                    new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
        } catch (TimeoutError unavailable) {
            throw new IllegalStateException("Google vignette has no visible, enabled Close within the original "
                    + "product action deadline; no overlay bypass or product click replay", unavailable);
        }
        if (!onVignette(page)) return false;
        AtfLocator close = readyCloseControl(page).orElseThrow(() -> new IllegalStateException(
                "Google vignette Close became unavailable; the product pointer will not be replayed"));
        long started = System.nanoTime();
        boolean activated = false, confirmed = false;
        try {
            // The parent Click owns the deadline and duration; this explicit DOM interaction is a child fact.
            close.locator().evaluate("""
                    element => {
                      if (!element.isConnected || !element.getClientRects().length || element.disabled)
                        throw new Error('Visible enabled Close is required');
                      element.click();
                    }
                    """, null, new Locator.EvaluateOptions().setTimeout(budget.remainingMilliseconds()));
            activated = true;
            page.waitForURL(url -> !url.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT),
                    new Page.WaitForURLOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.COMMIT)
                            .setTimeout(budget.remainingMilliseconds()));
            confirmed = true;
            return true;
        } finally {
            actions.information("Google vignette Close", close.selector(), CLOSE_EXPECTATION,
                    "explicit HTMLElement.click(); DOM activation, not trusted pointer; activated=" + activated
                            + "; exit confirmed=" + confirmed + "; elapsed="
                            + java.time.Duration.ofNanos(System.nanoTime() - started).toMillis()
                            + " ms; included in enclosing product Click; no product click replay");
        }
    }

    private static boolean onVignette(Page page) {
        return page.url().contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT);
    }

    private static Optional<AtfLocator> readyCloseControl(Page page) {
        for (Frame frame : page.frames()) {
            if (frame.isDetached()) continue;
            Locator candidate = frame.locator(AutoExSelectors.VIGNETTE_CLOSE_CONTROL);
            int matches = candidate.count();
            if (matches > 1) {
                throw new IllegalStateException("Google vignette Close is ambiguous; no control will be activated");
            }
            if (matches == 1 && candidate.isVisible() && candidate.isEnabled()) {
                return Optional.of(new AtfLocator(candidate, AutoExSelectors.VIGNETTE_CLOSE_CONTROL));
            }
        }
        return Optional.empty();
    }
}
