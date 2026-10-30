package atf.ui.playwright;

import atf.core.ui.AtfUiActionRecord;
import atf.ui.config.UiRetryPolicy;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Stable facade for ordered Playwright evidence.
 *
 * <p>Execution is delegated to browser and locator action owners; this class
 * keeps only the public sequence vocabulary and recorder lifecycle.</p>
 */
public final class PlaywrightActionSequence {
    private static final Duration DEFAULT_STATE_TIMEOUT = Duration.ofSeconds(10);
    private final UiActionRecorder recorder;
    private final PlaywrightLocatorActions locatorActions;
    private final PlaywrightBrowserActions browserActions;
    private final Duration actionTimeout;

    public PlaywrightActionSequence() {
        this(UiActionTrace.noOp());
    }

    public PlaywrightActionSequence(UiActionTrace trace) {
        this(trace, DEFAULT_STATE_TIMEOUT);
    }

    PlaywrightActionSequence(UiActionTrace trace, Duration stateTimeout) {
        this(trace, stateTimeout, UiRetryPolicy.none());
    }

    PlaywrightActionSequence(UiActionTrace trace, Duration stateTimeout, UiRetryPolicy retryPolicy) {
        this(trace, stateTimeout, retryPolicy, System::nanoTime);
    }

    PlaywrightActionSequence(UiActionTrace trace, Duration stateTimeout, UiRetryPolicy retryPolicy, LongSupplier ticks) {
        actionTimeout = stateTimeout;
        recorder = new UiActionRecorder(trace);
        recorder.setRetryPolicy(retryPolicy);
        locatorActions = new PlaywrightLocatorActions(recorder, stateTimeout, ticks);
        browserActions = new PlaywrightBrowserActions(recorder, stateTimeout);
    }

    public void navigate(Page page, URI uri) {
        navigate(page, uri, "document is loaded");
    }

    public void navigate(Page page, URI uri, String expected) {
        browserActions.navigate(page, uri, expected);
    }

    public void click(Locator locator, String locatorTarget, String expected) {
        locatorActions.click(locator, locatorTarget, expected);
    }

    public void click(AtfLocator element, String expected) {
        locatorActions.click(element, expected);
    }

    /**
     * One native pointer activation, followed by the caller's read-only postcondition.
     * Navigation auto-wait is replaced only for this explicit operation; both stages
     * share the action timeout. A failed or unknown outcome never replays the click.
     * The callback may use native read-only waits, each bounded by the same remaining budget.
     */
    public void clickAndAwait(AtfLocator element, String expected, Consumer<ObservationBudget> postcondition) {
        locatorActions.clickAndAwait(element, expected, actionTimeout, postcondition);
    }

    /** One monotonic deadline for the pointer action and every read-only confirmation. */
    public static final class ObservationBudget {
        private final LongSupplier ticks;
        private final long started;
        private final long timeoutNanos;

        ObservationBudget(Duration timeout) { this(timeout, System::nanoTime); }

        ObservationBudget(Duration timeout, LongSupplier ticks) {
            this.ticks = java.util.Objects.requireNonNull(ticks, "ticks");
            started = ticks.getAsLong();
            timeoutNanos = timeout.toNanos();
        }

        public double remainingMilliseconds() {
            long remaining = timeoutNanos - (ticks.getAsLong() - started);
            if (remaining <= 0) throw new IllegalStateException("Action observation budget exhausted");
            return Math.max(1d, remaining / 1_000_000d);
        }
    }

    /**
     * Explicit DOM activation, never an automatic fallback for {@link #click(AtfLocator, String)}.
     * The caller's read-only postcondition must succeed before the single action is recorded as PASS.
     * This does not exercise trusted pointer input or hit testing and is never automatically retried.
     */
    public void clickWithJavaScript(AtfLocator element, String expected, Runnable postcondition) {
        locatorActions.clickWithJavaScript(element, expected, postcondition);
    }

    public Page clickAndWaitForPopup(Page page, Locator locator, String locatorTarget,
                                     String expected) {
        return browserActions.clickAndWaitForPopup(page, locator, locatorTarget, expected);
    }

    /** Records one pointer activation and bounded event observation; never automatically replays the click. */
    public com.microsoft.playwright.Download download(Page page, AtfLocator element, String expected,
            java.util.function.BiConsumer<ObservationBudget, java.util.function.BooleanSupplier> readiness) {
        return browserActions.download(page, element, expected, readiness);
    }

    public String getText(Locator locator, String locatorTarget, String expected) {
        return locatorActions.getText(locator, locatorTarget, expected);
    }

    public String getText(AtfLocator element, String expected) {
        // The caller may need the value for an in-memory business assertion; recorded evidence stays redacted.
        return locatorActions.getText(element, expected);
    }

    /** Fills a field and publishes its replay value, redacting sensitive targets. */
    public void fill(AtfLocator element, String value, String expected) {
        locatorActions.fill(element, value, expected);
    }

    /** Observes readiness, then selects the native label once within one action deadline. */
    public void selectOption(AtfLocator element, String label, String expected) {
        locatorActions.selectOption(element, label, expected);
    }

    public Locator findElement(Locator locator, String locatorTarget) {
        return locatorActions.findElement(locator, locatorTarget);
    }

    public Locator findElement(AtfLocator element) {
        return locatorActions.findElement(element);
    }

    public String expectText(Locator locator, String locatorTarget, String expected) {
        return locatorActions.expectText(locator, locatorTarget, expected);
    }

    public String expectText(AtfLocator element, String expected) {
        return locatorActions.expectText(element, expected);
    }

    /** Verifies a native input/select value and records the observed value. */
    public String expectValue(AtfLocator element, String expectedValue) {
        return locatorActions.expectValue(element, expectedValue);
    }

    public void focus(Locator locator, String locatorTarget, String expected) {
        locatorActions.focus(locator, locatorTarget, expected);
    }

    public void focus(AtfLocator element, String expected) {
        locatorActions.focus(element, expected);
    }

    public void pressKey(Page page, String key, String locatorTarget, String expected) {
        browserActions.pressKey(page, key, locatorTarget, expected);
    }

    public void pressKey(Page page, String key, AtfLocator element, String expected) {
        browserActions.pressKey(page, key, element, expected);
    }

    public void waitForVisible(Locator locator, String locatorTarget) {
        locatorActions.waitForVisible(locator, locatorTarget);
    }

    public void waitForVisible(AtfLocator element) {
        locatorActions.waitForVisible(element);
    }

    /** One deadline for explicit readiness work and the final native visibility check; never automatically replayed. */
    public void awaitVisible(AtfLocator element, Consumer<ObservationBudget> readiness) {
        browserActions.awaitVisible(element, readiness);
    }

    /** Waits for a concrete DOM text marker and records the synchronization step. */
    public void waitForText(Page page, AtfLocator element, String expectedText, String expected) {
        browserActions.waitForText(page, element, expectedText, expected);
    }

    /** Waits for a form control to become enabled after its page data is ready. */
    public void waitForEnabled(Page page, AtfLocator element, String expected) {
        browserActions.waitForEnabled(page, element, expected);
    }

    public void assertVisible(Locator locator, String locatorTarget) {
        locatorActions.assertVisible(locator, locatorTarget);
    }

    public void assertVisible(AtfLocator element) {
        locatorActions.assertVisible(element);
    }

    /** Waits on a DOM state transition instead of sleeping for an arbitrary duration. */
    public void waitForAttribute(Page page, AtfLocator element, String attribute,
                                 String expectedValue, String expected) {
        browserActions.waitForAttribute(page, element, attribute, expectedValue, expected);
    }

    /** Waits for the native disclosure state; the DOM's {@code details.open} is authoritative. */
    public void waitForDisclosureOpen(Page page, AtfLocator summary, String expected) {
        browserActions.waitForDisclosureOpen(page, summary, expected);
    }

    public void observation(String operation, String target, String expected, String observed) {
        recorder.observation(operation, target, expected, observed);
    }

    /** Decision evidence without asserting success; preserves the existing PASS observation contract. */
    public void information(String operation, String target, String expected, String observed) {
        recorder.information(operation, target, expected, observed);
    }

    public List<AtfUiActionRecord> snapshot() {
        return recorder.snapshot();
    }

    public List<AtfUiActionRecord> drain() {
        return recorder.drain();
    }

    static String requireLocatorTarget(String locatorTarget) {
        if (locatorTarget == null || locatorTarget.trim().isEmpty()) {
            throw new IllegalArgumentException("A Playwright action requires a non-blank locator target");
        }
        String target = locatorTarget.trim();
        if (!looksLikeCssSelector(target)) {
            throw new IllegalArgumentException(
                    "A Playwright action requires the effective CSS locator, not a prose description: " + target);
        }
        return target;
    }

    private static boolean looksLikeCssSelector(String target) {
        return target.startsWith("#") || target.startsWith(".") || target.startsWith("[")
                || target.startsWith(":") || target.matches(".*[.#\\[\\]:>+~()].*")
                || target.matches("[A-Za-z][A-Za-z0-9_-]*");
    }
}
