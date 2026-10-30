package atf.ui.playwright;

import com.microsoft.playwright.Locator;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.TimeoutError;
import atf.ui.playwright.selection.PlaywrightSelectAction;

/** Executes and records actions whose target is a Playwright locator. */
final class PlaywrightLocatorActions {
    private static final String JAVASCRIPT_CLICK_OPERATION = "JavaScript click";
    private static final String JAVASCRIPT_CLICK_SCRIPT = "element => element.click()";
    private static final String JAVASCRIPT_CLICK_OBSERVATION =
            "HTMLElement.click() completed; caller postcondition satisfied; DOM activation, not a trusted pointer click";
    private static final Pattern SENSITIVE_TARGET = Pattern.compile(
            "password|token|secret|credential|authorization|cookie|api[-_]?key"
                    + "|(?:^|[^a-z])(?:e-?mail|first[_-]?name|last[_-]?name|date[_-]?of[_-]?birth|dob"
                    + "|phone|mobile|postal[_-]?code|zip[_-]?code|house[_-]?number|street|city|state|address"
                    + "|card[-_ ]?(?:number|no|num|holder)|cc[-_ ]?(?:number|num)"
                    + "|pan|cvc|cvv|security[-_ ]?code|payment[-_ ]?(?:card|number|security)"
                    + ")(?:$|[^a-z])",
            Pattern.CASE_INSENSITIVE);
    private final UiActionRecorder recorder;
    private final Duration timeout;
    private final java.util.function.LongSupplier ticks;

    PlaywrightLocatorActions(UiActionRecorder recorder, Duration timeout) {
        this(recorder, timeout, System::nanoTime);
    }

    PlaywrightLocatorActions(UiActionRecorder recorder, Duration timeout, java.util.function.LongSupplier ticks) {
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.ticks = Objects.requireNonNull(ticks, "ticks");
    }

    void click(Locator locator, String locatorTarget, String expected) {
        click(new AtfLocator(locator, locatorTarget), expected);
    }

    void click(AtfLocator element, String expected) {
        String target = element.selector();
        recorder.run("Click", target, expected, isSensitiveTarget(element), () -> {
            // The native strict action owns target resolution, actionability and its single timeout.
            element.locator().click();
            return "click completed";
        });
    }

    void clickWithJavaScript(AtfLocator element, String expected, Runnable postcondition) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(postcondition, "postcondition");
        recorder.run(JAVASCRIPT_CLICK_OPERATION, element.selector(), expected, isSensitiveTarget(element), () -> {
            requireExactlyOne(element, JAVASCRIPT_CLICK_OPERATION);
            Locator target = element.locator();
            target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            if (!target.isEnabled()) {
                throw new IllegalStateException("JavaScript click target must be enabled: " + element.selector());
            }
            target.evaluate(JAVASCRIPT_CLICK_SCRIPT);
            postcondition.run();
            return JAVASCRIPT_CLICK_OBSERVATION;
        });
    }

    void clickAndAwait(AtfLocator element, String expected, Duration timeout,
                       Consumer<PlaywrightActionSequence.ObservationBudget> postcondition) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(postcondition, "postcondition");
        recorder.run("Click", element.selector(), expected, isSensitiveTarget(element), () -> {
            var budget = new PlaywrightActionSequence.ObservationBudget(timeout, ticks);
            element.locator().click(new Locator.ClickOptions()
                    .setTimeout(budget.remainingMilliseconds()).setNoWaitAfter(true));
            try {
                budget.remainingMilliseconds();
                postcondition.accept(budget);
                budget.remainingMilliseconds();
            } catch (RuntimeException failure) {
                throw new IllegalStateException("Pointer click completed; explicit postcondition not confirmed: "
                        + element.selector(), failure);
            }
            return "pointer click completed; explicit postcondition satisfied";
        });
    }

    String getText(Locator locator, String locatorTarget, String expected) {
        return getText(new AtfLocator(locator, locatorTarget), expected);
    }

    String getText(AtfLocator element, String expected) {
        String target = element.selector();
        boolean sensitive = isSensitiveTarget(element);
        return recorder.run("Get text", target, expected, sensitive,
                () -> readableText(element.locator()), observed -> sensitive ? "[REDACTED]" : observed);
    }

    void fill(AtfLocator element, String value, String expected) {
        String target = element.selector();
        String displayValue = displayValue(element, value);
        recorder.run("Fill", target, withValue(expected, displayValue, value), isSensitiveTarget(element), () -> {
            element.locator().fill(value == null ? "" : value);
            return "value entered: " + displayValue;
        });
    }

    void selectOption(AtfLocator element, String label, String expected) {
        String target = element.selector();
        String displayLabel = displayValue(element, label);
        recorder.run("Select option", target, withValue(expected, displayLabel, label),
                isSensitiveTarget(element), () -> {
            try {
                new PlaywrightSelectAction(element, new PlaywrightActionSequence.ObservationBudget(timeout)).select(label);
            } catch (TimeoutError failure) {
                throw new TimeoutError("Select option original timeout " + timeout.toMillis() + "ms: " + failure.getMessage(), failure);
            }
            return "option selected: " + displayLabel;
        });
    }

    Locator findElement(Locator locator, String locatorTarget) {
        return findElement(new AtfLocator(locator, locatorTarget));
    }

    Locator findElement(AtfLocator element) {
        String target = element.selector();
        recorder.run("Find element", target, "element exists", isSensitiveTarget(element), () -> {
            if (element.locator().count() == 0) {
                throw new AssertionError("Element was not found: " + target);
            }
            return "element found";
        });
        return element.locator();
    }

    String expectText(Locator locator, String locatorTarget, String expected) {
        return expectText(new AtfLocator(locator, locatorTarget), expected);
    }

    String expectText(AtfLocator element, String expected) {
        String target = element.selector();
        String expectedText = expected == null ? "" : expected.trim();
        boolean sensitive = isSensitiveTarget(element);
        String displayExpected = sensitive ? "[REDACTED]" : expectedText;
        return recorder.run("Expect text", target, "text contains: " + displayExpected, sensitive, () -> {
            String observed = readableText(element.locator());
            if (!observed.contains(expectedText)) {
                throw new AssertionError(sensitive ? "Sensitive text did not match the expected value"
                        : "Expected text '" + expectedText + "' but observed '" + observed + "'");
            }
            return sensitive ? "[REDACTED]" : observed;
        });
    }

    String expectValue(AtfLocator element, String expectedValue) {
        String target = element.selector();
        String expected = expectedValue == null ? "" : expectedValue;
        boolean sensitive = isSensitiveTarget(element);
        String displayExpected = sensitive ? "[REDACTED]" : expected;
        return recorder.run("Expect value", target, "value equals: " + displayExpected, sensitive, () -> {
            String observed = element.locator().inputValue();
            if (!observed.equals(expected)) {
                throw new AssertionError(sensitive ? "Sensitive field value did not match the expected value"
                        : "Expected value '" + expected + "' but observed '" + observed + "'");
            }
            return sensitive ? "[REDACTED]" : observed;
        });
    }

    void focus(Locator locator, String locatorTarget, String expected) {
        focus(new AtfLocator(locator, locatorTarget), expected);
    }

    void focus(AtfLocator element, String expected) {
        String target = element.selector();
        recorder.run("Focus", target, expected, isSensitiveTarget(element), () -> {
            element.locator().focus();
            return "focus completed";
        });
    }

    void waitForVisible(Locator locator, String locatorTarget) {
        waitForVisible(new AtfLocator(locator, locatorTarget));
    }

    void waitForVisible(AtfLocator element) {
        String target = element.selector();
        recorder.run("Wait for visible", target, "state=Visible", isSensitiveTarget(element), () -> {
            element.locator().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            return "state reached";
        });
    }

    void assertVisible(Locator locator, String locatorTarget) {
        assertVisible(new AtfLocator(locator, locatorTarget));
    }

    void assertVisible(AtfLocator element) {
        String target = element.selector();
        recorder.run("Expect visible", target, "locator is visible", isSensitiveTarget(element), () -> {
            if (!element.locator().isVisible()) {
                throw new AssertionError("Locator is not visible: " + target);
            }
            return "operation completed";
        });
    }

    private static void requireExactlyOne(AtfLocator element, String operation) {
        int count = element.locator().count();
        if (count != 1) {
            throw new AssertionError(operation + " locator must resolve to exactly one element: "
                    + element.selector() + " (matched " + count + ")");
        }
    }

    private static String withValue(String expected, String value, String originalValue) {
        String context = expected == null ? "" : expected.trim();
        if (value.equals("[REDACTED]") && originalValue != null && !originalValue.isBlank()) {
            context = context.replace(originalValue, "[REDACTED]");
        }
        return (context.isBlank() ? "value" : context) + " · value=" + value;
    }

    private static String displayValue(AtfLocator element, String value) {
        if (isSensitiveTarget(element)) return "[REDACTED]";
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    static boolean isSensitiveTarget(AtfLocator element) {
        return element.sensitiveData() || SENSITIVE_TARGET.matcher(element.selector()).find();
    }

    private static String readableText(Locator locator) {
        List<Locator> rows = locator.locator("tr").all();
        if (!rows.isEmpty()) {
            String tableText = rows.stream()
                    .map(row -> String.join(" | ", row.locator("th,td").allInnerTexts()))
                    .collect(Collectors.joining("\n"))
                    .trim();
            if (!tableText.isBlank()) return tableText;
        }
        return locator.innerText().trim();
    }
}
