package atf.api.ui.network;

import atf.api.testkit.AtfComponentTest;
import atf.api.ui.config.UiBrowserConfiguration;
import atf.api.ui.config.UiNetworkInspectionConfiguration;
import atf.api.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Capture and sanitization must not turn a present body into absence. */
@AtfComponentTest
class UiNetworkBodyFidelityComponentTest {
    private static final String HOST = "network-body-fixture.test";
    private static final String ORIGIN = "http://" + HOST;
    private static final String WHITESPACE = " \r\n\t ";

    @ParameterizedTest
    @ValueSource(strings = {"", " \r\n\t ", "  plain text  ", "null"})
    void typedSanitizationAndSubscriberRetainBodyText(String body) {
        var published = new ArrayList<UiNetworkObservation>();
        var input = observation(body);
        try (var inspector = inspector()) {
            inspector.subscribe(published::add);
            var captured = inspector.capture(input).orElseThrow();
            assertEquals(body, input.requestBody());
            assertEquals(body, captured.requestBody());
            assertEquals(body, captured.responseBody());
            assertEquals(body, captured.withService("consumer").requestBody());
            assertEquals(Map.of("x-observed", "yes"), captured.requestHeaders());
            assertEquals(captured, published.getFirst());
        }
    }

    @Test
    void requestSignaturesDoNotCollapseWhitespaceIntoAnAbsentBody() {
        try (var inspector = inspector()) {
            var absent = inspector.capture(observation("")).orElseThrow();
            var present = inspector.capture(observation(WHITESPACE)).orElseThrow();
            assertNotEquals(absent.requestSignature(), present.requestSignature());
            assertEquals(2, inspector.presentationObservations().size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {" \r\n\t ", "  plain text  ", "null"})
    void browserCapturePreservesRequestAndResponseBodies(String body) {
        String responseBody = "  response\n\t ";
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
             var inspector = inspector()) {
            var page = browser.page();
            page.route(ORIGIN + "/**", route -> route.fulfill(new Route.FulfillOptions()
                    .setStatus(200).setContentType("text/plain").setBody(responseBody)));
            page.navigate(ORIGIN);
            inspector.attach(page);
            page.evaluate("async body => (await fetch('/body', {method: 'POST', body})).text()", body);
            page.waitForCondition(() -> inspector.observations().size() == 1);
            var captured = inspector.observations().getFirst();
            assertEquals(body, captured.requestBody());
            assertEquals(responseBody, captured.responseBody());
            assertFalse(captured.requestHeaders().values().contains("application/json"));
        }
    }

    private static UiNetworkInspector inspector() {
        return new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(Set.of(HOST))
                .withCapturePolicy(UiNetworkCapturePolicy.asynchronous()));
    }

    private static UiNetworkObservation observation(String body) {
        return new UiNetworkObservation(1, Instant.EPOCH, "POST", ORIGIN + "/body", "/body", 200,
                Map.of("x-observed", "yes"), Map.of("content-type", "text/plain"), body, body,
                "none", "none", HOST, "controlled fixture", "", true, "",
                UiNetworkCapturePolicy.ResourceKind.FETCH);
    }
}
