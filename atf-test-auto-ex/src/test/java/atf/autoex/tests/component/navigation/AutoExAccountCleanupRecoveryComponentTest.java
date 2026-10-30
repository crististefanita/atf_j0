package atf.autoex.tests.component.navigation;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.runtime.navigation.AutoExStoreUnavailableException;
import atf.autoex.business.cart.AutoExCartValidationException;
import atf.reporting.ui.failure.UiCheckpointFailure;
import org.junit.jupiter.api.Test;
import java.util.List;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Real browser documents prove cleanup recovery, identity ownership and the non-replay boundary. */
@AutoExComponentTest
class AutoExAccountCleanupRecoveryComponentTest {
    @org.junit.jupiter.params.ParameterizedTest(name = "after account setup · apiEnabled={0} · foreignIdentity={1}")
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void failureAfterRealRegistrationRetainsItsCauseAndDeletesOnlyTheOwnedIdentity(
            boolean apiEnabled, boolean foreignIdentity) throws Exception {
        var setupCalls = List.of("GET /", "GET /login", "POST /signup", "POST /create_account", "GET /");
        var ownedCalls = List.of("GET /", "GET /login", "POST /signup", "POST /create_account", "GET /",
                "GET /delete_account", "GET /");
        try (var fixture = AutoExCleanupBrowserFixture.accountSetup(apiEnabled, foreignIdentity)) {
            assertFalse(fixture.run.accountLifecycle().creationWasAttempted(), "Only the real create action may arm cleanup");
            var injected = new IllegalStateException("Controlled failure after account setup");
            var primary = assertThrows(IllegalStateException.class, () -> fixture.failAfterAccountSetup(injected));
            assertSame(injected, primary);
            assertTrue(fixture.run.accountLifecycle().creationWasConfirmed());
            fixture.markBrowser();
            String originalImage = fixture.failurePng();
            fixture.run.finish(primary);
            assertEquals(1, fixture.signups, "Signup is submitted once without replay");
            assertEquals(1, fixture.creations, "Account creation is submitted once without replay");
            assertEquals(foreignIdentity ? setupCalls : ownedCalls, fixture.calls);
            assertEquals(foreignIdentity ? 0 : 1, fixture.deletions);
            assertEquals(!foreignIdentity, fixture.run.accountLifecycle().deletionWasConfirmed());
            assertEquals(foreignIdentity ? 1 : 0, primary.getSuppressed().length);
            if (foreignIdentity) assertTrue(primary.getSuppressed()[0].getMessage().contains("identity is not owned"));
            String html = fixture.html();
            assertTrue(html.contains(primary.getMessage()), "Cleanup preserves the primary diagnostic in the report");
            assertEquals(foreignIdentity, html.contains("identity is not owned"),
                    "The report retains a secondary ownership refusal without fabricating one for owned cleanup");
            assertEquals(foreignIdentity, html.contains("Automation Exercise account cleanup failure"));
            assertTrue(reportMarkup(html).contains("data-atf-evidence-status=\"fail\""));
            assertTrue(html.contains("data:image/png;base64," + originalImage), "Failure evidence precedes cleanup navigation");
            assertTrue(html.contains("disposable account is created on the practice site"));
            assertEquals(apiEnabled, html.contains("API network inspector"));
            assertFalse(html.contains(fixture.account.password()), "The generated password cannot leak into evidence");
        }
        try (var healthy = AutoExCleanupBrowserFixture.accountSetup(apiEnabled, false)) {
            assertEquals(null, healthy.browserMarker(), "The next registration receives a fresh browser context");
            healthy.registerAccount();
            healthy.completeOwnedAccount();
            healthy.run.finish(null);
            assertEquals(ownedCalls, healthy.calls);
            assertEquals(1, healthy.signups);
            assertEquals(1, healthy.creations);
            assertEquals(1, healthy.deletions);
            String html = healthy.html();
            assertFalse(html.contains("Controlled failure after account setup"));
            assertFalse(reportMarkup(html).contains("data-atf-evidence-status=\"fail\""));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "cart confirmation · apiEnabled={0}")
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void aWrongCartBlocksCheckoutWithoutReplayAndTheNextValidCartRemainsIndependent(boolean apiEnabled) throws Exception {
        try (var fixture = AutoExCleanupBrowserFixture.cart(apiEnabled, false)) {
            fixture.markBrowser();
            var failure = assertThrows(AutoExCartValidationException.class, fixture::checkoutCart);
            assertEquals("Cart validation failed: [Unit price]", failure.getMessage());
            assertInstanceOf(UiCheckpointFailure.class, failure.getCause());
            assertTrue(failure.getCause().getMessage().contains("the cart preserves the product, quantity and calculated total"));
            assertEquals(fixture.run.configuration().catalog().unitPrice(), failure.result().expected().unitPrice(),
                    "Expected price is taken from the independent configured product");
            assertEquals("Rs. 999", failure.result().observed().unitPrice(), "The real page adapter reads the wrong DOM price");
            String originalImage = fixture.failurePng();
            fixture.run.finish(failure);
            assertEquals(0, failure.getSuppressed().length, "Owned cleanup preserves the original cart failure");
            assertTrue(fixture.run.accountLifecycle().deletionWasConfirmed());
            assertEquals(List.of("GET /", "POST /view_cart", "GET /delete_account", "GET /"), fixture.calls,
                    "One add, no checkout activation and no mutation replay before owned cleanup");
            assertEquals(1, fixture.deletions);
            String html = fixture.html();
            assertTrue(html.contains("the cart preserves the product, quantity and calculated total"));
            assertTrue(html.contains("Rs. 999"));
            assertTrue(reportMarkup(html).contains("data-atf-evidence-status=\"fail\""));
            assertTrue(html.contains("data:image/png;base64," + originalImage), "The failed cart image precedes cleanup navigation");
            assertTrue(html.contains("#add"), "The unique native activation retains its locator");
            assertEquals(apiEnabled, html.contains("API network inspector"));
        }
        try (var healthy = AutoExCleanupBrowserFixture.cart(apiEnabled, true)) {
            assertEquals(null, healthy.browserMarker(), "The next cart receives an independent browser context");
            var result = healthy.checkoutCart();
            assertTrue(result.passed(), "The healthy neighbour validates the configured cart");
            healthy.completeOwnedAccount();
            healthy.run.finish(null);
            assertEquals(List.of("GET /", "POST /view_cart", "GET /checkout", "GET /delete_account", "GET /"), healthy.calls,
                    "Only a validated cart enters checkout, once");
            assertEquals(1, healthy.deletions);
            String html = healthy.html();
            assertFalse(html.contains("Rs. 999"), "The rejected DOM state is not inherited");
            assertFalse(reportMarkup(html).contains("data-atf-evidence-status=\"fail\""), "The next scenario keeps its own passing verdict");
        }
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "order confirmation · apiEnabled={0}")
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void orderRejectionRetainsBusinessEvidenceBeforeOwnedCleanupAndTheNextRunIsIndependent(boolean apiEnabled) throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(apiEnabled)) {
            fixture.markBrowser();
            Throwable primary = fixture.rejectOrderConfirmation();
            assertTrue(primary.getMessage().contains("the demo checkout confirms the order"));
            assertTrue(primary.getMessage().contains("Order Placed!"), "Expected business text is independent of the rejected document");
            String originalImage = fixture.failurePng();
            fixture.run.finish(primary);
            assertEquals(0, primary.getSuppressed().length, "Successful owned cleanup does not replace the business cause");
            assertTrue(fixture.run.accountLifecycle().deletionWasConfirmed());
            assertEquals(List.of("GET /", "POST /payment", "GET /delete_account", "GET /"), fixture.calls,
                    "Payment is never replayed and only the owned account is deleted once");
            assertEquals(1, fixture.deletions);
            String html = fixture.html();
            assertTrue(html.contains("TEST BODY"));
            assertTrue(html.contains("the demo checkout confirms the order"));
            assertTrue(html.contains("PAYMENT REJECTED"));
            assertTrue(reportMarkup(html).contains("data-atf-evidence-status=\"fail\""), "Cleanup success cannot turn the rejected order into PASS");
            assertTrue(html.contains("#pay"), "The single native payment activation retains its actual locator in evidence");
            assertTrue(html.contains("data:image/png;base64," + originalImage),
                    "The report retains the actual rejected document captured before cleanup navigation");
            assertEquals(apiEnabled, html.contains("API network inspector"));
            assertTrue(html.contains(apiEnabled ? "UI + API evidence" : "UI evidence"),
                    "System identifies the effective evidence channel of this invocation");
        }
        try (var healthy = new AutoExCleanupBrowserFixture(apiEnabled)) {
            assertEquals(null, healthy.browserMarker(), "The next invocation receives a fresh browser context");
            assertEquals(List.of("GET /"), healthy.calls, "A new invocation starts without the failed payment");
            healthy.completeOwnedAccount();
            healthy.run.finish(null);
            assertTrue(healthy.run.accountLifecycle().deletionWasConfirmed());
            assertEquals(1, healthy.deletions);
            String html = healthy.html();
            assertFalse(html.contains("PAYMENT REJECTED"), "The previous failure is not inherited");
            assertTrue(html.contains("Account deletion navigation"));
        }
    }

    @Test void queueFullAfterPaymentRecoversThroughHomeAndDeletesOnlyTheOwnedAccount() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(false, false)) {
            Throwable primary = fixture.failPayment();
            fixture.run.finish(primary);
            assertEquals(0, primary.getSuppressed().length, java.util.Arrays.toString(primary.getSuppressed()));
            assertTrue(fixture.run.accountLifecycle().deletionWasConfirmed());
            assertEquals(List.of("GET /", "POST /payment", "GET /", "GET /delete_account", "GET /"), fixture.calls);
            assertEquals(1, fixture.deletions);
            String html = fixture.html();
            assertTrue(html.contains("POST /payment"));
            assertTrue(html.contains("queueFull=true"));
            assertTrue(html.contains("Cleanup recovery destination"));
            assertTrue(html.contains("Account deletion navigation"));
        }
    }

    private static String reportMarkup(String html) {
        return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
    }

    @Test void anotherVisibleIdentityCannotAuthorizeDeletionAfterRecovery() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(true, false)) {
            Throwable primary = fixture.failPayment();
            fixture.run.finish(primary);
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0].getMessage().contains("identity is not owned"));
            assertEquals(0, fixture.deletions);
            assertEquals(List.of("GET /", "POST /payment", "GET /"), fixture.calls);
        }
    }

    @Test void persistentBusyHomeIsBoundedAndNeverReplaysPayment() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(false, true)) {
            Throwable primary = fixture.failPayment();
            fixture.run.finish(primary);
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0] instanceof AutoExStoreUnavailableException);
            assertEquals(1, fixture.calls.stream().filter("POST /payment"::equals).count());
            assertEquals(4, fixture.calls.stream().filter("GET /"::equals).count());
            assertEquals(0, fixture.deletions);
        }
    }

    @Test void anAlreadyStartedDeletionCannotBeReplayedByDocumentRecovery() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(false, false)) {
            Throwable primary = fixture.failPayment();
            fixture.run.accountLifecycle().deletionStarted();
            fixture.run.finish(primary);
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0].getMessage().contains("may replay"));
            assertEquals(List.of("GET /", "POST /payment"), fixture.calls);
            assertEquals(0, fixture.deletions);
        }
    }

}
