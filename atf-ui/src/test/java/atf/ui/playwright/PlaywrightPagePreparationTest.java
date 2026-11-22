package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Native entry points borrow one setup deadline; no wall-clock timing assertion or browser scheduling is involved. */
@AtfComponentTest
class PlaywrightPagePreparationTest {
    @Test void navigationAttachmentAndCaptureBorrowOneDeadline() {
        var probe = new Probe(4000, 500, 499);
        var prepared = probe.preparation.navigate("http://fixture.invalid/start");
        assertEquals(List.of(5000d, 1000d, 500d), probe.timeouts);
        assertEquals(4999, prepared.elapsedMillis());
        assertEquals("http://fixture.invalid/start", prepared.url());
        assertEquals(List.of("navigate", "attach", "capture"), probe.calls);
    }

    @Test void replacingContentUsesTheSameBoundedPipeline() {
        var probe = new Probe(1200, 300, 200);
        probe.preparation.setContent("<button>fixture</button>");
        assertEquals(List.of(5000d, 3800d, 3500d), probe.timeouts);
        assertEquals(List.of("setContent", "attach", "capture"), probe.calls);
    }

    @Test void anExpiredLoadNeverStartsAttachmentOrCapture() {
        var probe = new Probe(5000, 0, 0);
        assertThrows(IllegalStateException.class, () -> probe.preparation.navigate("http://fixture.invalid/start"));
        assertEquals(List.of("navigate"), probe.calls);
    }

    @Test void attachmentCannotGrantANewBudgetToCapture() {
        var probe = new Probe(4500, 500, 0);
        assertThrows(IllegalStateException.class, () -> probe.preparation.navigate("http://fixture.invalid/start"));
        assertEquals(List.of("navigate", "attach"), probe.calls);
    }

    @Test void aCaptureReturningAtTheDeadlineCannotAuthorizeInput() {
        var probe = new Probe(4000, 500, 500);
        var failure = assertThrows(IllegalStateException.class, () -> probe.preparation.navigate("http://fixture.invalid/start"));
        assertTrue(failure.getMessage().contains("5000 ms navigation budget"));
        assertEquals(List.of("navigate", "attach", "capture"), probe.calls);
    }

    @Test void captureFailureRetainsIdentityAndTheReceiptDoesNotExposeMutablePngBytes() {
        var refused = new Probe(0, 0, 0);
        var primary = new IllegalStateException("native capture refused");
        refused.captureFailure = primary;
        assertSame(primary, assertThrows(IllegalStateException.class,
                () -> refused.preparation.navigate("http://fixture.invalid/start")).getCause());
        var success = new Probe(0, 0, 0);
        var prepared = success.preparation.navigate("http://fixture.invalid/start");
        success.png[0] = 9; prepared.viewportPng()[1] = 9;
        assertArrayEquals(new byte[]{1, 2, 3}, prepared.viewportPng());
    }

    @Test void unboundedAndNegativePreparationBudgetsAreRejected() {
        var probe = new Probe(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new PlaywrightPagePreparation(probe.page, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PlaywrightPagePreparation(probe.page, Duration.ofMillis(-1)));
    }

    private static final class Probe {
        final AtomicLong ticks = new AtomicLong();
        final List<Double> timeouts = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        final byte[] png = {1, 2, 3};
        final Page page;
        final PlaywrightPagePreparation preparation;
        RuntimeException captureFailure;
        Probe(long load, long attachment, long capture) {
            Locator root = (Locator) Proxy.newProxyInstance(Locator.class.getClassLoader(), new Class<?>[]{Locator.class}, (proxy, method, args) -> {
                assertEquals("waitFor", method.getName());
                var options = (Locator.WaitForOptions) args[0];
                assertEquals(WaitForSelectorState.ATTACHED, options.state);
                calls.add("attach"); timeouts.add(options.timeout); advance(attachment); return null;
            });
            page = (Page) Proxy.newProxyInstance(Page.class.getClassLoader(), new Class<?>[]{Page.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "navigate" -> { calls.add("navigate"); timeouts.add(((Page.NavigateOptions) args[1]).timeout); advance(load); yield null; }
                    case "setContent" -> { calls.add("setContent"); timeouts.add(((Page.SetContentOptions) args[1]).timeout); advance(load); yield null; }
                    case "locator" -> { assertEquals(":root", args[0]); yield root; }
                    case "url" -> "http://fixture.invalid/start";
                    case "screenshot" -> {
                        var options = (Page.ScreenshotOptions) args[0];
                        assertEquals(ScreenshotType.PNG, options.type); assertEquals(false, options.fullPage);
                        assertEquals(ScreenshotAnimations.ALLOW, options.animations);
                        calls.add("capture"); timeouts.add(options.timeout); advance(capture);
                        if (captureFailure != null) throw captureFailure;
                        yield png;
                    }
                    default -> throw new AssertionError("Preparation must not perform target input/mutation: " + method.getName());
                };
            });
            preparation = new PlaywrightPagePreparation(page, Duration.ofSeconds(5), ticks::get);
        }
        void advance(long millis) { ticks.addAndGet(Duration.ofMillis(millis).toNanos()); }
    }
}
