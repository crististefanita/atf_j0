package atf.ui.playwright;

import atf.core.reporting.console.AtfConsoleSink;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.config.geometry.UiViewportScale;
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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.function.Supplier;

/** Scoped Playwright adapter. One instance owns exactly one browser context. */
public final class PlaywrightUiSession implements AutoCloseable {
    private static final String EXECUTABLE_PROPERTY = "atf.ui.playwright.executablePath";
    private static final String EXECUTABLE_ENVIRONMENT = "ATF_UI_PLAYWRIGHT_EXECUTABLE_PATH";
    private final OwnedResources resources;
    private final Page page;
    private final UiActionTrace actionTrace;
    private final Duration actionTimeout;
    private final UiRetryPolicy retryPolicy;
    private final UiNetworkInspector networkInspector;
    private boolean closed;

    private PlaywrightUiSession(OwnedResources resources, Page page, UiActionTrace actionTrace,
                                Duration actionTimeout, UiRetryPolicy retryPolicy,
                                UiNetworkInspector networkInspector) {
        this.resources = resources;
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
        return open(configuration, console, Playwright::create);
    }

    /** Package-local acquisition seam; production always creates the native driver. */
    static PlaywrightUiSession open(UiBrowserConfiguration configuration, AtfConsoleSink console,
                                    Supplier<Playwright> driverFactory) {
        Objects.requireNonNull(configuration, "configuration");
        UiActionTrace trace = UiActionTrace.console(Objects.requireNonNull(console, "console"));
        UiViewportScale geometry = new UiViewportScale(configuration);
        var resources = new OwnedResources();
        try {
            Playwright playwright = Objects.requireNonNull(driverFactory.get(), "playwright");
            resources.add(playwright::close);
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
            resources.add(browser::close);
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(new ViewportSize(geometry.layoutWidth(), geometry.layoutHeight()))
                    .setDeviceScaleFactor(geometry.deviceScaleFactor()));
            resources.add(context::close);
            context.setDefaultTimeout(configuration.timeout().toMillis());
            context.setDefaultNavigationTimeout(configuration.navigationTimeout().toMillis());
            Page page = context.newPage();
            UiNetworkInspector networkInspector =
                    new UiNetworkInspector(configuration.networkInspection());
            resources.add(networkInspector::close);
            networkInspector.attach(page);
            return new PlaywrightUiSession(resources, page, trace,
                    configuration.timeout(), configuration.retryPolicy(), networkInspector);
        } catch (RuntimeException | Error primary) {
            resources.release(primary);
            throw primary;
        }
    }

    public Page page() { return page; }

    /** Generic opt-in network observer for consumers that need UI/API correlation. */
    public UiNetworkInspector networkInspector() { return networkInspector; }

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
        resources.release(null);
    }

    /** Register immediately after acquisition; context owns its pages. Release continues after every distinct failure. */
    private static final class OwnedResources {
        private final ArrayDeque<Runnable> closers = new ArrayDeque<>();
        private boolean released;

        void add(Runnable close) { closers.push(close); }

        void release(Throwable primary) {
            if (released) return;
            released = true;
            var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
            if (primary != null) seen.add(primary);
            Throwable failure = primary;
            while (!closers.isEmpty()) {
                try { closers.pop().run(); }
                catch (RuntimeException | Error secondary) {
                    if (!seen.add(secondary)) continue;
                    if (failure == null) failure = secondary;
                    else failure.addSuppressed(secondary);
                }
            }
            if (primary == null) {
                if (failure instanceof RuntimeException runtime) throw runtime;
                if (failure instanceof Error error) throw error;
            }
        }
    }
}
