package atf.ui.tests.support.selection;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** One driver/browser owner; every case receives a fresh context/page and its own event counters. */
public final class SelectContinuityBrowser implements AutoCloseable {
    private static final Duration ACTION_TIMEOUT = Duration.ofMillis(700);
    private final List<AtfConsoleEvent> trace = new ArrayList<>();
    private final PlaywrightUiSession session = PlaywrightUiSession.open(new UiBrowserConfiguration(
            URI.create("about:blank"), "chromium", true, ACTION_TIMEOUT, Duration.ofSeconds(3),
            1280, 800, 100, true, UiRetryPolicy.readOnly(3, Duration.ZERO)), trace::add);

    public SelectContinuityBrowser() {
        try {
            // Browser startup is fixture setup, outside every measured selection deadline.
            session.page().setContent("<select id='ready'><option>Ready</option></select>");
            var ready = session.page().locator("#ready");
            ready.evaluate("node => node.options.length"); ready.isVisible(); ready.isEnabled();
        } catch (RuntimeException | Error failure) {
            try { session.close(); } catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public SelectContinuityFixture open(String mode, String frame, boolean sensitive) throws Exception {
        return new SelectContinuityFixture(this, mode, frame, sensitive);
    }
    Page newPage() {
        Page page = session.page().context().browser().newPage();
        page.setDefaultTimeout(ACTION_TIMEOUT.toMillis()); page.setDefaultNavigationTimeout(3000);
        return page;
    }
    PlaywrightUiSession session() { return session; }
    List<AtfConsoleEvent> trace() { return trace; }
    @Override public void close() { session.close(); }
}
