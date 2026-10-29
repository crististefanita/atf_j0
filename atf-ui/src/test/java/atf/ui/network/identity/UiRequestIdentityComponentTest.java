package atf.ui.network.identity;

import atf.testkit.AtfComponentTest;
import atf.ui.config.*;
import atf.ui.network.*;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiRequestIdentityComponentTest {
    @Test void receiptOrderNeverFabricatesAStartAndForeignScopesCannotSatisfyAMarker() {
        var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(Set.of("identity.fixture.test")));
        var marker = inspector.requestMarker(); var started = inspector.requestStarted();
        var raw = new UiNetworkObservation(900, Instant.EPOCH, "GET", "https://identity.fixture.test/read", "/read", 200,
                Map.of(), Map.of(), "", "{}", "none", "none", "fixture", "controlled", "untrusted", true);
        assertTrue(inspector.capture(raw).orElseThrow().requestIdentity().isEmpty());
        var copy = inspector.capture(raw.withRequestIdentity(started).withService("owned-alias")).orElseThrow();
        assertEquals(started, copy.requestIdentity().orElseThrow()); assertTrue(started.isAfter(marker));
        assertFalse(started.isAfter(new UiRequestIdentity(UUID.randomUUID(), 0)));
        assertFalse(started.isAfter(inspector.requestMarker()));
    }

    @Test void theBrowserStartSurvivesCaptureAndTheNextFenceExcludesTheEarlierRequest() {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
             var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(Set.of("identity.fixture.test"))
                     .withCapturePolicy(UiNetworkCapturePolicy.asynchronous()))) {
            var page = browser.page(); page.route("https://identity.fixture.test/**", route -> route.fulfill(
                    new com.microsoft.playwright.Route.FulfillOptions().setContentType("text/html").setBody("<p>owned</p>")));
            inspector.attach(page); page.navigate("https://identity.fixture.test/");
            var before = inspector.requestMarker(); page.evaluate("async () => (await fetch('/read')).text()");
            page.waitForCondition(() -> inspector.observations().size() == 1);
            var start = inspector.observations().getFirst().requestIdentity().orElseThrow();
            assertTrue(start.isAfter(before)); assertFalse(start.isAfter(inspector.requestMarker()));
            assertEquals(1, inspector.observations().getFirst().sequence());
        }
    }
}
