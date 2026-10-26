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
        actions.observation("Observed Google vignette", currentUrl,
                "visible Close control in the site's displayed advert",
                "Close visible; document CSS zoom=" + zoom);
        actions.clickWithJavaScript(closeControl, CLOSE_EXPECTATION,
                () -> page.waitForURL(url -> !url.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT)));
        return true;
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
