package atf.shop.tests.component.evidence;

import atf.core.http.protocol.ApiUris;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.shop.tests.support.ShopComponentTest;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.shop.evidence.ShopBrowserCaptureProvider;
import atf.ui.playwright.UiScreenValidation;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the Practice Shop browser adapter returns renderer-neutral PNG evidence. */
@ShopComponentTest
class ShopBrowserCaptureProviderTest {
    private static final byte[] ONE_PIXEL_PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Test
    void capturesThroughTheTypedBrowserProviderWithDeclaredProvenance() {
        AtomicReference<String> maskSelector = new AtomicReference<>();
        AtomicReference<Page.ScreenshotOptions> screenshotOptions = new AtomicReference<>();
        Locator sensitiveFields = (Locator) Proxy.newProxyInstance(Locator.class.getClassLoader(),
                new Class<?>[]{Locator.class}, (proxy, method, args) -> null);
        Page page = (Page) Proxy.newProxyInstance(Page.class.getClassLoader(),
                new Class<?>[]{Page.class}, (proxy, method, args) -> {
                    if ("locator".equals(method.getName())) {
                        maskSelector.set((String) args[0]);
                        return sensitiveFields;
                    }
                    if ("screenshot".equals(method.getName())) {
                        screenshotOptions.set((Page.ScreenshotOptions) args[0]);
                        return ONE_PIXEL_PNG;
                    }
                    return null;
                });
        var provider = new ShopBrowserCaptureProvider(page,
                UiScreenValidationConfiguration.defaults());

        var screenshot = provider.capture("final state",
                ApiUris.httpsUrl("example.test", "/checkout"));

        assertEquals(ReportEvidenceCaptureSource.BROWSER, provider.source());
        assertEquals("final state", screenshot.title());
        assertEquals("input, textarea, select, [contenteditable='true']", maskSelector.get());
        assertEquals(java.util.List.of(sensitiveFields), screenshotOptions.get().mask);
        assertEquals("#343a40", screenshotOptions.get().maskColor);
        assertTrue(UiScreenValidation.isValidPngPayload(
                screenshot.base64Png()));
    }
}
