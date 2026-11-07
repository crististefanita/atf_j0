package atf.ui.playwright;

import atf.core.reporting.console.AtfConsoleSink;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.geometry.UiViewportScale;
import atf.ui.network.UiNetworkInspector;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.net.URI;

/** A real UI session whose resources belong to the Reporting test worker. */
public final class ReportingBrowserSession implements AutoCloseable {
    private final Playwright driver;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final UiNetworkInspector network;
    private final UiActionTrace trace;
    private final UiBrowserConfiguration configuration;

    private ReportingBrowserSession(Playwright driver, Browser browser, BrowserContext context, Page page,
                                    UiNetworkInspector network, UiActionTrace trace, UiBrowserConfiguration configuration) {
        this.driver = driver;
        this.browser = browser;
        this.context = context;
        this.page = page;
        this.network = network;
        this.trace = trace;
        this.configuration = configuration;
    }

    public static ReportingBrowserSession open(UiBrowserConfiguration configuration) {
        return open(configuration, AtfConsoleSink.noOp());
    }

    public static ReportingBrowserSession open(UiBrowserConfiguration configuration, AtfConsoleSink console) {
        var trace = UiActionTrace.console(console);
        var geometry = new UiViewportScale(configuration);
        var options = new BrowserType.LaunchOptions().setHeadless(configuration.headless());
        PlaywrightUiSession.configuredExecutablePath().ifPresent(options::setExecutablePath);
        Playwright driver = BrowserResources.create(Playwright::create);
        Browser browser = null;
        BrowserContext context = null;
        UiNetworkInspector network = null;
        try {
            browser = BrowserResources.launch(driver, configuration.browser(), options);
            context = BrowserResources.context(browser, new Browser.NewContextOptions()
                    .setViewportSize(geometry.layoutWidth(), geometry.layoutHeight())
                    .setDeviceScaleFactor(geometry.deviceScaleFactor()));
            context.setDefaultTimeout(configuration.timeout().toMillis());
            context.setDefaultNavigationTimeout(configuration.navigationTimeout().toMillis());
            Page page = BrowserResources.newPage(context);
            network = new UiNetworkInspector(configuration.networkInspection());
            network.attach(page);
            return new ReportingBrowserSession(driver, browser, context, page, network, trace, configuration);
        } catch (RuntimeException | Error failure) {
            BrowserResources.recordFailure(failure);
            try { BrowserResources.closeAll(network, context, browser, driver); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public Page page() { return page; }

    public void navigate(URI uri) { page.navigate(uri.toString()); }

    public PlaywrightActionSequence newActionSequence() {
        return new PlaywrightActionSequence(trace, configuration.timeout(), configuration.retryPolicy());
    }

    @Override public void close() { BrowserResources.closeAll(network, context, browser, driver); }
}
