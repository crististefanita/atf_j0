package atf.shop.runtime;

import atf.core.reporting.console.AtfConsoleSink;
import atf.shop.config.ShopConfiguration;
import atf.shop.evidence.ShopBrowserCaptureProvider;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import java.util.Objects;

/** Runtime acquisition with explicit policy/sink; the caller owns the returned browser. */
public record ShopBrowserRuntime(PlaywrightUiSession browser, ShopBrowserCaptureProvider captures,
                                  ShopApiNetworkJournal network) {
    public ShopBrowserRuntime {
        Objects.requireNonNull(browser); Objects.requireNonNull(captures); Objects.requireNonNull(network);
    }
    public static ShopBrowserRuntime open(ShopConfiguration configuration, AtfConsoleSink console) {
        Objects.requireNonNull(configuration, "configuration"); Objects.requireNonNull(console, "console");
        var browser = PlaywrightUiSession.open(configuration.browser(), console);
        try {
            var captures = new ShopBrowserCaptureProvider(browser.page(), UiScreenValidationConfiguration.defaults());
            var network = new ShopApiNetworkJournal(configuration.validationProfile(), configuration.api().evidenceBinding());
            network.attach(browser.page());
            return new ShopBrowserRuntime(browser, captures, network);
        } catch (RuntimeException | Error primary) {
            try { browser.close(); } catch (RuntimeException | Error closing) { if (closing != primary) primary.addSuppressed(closing); }
            throw primary;
        }
    }
}
