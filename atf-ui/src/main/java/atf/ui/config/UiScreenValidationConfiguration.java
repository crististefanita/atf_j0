package atf.ui.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Policy for visual evidence produced by a UI validation.
 *
 * <p>The policy is deliberately independent of the report renderer. The
 * Playwright adapter captures the page, while {@code atf-reporting} embeds
 * the resulting Base64 image in the audit artifact.</p>
 */
public record UiScreenValidationConfiguration(boolean enabled, boolean fullPage,
                                              Duration screenshotTimeout) {
    public static final Duration DEFAULT_SCREENSHOT_TIMEOUT = Duration.ofSeconds(30);

    public UiScreenValidationConfiguration(boolean enabled, boolean fullPage) {
        this(enabled, fullPage, DEFAULT_SCREENSHOT_TIMEOUT);
    }

    public UiScreenValidationConfiguration {
        screenshotTimeout = Objects.requireNonNull(screenshotTimeout, "screenshotTimeout");
        if (screenshotTimeout.isZero() || screenshotTimeout.isNegative()) {
            throw new IllegalArgumentException("screenshotTimeout must be positive");
        }
    }

    public static UiScreenValidationConfiguration defaults() {
        return new UiScreenValidationConfiguration(true, false, DEFAULT_SCREENSHOT_TIMEOUT);
    }
}
