package atf.ui.network;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.identity.UiRequestIdentity;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class UiNetworkRequestIdentityComponentTest {
    @Test void responseAndFailureKeepTheirStartAcrossSanitizationAndScopes() {
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage();
             var first = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("start.test")));
             var sibling = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("start.test")))) {
            page.setDefaultTimeout(3000);
            page.route("http://start.test/**", route -> {
                if (route.request().url().endsWith("/failed")) route.abort();
                else route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                        .setBody("{\"password\":\"raw-request-secret\",\"ok\":true}"));
            });
            page.navigate("http://start.test/");
            first.attach(page); sibling.attach(page);
            var marker = first.requestMarker();
            var otherMarker = sibling.requestMarker();
            page.evaluate("() => fetch('/success').then(r=>r.text())");
            page.evaluate("() => fetch('/failed').catch(()=>null)");
            page.waitForCondition(() -> first.observations().size() == 2);
            for (var item : first.observations()) {
                assertTrue(item.requestIdentity().orElseThrow().isAfter(marker));
                assertFalse(item.requestIdentity().orElseThrow().isAfter(otherMarker), "Another capture scope cannot satisfy this marker");
                var safe = first.capture(item.withService("consumer alias")).orElseThrow();
                assertEquals(item.requestIdentity(), safe.requestIdentity());
                assertFalse(safe.responseBody().contains("raw-request-secret"));
            }
            assertTrue(first.observations().stream().anyMatch(UiNetworkObservation::requestFailed));
            first.close();
            assertFalse(first.observations().getFirst().requestIdentity().orElseThrow().isAfter(first.requestMarker()));
            assertFalse(first.observations().getFirst().requestIdentity().orElseThrow().isAfter(sibling.requestMarker()));
        }
    }

    @Test void attachingAfterARequestStartedNeverInventsItsStartWhenItFinishes() {
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage();
             var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("start.test")))) {
            page.setDefaultTimeout(3000);
            var held = new AtomicReference<Route>();
            page.route("http://start.test/**", route -> {
                if (route.request().url().endsWith("/late")) held.set(route);
                else route.fulfill(new Route.FulfillOptions().setBody("<html>local fixture</html>"));
            });
            page.navigate("http://start.test/");
            page.evaluate("() => { fetch('/late').then(()=>window.done=true); }");
            page.waitForCondition(() -> held.get() != null);
            inspector.attach(page);
            var marker = inspector.requestMarker();
            held.get().fulfill(new Route.FulfillOptions().setContentType("application/json").setBody("{}"));
            page.waitForCondition(() -> !inspector.observations().isEmpty());
            var identity = inspector.observations().getFirst().requestIdentity();
            assertTrue(identity.isEmpty());
            assertFalse(identity.filter(start -> start.isAfter(marker)).isPresent());
        }
    }

    @Test void compatibilityObservationsStayUncorrelatedWithoutInventingBrowserEvents() {
        var observation = new UiNetworkObservation(1, Instant.EPOCH, "POST", "http://start.test/order", "/order",
                201, Map.of(), Map.of(), "{}", "{}", "none", "none", "start.test", "fixture", "POST /order", true);
        assertTrue(observation.requestIdentity().isEmpty());
        assertFalse(observation.requestIdentity().filter(start -> start.isAfter(new UiRequestIdentity(java.util.UUID.randomUUID(), 0))).isPresent());
    }
}
