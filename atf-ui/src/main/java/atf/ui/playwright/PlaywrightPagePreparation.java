package atf.ui.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Explicit document preparation; native input and its actionability keep their own unchanged budget. */
public final class PlaywrightPagePreparation {
    private final Page page;
    private final Duration navigationBudget;
    private final LongSupplier ticks;

    public PlaywrightPagePreparation(Page page, Duration navigationBudget) {
        this(page, navigationBudget, System::nanoTime);
    }

    PlaywrightPagePreparation(Page page, Duration navigationBudget, LongSupplier ticks) {
        this.page = Objects.requireNonNull(page, "page");
        this.navigationBudget = Objects.requireNonNull(navigationBudget, "navigationBudget");
        this.ticks = Objects.requireNonNull(ticks, "ticks");
        if (navigationBudget.isNegative() || navigationBudget.isZero())
            throw new IllegalArgumentException("Document preparation requires a positive navigation budget");
    }

    public PreparedPage navigate(String url) {
        Objects.requireNonNull(url, "url");
        return prepare(budget -> page.navigate(url, new Page.NavigateOptions().setTimeout(budget.remainingMilliseconds())));
    }

    public PreparedPage setContent(String html) {
        Objects.requireNonNull(html, "html");
        return prepare(budget -> page.setContent(html, new Page.SetContentOptions().setTimeout(budget.remainingMilliseconds())));
    }

    private PreparedPage prepare(Consumer<PlaywrightActionSequence.ObservationBudget> load) {
        var budget = new PlaywrightActionSequence.ObservationBudget(navigationBudget, ticks);
        long started = ticks.getAsLong();
        try {
            load.accept(budget);
            // Initialize the current native document context without inspecting or changing any action target.
            page.locator(":root").waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED).setTimeout(budget.remainingMilliseconds()));
            byte[] png = page.screenshot(new Page.ScreenshotOptions().setType(ScreenshotType.PNG)
                    .setFullPage(false).setAnimations(ScreenshotAnimations.ALLOW).setTimeout(budget.remainingMilliseconds()));
            budget.remainingMilliseconds();
            return new PreparedPage(png, Duration.ofNanos(ticks.getAsLong() - started).toMillis(), page.url());
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Page preparation failed within its " + navigationBudget.toMillis()
                    + " ms navigation budget", failure);
        }
    }

    /** A fresh native viewport receipt, never a claim that a particular target is actionable. */
    public record PreparedPage(byte[] viewportPng, long elapsedMillis, String url) {
        public PreparedPage {
            viewportPng = Objects.requireNonNull(viewportPng, "viewportPng").clone();
            Objects.requireNonNull(url, "url");
        }
        @Override public byte[] viewportPng() { return viewportPng.clone(); }
    }
}
