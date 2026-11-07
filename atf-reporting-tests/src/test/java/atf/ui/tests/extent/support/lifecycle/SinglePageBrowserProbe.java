package atf.ui.tests.extent.support.lifecycle;

import atf.reporting.browser.lifecycle.BrowserProbe;
import com.microsoft.playwright.*;
import java.nio.file.Path;

/** Explicit context ownership, shared by the SDK control and the real profile browser oracle. */
public abstract class SinglePageBrowserProbe implements BrowserProbe {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private Path directory;

    @Override public final void open(Path directory) {
        this.directory = directory;
        playwright = BrowserResources.create(Playwright::create);
        browser = BrowserResources.launch(playwright);
        context = BrowserResources.context(browser, new Browser.NewContextOptions().setViewportSize(1100, 900));
        page = BrowserResources.newPage(context);
        page.setDefaultTimeout(5_000);
        page.setDefaultNavigationTimeout(30_000);
    }

    protected final Page page() { return page; }
    protected final Path directory() { return directory; }

    @Override public final void close() {
        BrowserResources.closeAll(context, browser, playwright);
    }
}
