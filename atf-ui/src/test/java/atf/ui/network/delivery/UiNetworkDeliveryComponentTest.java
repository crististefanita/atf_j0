package atf.ui.network.delivery;

import atf.testkit.AtfComponentTest;
import atf.ui.config.*;
import atf.ui.network.*;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiNetworkDeliveryComponentTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void subscriberOrderCannotBlockHealthyDeliveryOrChangeRawRetention(boolean brokenFirst) {
        var inspector = inspector(); var healthy = new ArrayList<UiNetworkObservation>();
        var failure = new IllegalStateException("controlled subscriber failure");
        java.util.function.Consumer<UiNetworkObservation> bad = value -> { throw failure; };
        inspector.subscribe(brokenFirst ? bad : healthy::add); inspector.subscribe(brokenFirst ? healthy::add : bad);
        var observed = inspector.capture(raw()).orElseThrow();
        assertEquals(List.of(observed), healthy); assertEquals(1, inspector.observations().size());
        assertTrue(inspector.statistics().complete()); assertEquals(0, inspector.statistics().dropped());
        assertSame(failure, inspector.deliveryFailures().getFirst().cause());
        assertEquals(brokenFirst ? 0 : 1, inspector.deliveryFailures().getFirst().subscriber());
        assertFalse(observed.toString().contains("PRIVATE_FIXTURE_PASSWORD"));
        assertThrows(UnsupportedOperationException.class, () -> inspector.deliveryFailures().clear());
    }

    @Test void everyRuntimeCauseRemainsAvailableAndClosingDuringDeliveryCannotRemoveTheSnapshot() {
        var inspector = inspector(); var healthy = new AtomicInteger();
        var first = new IllegalStateException("first"); var second = new IllegalArgumentException("second");
        inspector.subscribe(value -> { throw first; }); inspector.subscribe(value -> inspector.close());
        inspector.subscribe(value -> { throw second; }); inspector.subscribe(value -> healthy.incrementAndGet());
        assertTrue(inspector.capture(raw()).isPresent()); assertEquals(1, healthy.get());
        assertEquals(List.of(first, second), inspector.deliveryFailures().stream().map(UiNetworkDeliveryFailure::cause).toList());
        assertEquals(1, inspector.observations().size()); assertEquals(0, inspector.statistics().dropped());
    }

    @Test void aFatalVmOrLinkageFailureIsNotReportedAsSuccessfulDelivery() {
        var inspector = inspector(); var fatal = new LinkageError("controlled fatal failure"); var healthy = new AtomicInteger();
        inspector.subscribe(value -> { throw fatal; }); inspector.subscribe(value -> healthy.incrementAndGet());
        assertSame(fatal, assertThrows(LinkageError.class, () -> inspector.capture(raw())));
        assertEquals(0, healthy.get()); assertEquals(1, inspector.observations().size());
        assertTrue(inspector.deliveryFailures().isEmpty());
    }

    @Test void theRealBrowserHookPreservesTheHealthySubscriberAndTheActionResult() {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults()); var inspector = inspector()) {
            var page = browser.page(); var healthy = new AtomicInteger();
            page.route("https://delivery.fixture.test/**", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setContentType("text/html").setBody("<html><body>owned fixture</body></html>")));
            inspector.attach(page, value -> { throw new IllegalStateException("controlled delivery failure"); });
            inspector.subscribe(value -> healthy.incrementAndGet());
            page.navigate("https://delivery.fixture.test/");
            assertEquals(200, ((Number) page.evaluate("async () => (await fetch('/read')).status")).intValue());
            page.waitForCondition(() -> inspector.observations().size() == 1);
            assertEquals(1, healthy.get()); assertEquals(1, inspector.deliveryFailures().size());
            assertTrue(inspector.statistics().complete()); assertEquals(0, inspector.statistics().dropped());
        }
    }
    private static UiNetworkInspector inspector() {
        return new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(Set.of("delivery.fixture.test"))
                .withCapturePolicy(UiNetworkCapturePolicy.asynchronous()));
    }
    private static UiNetworkObservation raw() {
        return new UiNetworkObservation(1, Instant.EPOCH, "GET", "https://delivery.fixture.test/read", "/read", 200,
                Map.of(), Map.of(), "", "{\"password\":\"PRIVATE_FIXTURE_PASSWORD\"}", "none", "none", "fixture", "controlled", "untrusted", true, "", UiNetworkCapturePolicy.ResourceKind.FETCH);
    }
}
