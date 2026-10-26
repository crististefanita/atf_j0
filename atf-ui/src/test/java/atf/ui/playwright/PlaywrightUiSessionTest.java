package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void configuredZoomAppliesToTheTopDocumentWithoutCompoundingInsideFrames() {
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofSeconds(5), Duration.ofSeconds(10), 1440, 1000, 70, true);
        try (var session = PlaywrightUiSession.open(configuration)) {
            var page = session.page();
            page.navigate("data:text/html;base64," + Base64.getEncoder()
                    .encodeToString(EMBEDDED_CONTROL_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
            var control = page.frameLocator("#embedded").locator("button");
            control.waitFor();
            assertEquals("0.7", page.evaluate("getComputedStyle(document.documentElement).zoom"));
            var embedded = page.frames().stream().filter(frame -> frame.parentFrame() != null)
                    .findFirst().orElseThrow();
            assertEquals("1", embedded.evaluate("getComputedStyle(document.documentElement).zoom"),
                    "the embedding document already scales the frame; its own zoom must remain native");
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
    void zoomInitScriptIsSafeBeforeDocumentElementExists() {
        String script = PlaywrightUiSession.zoomInitScript(70);

        assertTrue(script.contains("if (document.documentElement)"));
        assertTrue(script.contains("DOMContentLoaded"));
        assertTrue(script.contains("document.documentElement.style.zoom = '70%'") );
        assertFalse(script.startsWith("document.documentElement.style.zoom"));
    }

    @Test
    void executableOverrideIsOptInAndDoesNotBecomeBusinessConfiguration() {
        assertTrue(PlaywrightUiSession.configuredExecutablePath(" ").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> PlaywrightUiSession.configuredExecutablePath("missing-playwright-browser.exe"));
    }
}
