package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class PlaywrightUiSessionTest {
    private static final String EMBEDDED_CONTROL_DOCUMENT = """
            <!doctype html><html><body>
              <iframe id="embedded" srcdoc="<button onclick=&quot;this.textContent='clicked'&quot;>Click me</button>"></iframe>
            </body></html>
            """;

    @Test
    void configuredContextScaleKeepsNativePointerCoordinatesWithoutInjectingCss() {
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofSeconds(5), Duration.ofSeconds(10), 1440, 1000, 70, true);
        try (var session = PlaywrightUiSession.open(configuration)) {
            var page = session.page();
            page.navigate("data:text/html;base64," + Base64.getEncoder()
                    .encodeToString(EMBEDDED_CONTROL_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
            var control = page.frameLocator("#embedded").locator("button");
            control.waitFor();
            assertEquals("1", page.evaluate("getComputedStyle(document.documentElement).zoom"));
            assertEquals(.7, ((Number) page.evaluate("devicePixelRatio")).doubleValue(), .000001);
            assertEquals(2057, page.viewportSize().width);
            var embedded = page.frames().stream().filter(frame -> frame.parentFrame() != null)
                    .findFirst().orElseThrow();
            assertEquals("1", embedded.evaluate("getComputedStyle(document.documentElement).zoom"),
                    "the context pixel density applies once; neither document receives an injected CSS zoom");
            control.click();
            assertEquals("clicked", control.innerText());
        }
    }

    @Test
    void nativeScalePreservesEmbeddedPointerCoordinates() {
        try (var session = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = session.page();
            page.navigate("data:text/html;base64," + Base64.getEncoder()
                    .encodeToString(EMBEDDED_CONTROL_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
            var control = page.frameLocator("#embedded").locator("button");
            control.click();
            assertEquals("clicked", control.innerText());
        }
    }

    @Test
    void scalingIsReadyBeforeNavigationWithoutAnInjectedRootStyle() {
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofSeconds(5), Duration.ofSeconds(10), 1440, 1000, 70, true);
        try (var session = PlaywrightUiSession.open(configuration)) {
            var errors = new java.util.ArrayList<String>();
            session.page().onPageError(errors::add);
            session.page().navigate("data:text/html,<html><body>document</body></html>");
            assertEquals("", session.page().evaluate("document.documentElement.style.cssText"));
            assertEquals(.7, ((Number) session.page().evaluate("devicePixelRatio")).doubleValue(), .000001);
            assertTrue(errors.isEmpty());
        }
    }

    @Test
    void executableOverrideIsOptInAndDoesNotBecomeBusinessConfiguration() {
        assertTrue(PlaywrightUiSession.configuredExecutablePath(" ").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> PlaywrightUiSession.configuredExecutablePath("missing-playwright-browser.exe"));
    }
}
