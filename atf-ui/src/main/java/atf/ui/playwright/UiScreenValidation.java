package atf.ui.playwright;

import atf.ui.config.UiScreenValidationConfiguration;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Captures one visual state for a group of UI assertions.
 *
 * <p>The returned evidence contains only the inline Base64 payload and its
 * semantic title. It does not write image files and it does not know how the
 * report renderer stores the evidence. A caller can therefore collect many
 * assertions and publish them with one evidence object.</p>
 */
public final class UiScreenValidation {
    private static final int MAX_CAPTURE_ATTEMPTS = 2;
    private static final int NAVIGATION_SETTLE_MILLIS = 75;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private UiScreenValidation() { }

    public static Evidence capture(Page page, UiScreenValidationConfiguration configuration,
                                   String title) {
        return capture(page, configuration, title, List.of());
    }

    /** Captures one visual state while masking caller-selected sensitive page regions. */
    public static Evidence capture(Page page, UiScreenValidationConfiguration configuration,
                                   String title, List<Locator> masks) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(configuration, "configuration");
        List<Locator> safeMasks = List.copyOf(Objects.requireNonNull(masks, "masks"));
        String safeTitle = title == null || title.isBlank() ? "UI validation" : title.trim();
        if (!configuration.enabled()) return new Evidence(safeTitle, "", false);
        Page.ScreenshotOptions options = new Page.ScreenshotOptions()
                .setFullPage(configuration.fullPage())
                .setTimeout(configuration.screenshotTimeout().toMillis());
        if (!safeMasks.isEmpty()) options.setMask(safeMasks).setMaskColor("#343a40");
        PlaywrightException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_CAPTURE_ATTEMPTS; attempt++) {
            try {
                page.waitForLoadState(LoadState.LOAD);
                if (attempt > 1) page.waitForTimeout(NAVIGATION_SETTLE_MILLIS);
                byte[] png = page.screenshot(options);
                String base64 = Base64.getEncoder().encodeToString(png);
                return new Evidence(safeTitle, base64, true);
            } catch (PlaywrightException transientNavigation) {
                lastFailure = transientNavigation;
            }
        }
        throw lastFailure;
    }

    public record Evidence(String title, String base64Png, boolean captured) {
        public Evidence {
            title = title == null || title.isBlank() ? "UI validation" : title.trim();
            base64Png = base64Png == null ? "" : base64Png;
        }

        /** Returns true when the evidence contains a captured Base64 screenshot payload. */
        public boolean hasScreenshotPayload() {
            return captured && isValidPngPayload(base64Png);
        }

        /** @deprecated use {@link #hasScreenshotPayload()} for renderer-neutral terminology. */
        @Deprecated
        public boolean hasInlineImage() {
            return hasScreenshotPayload();
        }
    }

    /**
     * Validates the renderer-neutral screenshot contract without decoding or
     * writing an image file. The report stores raw Base64 PNG bytes, not a
     * data-URI prefix.
     */
    public static boolean isValidPngPayload(String base64Png) {
        if (base64Png == null || base64Png.isBlank()) return false;
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Png.trim());
        } catch (IllegalArgumentException invalidBase64) {
            return false;
        }
        if (decoded.length < PNG_SIGNATURE.length) return false;
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (decoded[index] != PNG_SIGNATURE[index]) return false;
        }
        return true;
    }
}
