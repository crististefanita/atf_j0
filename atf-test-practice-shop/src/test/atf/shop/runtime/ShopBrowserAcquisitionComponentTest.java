package atf.shop.runtime;

import atf.core.reporting.console.AtfConsoleSink;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.evidence.ShopBrowserCaptureProvider;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.tests.support.ShopComponentTest;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Uses the production acquisition stages and an actual browser; only the failure boundary is controlled. */
@ShopComponentTest
class ShopBrowserAcquisitionComponentTest {
    @ParameterizedTest @ValueSource(strings={"captures", "network"})
    void partialAcquisitionClosesTheNativeContextOnceAndKeepsItsCause(String stage) {
        var probe = new Probe(stage, new IllegalStateException("controlled " + stage), null);
        assertSame(probe.primary, assertThrows(IllegalStateException.class, probe::open));
        assertEquals(1, probe.releases, "The acquired browser is released exactly once after assembly failure");
        assertEquals(1, probe.nativeCloses.get(), "The actual Playwright context emitted one close event");
        assertTrue(probe.browser.page().isClosed());
        assertEquals(0, probe.primary.getSuppressed().length);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void runtimeAndErrorFailuresRetainTheDistinctCleanupError(boolean error) {
        Throwable primary = error ? new AssertionError("assembly") : new IllegalStateException("assembly");
        var cleanup = new AssertionError("cleanup"); var probe = new Probe("network", primary, cleanup);
        Throwable observed = error ? assertThrows(AssertionError.class, probe::open) : assertThrows(IllegalStateException.class, probe::open);
        assertSame(primary, observed); assertEquals(1, observed.getSuppressed().length);
        assertSame(cleanup, observed.getSuppressed()[0]);
        assertEquals(1, probe.releases); assertEquals(1, probe.nativeCloses.get());
    }
    @Test void theSameThrowableNeverSuppressesItself() {
        var shared = new AssertionError("same primary and cleanup"); var probe = new Probe("captures", shared, shared);
        assertSame(shared, assertThrows(AssertionError.class, probe::open));
        assertEquals(0, shared.getSuppressed().length); assertEquals(1, probe.nativeCloses.get());
    }
    @Test void successfulAssemblyTransfersBrowserOwnershipToTheCaller() {
        var probe = new Probe("none", null, null); var acquired = probe.open();
        try {
            assertSame(probe.browser, acquired.browser()); assertFalse(acquired.browser().page().isClosed());
            assertEquals(0, probe.releases); assertEquals(0, probe.nativeCloses.get());
            assertNotNull(acquired.captures()); assertNotNull(acquired.network());
        } finally { acquired.browser().close(); }
        acquired.browser().close();
        assertEquals(1, probe.nativeCloses.get(), "The caller closes the transferred context once, including repeated close");
        assertEquals(0, probe.releases, "Acquisition no longer owns a successfully returned browser");
    }
    @Test void failureBeforeBrowserAcquisitionHasNothingToRelease() {
        var probe = new Probe("browser", new IllegalStateException("open"), null);
        assertSame(probe.primary, assertThrows(IllegalStateException.class, probe::open));
        assertEquals(0, probe.releases); assertEquals(0, probe.nativeCloses.get()); assertNull(probe.browser);
    }

    private static final class Probe implements ShopBrowserRuntime.Factory {
        final String stage; final Throwable primary, cleanup; final AtomicInteger nativeCloses = new AtomicInteger();
        PlaywrightUiSession browser; int releases;
        Probe(String stage, Throwable primary, Throwable cleanup) { this.stage=stage;this.primary=primary;this.cleanup=cleanup; }
        ShopBrowserRuntime open() { return ShopBrowserRuntime.open(ShopConfigurationLoader.load(), AtfConsoleSink.noOp(), this); }
        @Override public PlaywrightUiSession browser(ShopConfiguration configuration, AtfConsoleSink console) {
            failAt("browser"); browser = ShopBrowserRuntime.Factory.super.browser(configuration, console);
            browser.page().context().onClose(ignored -> nativeCloses.incrementAndGet()); return browser;
        }
        @Override public ShopBrowserCaptureProvider captures(PlaywrightUiSession browser) {
            failAt("captures"); return ShopBrowserRuntime.Factory.super.captures(browser);
        }
        @Override public ShopApiNetworkJournal network(ShopConfiguration configuration, PlaywrightUiSession browser) {
            var network = ShopBrowserRuntime.Factory.super.network(configuration, browser); failAt("network"); return network;
        }
        @Override public void close(PlaywrightUiSession browser) {
            releases++; ShopBrowserRuntime.Factory.super.close(browser); raise(cleanup);
        }
        private void failAt(String point) { if (stage.equals(point)) raise(primary); }
        private static void raise(Throwable failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
        }
    }
}
