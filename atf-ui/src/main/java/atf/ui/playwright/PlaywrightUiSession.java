package atf.ui.playwright;

import atf.core.reporting.console.AtfConsoleSink;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.network.UiNetworkInspector;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;

import java.net.URI;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Scoped Playwright adapter. One instance owns exactly one browser context. */
public final class PlaywrightUiSession implements AutoCloseable {
    private static final String EXECUTABLE_PROPERTY = "atf.ui.playwright.executablePath";
    private static final String EXECUTABLE_ENVIRONMENT = "ATF_UI_PLAYWRIGHT_EXECUTABLE_PATH";
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final UiActionTrace actionTrace;
    private final Duration actionTimeout;
    private final UiRetryPolicy retryPolicy;
    private final UiNetworkInspector networkInspector;
    private boolean closed;

    private PlaywrightUiSession(Playwright playwright, Browser browser,
                                BrowserContext context, Page page, UiActionTrace actionTrace,
                                Duration actionTimeout, UiRetryPolicy retryPolicy,
                                UiNetworkInspector networkInspector) {
        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
        this.page = page;
        this.actionTrace = Objects.requireNonNull(actionTrace, "actionTrace");
        this.actionTimeout = Objects.requireNonNull(actionTimeout, "actionTimeout");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.networkInspector = Objects.requireNonNull(networkInspector, "networkInspector");
    }

    public static PlaywrightUiSession open(UiBrowserConfiguration configuration) {
        return open(configuration, AtfConsoleSink.noOp());
    }

    /** Opens a session with an injected diagnostic sink; stdout remains console-owned. */
    public static PlaywrightUiSession open(UiBrowserConfiguration configuration,
                                           AtfConsoleSink console) {
        Objects.requireNonNull(configuration, "configuration");
        UiActionTrace trace = UiActionTrace.console(Objects.requireNonNull(console, "console"));
        Playwright playwright = Playwright.create();
        try {
            BrowserType type = switch (configuration.browser()) {
                case "chromium" -> playwright.chromium();
                case "firefox" -> playwright.firefox();
                case "webkit" -> playwright.webkit();
                default -> throw new IllegalArgumentException("Unsupported browser: " + configuration.browser());
            };
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(configuration.headless());
            configuredExecutablePath().ifPresent(launchOptions::setExecutablePath);
            Browser browser = type.launch(launchOptions);
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(new ViewportSize(configuration.viewportWidth(), configuration.viewportHeight())));
            context.addInitScript(zoomInitScript(configuration.zoomPercent()));
            context.setDefaultTimeout(configuration.timeout().toMillis());
            context.setDefaultNavigationTimeout(configuration.navigationTimeout().toMillis());
            Page page = context.newPage();
            UiNetworkInspector networkInspector =
                    new UiNetworkInspector(configuration.networkInspection());
            networkInspector.attach(page);
            return new PlaywrightUiSession(playwright, browser, context, page, trace,
                    configuration.timeout(), configuration.retryPolicy(), networkInspector);
        } catch (RuntimeException error) {
            playwright.close();
            throw error;
        }
    }

    public Page page() { return page; }

    /** Generic opt-in network observer for consumers that need UI/API correlation. */
    public UiNetworkInspector networkInspector() { return networkInspector; }

    /**
     * Init scripts can run before the document element exists.  Keep the
     * configurable zoom, but apply it only after the root is available so a
     * normal report load never emits a page error.
     */
    static String zoomInitScript(int zoomPercent) {
        return "(() => { if (window !== window.top) return; const applyZoom = () => { "
                + "if (document.documentElement) { document.documentElement.style.zoom = '"
                + zoomPercent + "%'; } }; "
                + "if (document.documentElement) { applyZoom(); } "
                + "else { document.addEventListener('DOMContentLoaded', applyZoom, { once: true }); }"
                + " })();";
    }

    /**
     * Allows CI/ATF Debug to point at an already provisioned Playwright browser
     * without changing the business YAML or attempting an uncontrolled CDN download.
     */
    static java.util.Optional<Path> configuredExecutablePath() {
        String configured = System.getProperty(EXECUTABLE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(EXECUTABLE_ENVIRONMENT);
        }
        return configuredExecutablePath(configured);
    }

    static java.util.Optional<Path> configuredExecutablePath(String configured) {
        if (configured == null || configured.isBlank()) return java.util.Optional.empty();
        Path executable = Path.of(configured.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(executable)) {
            throw new IllegalArgumentException("Configured Playwright executable does not exist: " + executable);
        }
        return java.util.Optional.of(executable);
    }

    /** Starts an isolated consecutive action log for one visual validation state. */
    public PlaywrightActionSequence newActionSequence() {
        if (closed) throw new IllegalStateException("Playwright UI session is closed");
        return new PlaywrightActionSequence(actionTrace, actionTimeout, retryPolicy);
    }

    public void navigate(URI uri) {
        page.navigate(Objects.requireNonNull(uri, "uri").toString());
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try { networkInspector.close(); }
        finally {
            try { context.close(); }
        finally {
            try { browser.close(); }
            finally { playwright.close(); }
        }
        }
    }
}
