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
    private static final Factory PRODUCTION = new Factory() { };
    public ShopBrowserRuntime {
        Objects.requireNonNull(browser); Objects.requireNonNull(captures); Objects.requireNonNull(network);
    }
    public static ShopBrowserRuntime open(ShopConfiguration configuration, AtfConsoleSink console) {
        return open(configuration, console, PRODUCTION);
    }
    static ShopBrowserRuntime open(ShopConfiguration configuration, AtfConsoleSink console, Factory factory) {
        Objects.requireNonNull(configuration, "configuration"); Objects.requireNonNull(console, "console");
        Objects.requireNonNull(factory, "factory");
        var browser = Objects.requireNonNull(factory.browser(configuration, console), "acquired browser");
        try {
            var captures = factory.captures(browser);
            return new ShopBrowserRuntime(browser, captures, factory.network(configuration, browser));
        } catch (RuntimeException | Error primary) {
            try { factory.close(browser); }
            catch (RuntimeException | Error closing) { if (closing != primary) primary.addSuppressed(closing); }
            throw primary;
        }
    }

    /** Concrete acquisition stages; package-private so failure injection does not expand the public runtime API. */
    interface Factory {
        default PlaywrightUiSession browser(ShopConfiguration configuration, AtfConsoleSink console) {
            return PlaywrightUiSession.open(configuration.browser(), console);
        }
        default ShopBrowserCaptureProvider captures(PlaywrightUiSession browser) {
            return new ShopBrowserCaptureProvider(browser.page(), UiScreenValidationConfiguration.defaults());
        }
        default ShopApiNetworkJournal network(ShopConfiguration configuration, PlaywrightUiSession browser) {
            var network = new ShopApiNetworkJournal(configuration.validationProfile(), configuration.api().evidenceBinding());
            network.attach(browser.page(), configuration.api().endpoint()); return network;
        }
        default void close(PlaywrightUiSession browser) { browser.close(); }
    }
}
