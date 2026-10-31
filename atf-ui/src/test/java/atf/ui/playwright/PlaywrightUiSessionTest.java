package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import org.junit.jupiter.api.Test;
import atf.core.reporting.console.AtfConsoleSink;
import atf.ui.config.UiNetworkInspectionConfiguration;
import com.microsoft.playwright.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

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
    static Stream<org.junit.jupiter.params.provider.Arguments> acquisitionFailures() {
        return Stream.of("driver", "type", "launch", "context", "timeout", "page", "attach")
                .flatMap(stage -> Stream.of(new IllegalStateException(stage), new AssertionError(stage))
                        .map(failure -> org.junit.jupiter.params.provider.Arguments.of(stage, failure)));
    }

    @ParameterizedTest @MethodSource("acquisitionFailures")
    void acquisitionKeepsTheExactCauseAndClosesOnlyAcquiredOwners(String stage, Throwable primary) {
        var probe = new DriverProbe(stage, primary);
        var contextClose = new AssertionError("context close");
        var browserClose = new IllegalStateException("browser close");
        probe.closeFailures.put("context", contextClose);
        probe.closeFailures.put("browser", browserClose);
        probe.closeFailures.put("driver", primary); // Self-suppression must not abort later cleanup.
        Throwable actual = assertThrows(primary.getClass(), () -> PlaywrightUiSession.open(configuration(), AtfConsoleSink.noOp(), probe));
        assertSame(primary, actual);
        List<String> expected = switch (stage) {
            case "driver" -> List.of();
            case "type", "launch" -> List.of("driver");
            case "context" -> List.of("browser", "driver");
            case "timeout", "page" -> List.of("context", "browser", "driver");
            case "attach" -> List.of("offRequest", "context", "browser", "driver");
            default -> throw new AssertionError(stage);
        };
        assertEquals(expected, probe.closed, "Only successfully returned resources transfer ownership");
        var secondary = new ArrayList<Throwable>();
        if (expected.contains("context")) secondary.add(contextClose);
        if (expected.contains("browser")) secondary.add(browserClose);
        assertArrayEquals(secondary.toArray(), actual.getSuppressed(), "Every distinct cleanup cause remains in release order");
    }

    @Test
    void closeAttemptsEveryOwnerOnceAndKeepsTheFirstFailureIncludingErrors() {
        var probe = new DriverProbe(null, null);
        var first = new AssertionError("inspector detach");
        var context = new IllegalStateException("context close");
        var driver = new AssertionError("driver close");
        probe.closeFailures.put("offRequest", first);
        probe.closeFailures.put("context", context);
        probe.closeFailures.put("browser", first);
        probe.closeFailures.put("driver", driver);
        var session = PlaywrightUiSession.open(configuration(), AtfConsoleSink.noOp(), probe);
        assertSame(first, assertThrows(AssertionError.class, session::close));
        assertArrayEquals(new Throwable[]{context, driver}, first.getSuppressed());
        assertEquals(List.of("offRequest", "offRequestFinished", "offRequestFailed", "context", "browser", "driver"), probe.closed);
        session.close();
        assertEquals(6, probe.closed.size(), "A failed close still consumes ownership exactly once");
        assertThrows(IllegalStateException.class, session::newActionSequence);
    }

    @Test
    void successfulCloseUsesTheSameReleaseOrderAndIsIdempotent() {
        var probe = new DriverProbe(null, null);
        var session = PlaywrightUiSession.open(configuration(), AtfConsoleSink.noOp(), probe);
        session.close(); session.close();
        assertEquals(List.of("offRequest", "offRequestFinished", "offRequestFailed", "context", "browser", "driver"), probe.closed);
    }

    private static UiBrowserConfiguration configuration() {
        var base = UiBrowserConfiguration.defaults();
        return new UiBrowserConfiguration(base.baseUrl(), base.browser(), base.headless(), base.timeout(), base.navigationTimeout(),
                base.viewportWidth(), base.viewportHeight(), base.zoomPercent(), base.screenshotOnFailure(), base.retryPolicy(),
                UiNetworkInspectionConfiguration.forHosts(Set.of("fixture.local")));
    }

    private static final class DriverProbe implements Supplier<Playwright> {
        final String stage; final Throwable primary;
        final Map<String, Throwable> closeFailures = new HashMap<>();
        final List<String> closed = new ArrayList<>();
        DriverProbe(String stage, Throwable primary) { this.stage = stage; this.primary = primary; }
        @Override public Playwright get() { fail("driver"); return proxy(Playwright.class, "driver"); }

        private <T> T proxy(Class<T> type, String owner) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
                return switch (method.getName()) {
                    case "chromium" -> { fail("type"); yield proxy(BrowserType.class, "type"); }
                    case "launch" -> { fail("launch"); yield proxy(Browser.class, "browser"); }
                    case "newContext" -> { fail("context"); yield proxy(BrowserContext.class, "context"); }
                    case "setDefaultTimeout" -> { fail("timeout"); yield null; }
                    case "newPage" -> { fail("page"); yield proxy(Page.class, "page"); }
                    case "onRequest" -> { fail("attach"); yield null; }
                    case "close" -> { close(owner); yield null; }
                    case "offRequest", "offRequestFinished", "offRequestFailed" -> { close(method.getName()); yield null; }
                    case "setDefaultNavigationTimeout", "onRequestFinished", "onRequestFailed" -> null;
                    default -> throw new AssertionError("Unexpected native call: " + owner + "." + method.getName());
                };
            }));
        }
        private void fail(String current) { if (current.equals(stage)) rethrow(primary); }
        private void close(String owner) { closed.add(owner); rethrow(closeFailures.get(owner)); }
        private static void rethrow(Throwable failure) {
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
        }
    }
}
