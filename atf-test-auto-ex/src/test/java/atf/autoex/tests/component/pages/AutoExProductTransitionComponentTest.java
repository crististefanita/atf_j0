package atf.autoex.tests.component.pages;

import atf.autoex.runtime.catalog.AutoExProductTransition;
import atf.autoex.runtime.navigation.AutoExReadOnlyNavigation;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.product.AutoExProductTransitionSite;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.net.URI;
import java.time.Duration;
import static atf.testkit.assertions.AuditedAssertions.*;

@AutoExComponentTest
class AutoExProductTransitionComponentTest {
    @ParameterizedTest(name="product transition · {0}")
    @CsvSource({"ready,true,1","late,true,1","520-once,true,2","queue-once,true,2",
            "repeat-520,false,2","forbidden,false,1","wrong-product,false,1","missing,false,1",
            "unsafe-target,false,0","post-document,false,1","vignette,true,1", "vignette-late,true,1", "vignette-520,true,2",
            "vignette-missing,false,0", "vignette-ambiguous,false,0", "vignette-stuck,false,0", "525-once,true,2", "repeat-525,false,2", "post-525,false,1",
            "queue-late,true,2", "ambiguous-product,false,1"})
    void theOwnedDocumentAndExpectedProductConfirmOnePointerActivation(String mode, boolean passes, int requests) throws Exception {
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofMillis(1100), Duration.ofSeconds(3), 1440, 1000, 70, true);
        try (var browser = PlaywrightUiSession.open(configuration);
             var site = new AutoExProductTransitionSite(browser.page(), mode)) {
            var page = browser.page(); var actions = browser.newActionSequence();
            var navigation = new AutoExReadOnlyNavigation(page, actions, site.origin(), UiRetryPolicy.readOnly(2, Duration.ZERO));
            site.openReady(page);
            assertEquals(0, site.activations(), "Fixture preparation never activates the product");
            var transition = new AutoExProductTransition(page, actions, site.origin(), navigation);
            if (passes) {
                assertDoesNotThrow(() -> transition.open("Blue Top"));
                assertEquals("Blue Top", page.locator(".product-information h2").innerText(), "The configured product is actually visible");
            } else {
                var failure = assertThrows(RuntimeException.class, () -> transition.open("Blue Top"));
                if (!mode.equals("unsafe-target")) assertNotNull(failure.getCause(), "Failure retains its original cause");
                if (mode.contains("520") || mode.contains("525")) assertTrue(failure.getMessage().contains(mode.contains("525") ? "525" : "520"),
                        "The observed HTTP failure remains explicit rather than a generic locator timeout");

            }
            if (mode.contains("520") || mode.contains("525") || mode.startsWith("queue-")) {
                var availability = actions.snapshot().stream().filter(action -> action.operation().equals("Store availability")).toList();
                assertFalse(availability.isEmpty(), "Observed transient responses remain auditable");
                assertTrue(availability.stream().allMatch(action -> action.status().name().equals("INFO")),
                        "HTTP 520/525 and queue-full observations cannot display a business PASS");
            }
            if (mode.startsWith("vignette")) {
                assertEquals(mode.equals("vignette-missing") || mode.equals("vignette-ambiguous") ? 0 : 1,
                        site.closes(), "Only one explicit visible Close is activated; its event is untrusted DOM input");

            }
            assertEquals(requests, site.requests().size(), "Only the allowlisted GET recovery can add another document request");
            assertEquals(mode.equals("unsafe-target") ? 0 : 1, site.activations(), "The pointer is never replayed");
            if (mode.equals("post-document") || mode.equals("post-525")) assertEquals("POST /product_details/1", site.requests().getFirst());
            if (!mode.equals("unsafe-target")) assertEquals(passes ? "PASS" : "FAIL", actions.snapshot().getLast().status().name());
            assertEquals(mode.equals("unsafe-target") ? 0 : 1,
                    actions.snapshot().stream().filter(action -> action.operation().equals("Click")).count(), "One timed logical action owns activation and readiness");
        }
    }
}
