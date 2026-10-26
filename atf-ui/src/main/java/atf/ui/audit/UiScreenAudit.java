package atf.ui.audit;

import atf.core.ui.AtfUiActionRecord;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.UiScreenValidation;

import java.util.List;
import java.util.Objects;

/** Records renderer-neutral screen evidence; the consumer owns assertions and their verdict. */
public final class UiScreenAudit {
    private UiScreenAudit() { }

    public static void record(PlaywrightActionSequence actions, String title, String url,
                              int assertionCount, UiScreenValidation.Evidence screenshot, boolean passed) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(screenshot, "screenshot");
        if (assertionCount < 0) throw new IllegalArgumentException("assertionCount must not be negative");
        String safeUrl = url == null || url.isBlank() ? "unavailable" : url.trim();
        actions.observation("Audit screen contract", title, "all component assertions pass",
                "status=" + (passed ? "PASS" : "FAIL")
                        + "; assertions=" + assertionCount
                        + "; screenshot=" + (screenshot.hasScreenshotPayload() ? "valid PNG" : "not captured")
                        + "; url=" + safeUrl);
    }

    public static List<AtfUiActionRecord> drain(PlaywrightActionSequence actions, String title, String url,
                                               int assertionCount, UiScreenValidation.Evidence screenshot,
                                               boolean passed, boolean screenshotRequired) {
        if (screenshotRequired) requireScreenshot(screenshot);
        record(actions, title, url, assertionCount, screenshot, passed);
        return actions.drain();
    }

    public static void requireScreenshot(UiScreenValidation.Evidence screenshot) {
        if (!Objects.requireNonNull(screenshot, "screenshot").hasScreenshotPayload()) {
            throw new IllegalStateException("UI validation cannot be published without a valid PNG screenshot");
        }
    }
}
