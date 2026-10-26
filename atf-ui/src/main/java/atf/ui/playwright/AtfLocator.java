package atf.ui.playwright;

import com.microsoft.playwright.Locator;

import java.util.Objects;

/** Immutable locator evidence, including an optional explicit sensitivity declaration. */
public record AtfLocator(Locator locator, String selector, boolean sensitiveData) {
    public AtfLocator(Locator locator, String selector) {
        this(locator, selector, false);
    }

    public AtfLocator {
        locator = Objects.requireNonNull(locator, "locator");
        selector = PlaywrightActionSequence.requireLocatorTarget(selector);
    }

    /** Marks a locator as sensitive when its selector cannot express that safely. */
    public AtfLocator withSensitiveData() {
        return sensitiveData ? this : new AtfLocator(locator, selector, true);
    }
}
