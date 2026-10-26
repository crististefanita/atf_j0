package atf.ui.config;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest

class UiBrowserConfigurationTest {
    @Test
    void defaultsAreDeterministicAndVendorNeutral() {
        var configuration = UiBrowserConfiguration.defaults();
        assertEquals("chromium", configuration.browser());
        assertEquals(1440, configuration.viewportWidth());
        assertEquals(UiBrowserConfiguration.DEFAULT_ZOOM_PERCENT, configuration.zoomPercent());
        assertEquals(Duration.ofSeconds(10), configuration.timeout());
        assertEquals(true, configuration.screenshotOnFailure());
        assertEquals(UiRetryPolicy.none(), configuration.retryPolicy());
    }

    @Test
    void rejectsUnsupportedBrowserAndNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new UiBrowserConfiguration(
                URI.create("about:blank"), "safari", true, Duration.ofSeconds(1),
                Duration.ofSeconds(1), 100, 100, true));
        assertThrows(IllegalArgumentException.class, () -> new UiBrowserConfiguration(
                URI.create("about:blank"), "chromium", true, Duration.ZERO,
                Duration.ofSeconds(1), 100, 100, true));
        assertThrows(IllegalArgumentException.class, () -> new UiBrowserConfiguration(
                URI.create("about:blank"), "chromium", true, Duration.ofSeconds(1),
                Duration.ofSeconds(1), 100, 100, 10, true));
    }
}
