package atf.ui.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/** Immutable, validated browser policy. It contains no Playwright types. */
public record UiBrowserConfiguration(
        URI baseUrl,
        String browser,
        boolean headless,
        Duration timeout,
        Duration navigationTimeout,
        int viewportWidth,
        int viewportHeight,
        int zoomPercent,
        boolean screenshotOnFailure,
        UiRetryPolicy retryPolicy,
        UiNetworkInspectionConfiguration networkInspection) {

    public static final int DEFAULT_ZOOM_PERCENT = 100;
    public static final int MIN_ZOOM_PERCENT = 25;
    public static final int MAX_ZOOM_PERCENT = 200;

    /** Backwards-compatible constructor for callers that use the pre-zoom policy. */
    public UiBrowserConfiguration(URI baseUrl, String browser, boolean headless,
                                  Duration timeout, Duration navigationTimeout,
                                  int viewportWidth, int viewportHeight,
                                  boolean screenshotOnFailure) {
        this(baseUrl, browser, headless, timeout, navigationTimeout,
                viewportWidth, viewportHeight, DEFAULT_ZOOM_PERCENT, screenshotOnFailure,
                UiRetryPolicy.none(), UiNetworkInspectionConfiguration.disabled());
    }

    /** Compatibility constructor for callers that use the zoom policy only. */
    public UiBrowserConfiguration(URI baseUrl, String browser, boolean headless,
                                  Duration timeout, Duration navigationTimeout,
                                  int viewportWidth, int viewportHeight,
                                  int zoomPercent, boolean screenshotOnFailure) {
        this(baseUrl, browser, headless, timeout, navigationTimeout,
                viewportWidth, viewportHeight, zoomPercent, screenshotOnFailure,
                UiRetryPolicy.none(), UiNetworkInspectionConfiguration.disabled());
    }

    /** Compatibility constructor for callers that already provide UI retry policy. */
    public UiBrowserConfiguration(URI baseUrl, String browser, boolean headless,
                                  Duration timeout, Duration navigationTimeout,
                                  int viewportWidth, int viewportHeight, int zoomPercent,
                                  boolean screenshotOnFailure, UiRetryPolicy retryPolicy) {
        this(baseUrl, browser, headless, timeout, navigationTimeout, viewportWidth, viewportHeight,
                zoomPercent, screenshotOnFailure, retryPolicy,
                UiNetworkInspectionConfiguration.disabled());
    }

    public UiBrowserConfiguration {
        baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        browser = Objects.requireNonNull(browser, "browser").trim().toLowerCase(Locale.ROOT);
        if (!browser.equals("chromium") && !browser.equals("firefox") && !browser.equals("webkit")) {
            throw new IllegalArgumentException("ui.browser must be chromium, firefox or webkit: " + browser);
        }
        timeout = positive(timeout, "timeout");
        navigationTimeout = positive(navigationTimeout, "navigationTimeout");
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("ui.viewport dimensions must be positive");
        }
        if (zoomPercent < MIN_ZOOM_PERCENT || zoomPercent > MAX_ZOOM_PERCENT) {
            throw new IllegalArgumentException("ui.zoomPercent must be between "
                    + MIN_ZOOM_PERCENT + " and " + MAX_ZOOM_PERCENT);
        }
        retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        networkInspection = Objects.requireNonNull(networkInspection, "networkInspection");
    }

    public static UiBrowserConfiguration defaults() {
        return new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofSeconds(10), Duration.ofSeconds(15), 1440, 900,
                DEFAULT_ZOOM_PERCENT, true, UiRetryPolicy.none(),
                UiNetworkInspectionConfiguration.disabled());
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
