package atf.ui.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.Objects;

import java.net.URI;
import java.time.Duration;

/** Executes and records page navigation, keyboard and stabilization actions. */
final class PlaywrightBrowserActions {
    private static final int STATE_TIMEOUT_MS = 10_000;
    private final UiActionRecorder recorder;
    private final Duration stateTimeout;

    PlaywrightBrowserActions(UiActionRecorder recorder) {
        this(recorder, Duration.ofMillis(STATE_TIMEOUT_MS));
    }

    PlaywrightBrowserActions(UiActionRecorder recorder, Duration stateTimeout) {
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.stateTimeout = requirePositive(stateTimeout, "stateTimeout");
    }

    void navigate(Page page, URI uri, String expected) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(uri, "uri");
        recorder.run("Navigate", uri.toString(), expected, () -> {
            page.navigate(uri.toString());
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            return "document loaded";
        });
    }

    Page clickAndWaitForPopup(Page page, Locator locator, String locatorTarget, String expected) {
        Objects.requireNonNull(page, "page");
        AtfLocator element = new AtfLocator(locator, locatorTarget);
        return recorder.run("Click", element.selector(), expected, () -> {
            requireExactlyOne(element, "Click");
            return page.waitForPopup(() -> element.locator().click());
        });
    }

    void pressKey(Page page, String key, String locatorTarget, String expected) {
        Objects.requireNonNull(page, "page");
        String target = PlaywrightActionSequence.requireLocatorTarget(locatorTarget);
        String pressedKey = Objects.requireNonNull(key, "key").trim();
        if (pressedKey.isEmpty()) throw new IllegalArgumentException("key must not be blank");
        recorder.run("Press key", target, expected, () -> {
            page.keyboard().press(pressedKey);
            return pressedKey + " pressed";
        });
    }

    void pressKey(Page page, String key, AtfLocator element, String expected) {
        Objects.requireNonNull(element, "element");
        pressKey(page, key, element.selector(), expected);
    }

    void waitForAttribute(Page page, AtfLocator element, String attribute,
                          String expectedValue, String expected) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(element, "element");
        String name = Objects.requireNonNull(attribute, "attribute").trim();
        String value = Objects.requireNonNull(expectedValue, "expectedValue");
        if (name.isEmpty()) throw new IllegalArgumentException("attribute must not be blank");
        recorder.run("Wait for state", element.selector(), expected, () -> {
            Locator target = element.locator();
            target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            waitUntil(page, () -> value.equals(target.getAttribute(name)),
                    name + "=" + value + " on " + element.selector());
            return name + "=" + value;
        });
    }

    void waitForText(Page page, AtfLocator element, String expectedText, String expected) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(element, "element");
        String value = Objects.requireNonNull(expectedText, "expectedText");
        if (value.isBlank()) throw new IllegalArgumentException("expectedText must not be blank");
        recorder.run("Wait for text", element.selector(), expected, () -> {
            Locator target = element.locator();
            target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            waitUntil(page, () -> target.isVisible() && target.innerText().contains(value),
                    "text contains '" + value + "' on " + element.selector());
            return value;
        });
    }

    void waitForEnabled(Page page, AtfLocator element, String expected) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(element, "element");
        recorder.run("Wait for enabled", element.selector(), expected, () -> {
            Locator target = element.locator();
            target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            waitUntil(page, target::isEnabled,
                    "enabled=true on " + element.selector());
            return "enabled=true";
        });
    }

    void waitForDisclosureOpen(Page page, AtfLocator summary, String expected) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(summary, "summary");
        recorder.run("Wait for disclosure", summary.selector(), expected, () -> {
            Locator summaryTarget = summary.locator();
            summaryTarget.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            Locator details = summaryTarget.locator("xpath=..");
            waitUntil(page, () -> details.getAttribute("open") != null,
                    "details.open=true on " + summary.selector());
            return "details.open=true";
        });
    }

    private void waitUntil(Page page, java.util.function.BooleanSupplier condition,
                           String description) {
        try {
            page.waitForCondition(condition,
                    new Page.WaitForConditionOptions().setTimeout(stateTimeout.toMillis()));
        } catch (RuntimeException error) {
            throw new IllegalStateException("Timed out waiting for " + description, error);
        }
    }

    private static Duration requirePositive(Duration timeout, String name) {
        Objects.requireNonNull(timeout, name);
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return timeout;
    }

    private static void requireExactlyOne(AtfLocator element, String operation) {
        int count = element.locator().count();
        if (count != 1) {
            throw new AssertionError(operation + " locator must resolve to exactly one element: "
                    + element.selector() + " (matched " + count + ")");
        }
    }
}
