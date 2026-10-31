package atf.autoex.tests.component.business;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.business.AutoExBusinessFault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Local fixture proof. The public-site negative journeys are explicit live acceptance probes. */
@AutoExComponentTest
class AutoExBusinessFaultComponentTest {
    @org.junit.jupiter.api.Test
    void cleanupCannotInjectAnUnreachedBusinessFaultOrCorruptTheOwnedIdentity() {
        try (var fixture = new AutoExBusinessFault.LocalFixture()) {
            for (var point : AutoExBusinessFault.Point.values()) {
                var fault = fixture.fault(point);
                fault.restore();
                fault.beforeRead(AutoExSelectors.LOGGED_IN_NAME);
                fault.beforeRead(AutoExSelectors.CART_QUANTITY);
                fault.beforeRead(AutoExSelectors.CART_QUANTITY);
                fault.beforeRead(AutoExSelectors.ORDER_SUCCESS);
                fault.beforeRead(AutoExSelectors.EMPTY_CART);
                fault.afterCheckpoint(atf.autoex.support.journey.AutoExPurchaseTraceStep.ACCOUNT_CREATED);
                assertFalse(fault.injected(), point + ": cleanup cannot invent the intended fault after an earlier failure");
                assertEquals(0, fixture.markers(), point + ": cleanup cannot create a false original-capture marker");
                assertEquals("Owned Account", fixture.text(AutoExSelectors.LOGGED_IN_NAME), point + ": ownership reads retain the real account identity");
                assertEquals("2", fixture.text(AutoExSelectors.CART_QUANTITY), point + ": cleanup reads do not corrupt the cart");
            }
        }
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "http://foreign.local/font?families=a|b, 0",
            "http://store.local/delete_account?diagnostic=a|b, 1"
    })
    void browserQueriesCannotAbortTheScopedDeletionListener(String url, int expectedEvents) {
        try (var fixture = new AutoExBusinessFault.LocalFixture()) {
            var actual = fixture.deletionTrafficAfter(url);
            assertEquals(expectedEvents, actual.observedRequests(), "Foreign traffic is ignored and an owned request remains fully observed");
            assertEquals(0, actual.navigationCount(), "A controlled fetch never invents a document navigation or deletion verdict");
            assertFalse(actual.singleDeletionNavigation(), "Noise and non-navigation traffic cannot satisfy the account deletion gate");
        }
    }

    @org.junit.jupiter.api.Test
    void redirectDiagnosticsDropTheBrowserQueryWithoutLosingThePath() {
        var actual = new atf.autoex.support.account.AutoExAccountDeletionAttempt.ObservedRequest(
                "GET", "document", true, "http://store.local/login?families=a|b#diagnostic");
        assertEquals("/login", actual.redirectSourcePath(), "Diagnostics retain the observed path without query values or URI parsing failures");
    }

    @ParameterizedTest @EnumSource(value = AutoExBusinessFault.Point.class, names = {"ACCOUNT", "CART", "PURCHASE"})
    void corruptsOnlyTheChosenObservedValueAndRestoresItWithoutNavigation(AutoExBusinessFault.Point point) {
        try (var fixture = new AutoExBusinessFault.LocalFixture()) {
            String selector = switch (point) {
                case ACCOUNT -> AutoExSelectors.LOGGED_IN_NAME;
                case CART -> AutoExSelectors.CART_QUANTITY;
                case PURCHASE -> AutoExSelectors.ORDER_SUCCESS;
                default -> throw new IllegalArgumentException("Not an original boundary: " + point);
            };
            String original = fixture.text(selector);
            var fault = fixture.fault(point);
            fault.beforeRead(".unrelated");
            assertFalse(fault.injected(), "An unrelated read cannot trigger the business fault");
            fault.beforeRead(selector);
            assertTrue(fault.injected(), "The exact real business read is intercepted");
            String rejected = switch (point) {
                case ACCOUNT -> "ATF controlled ACCOUNT mismatch";
                case CART -> "99";
                case PURCHASE -> "ATF controlled PURCHASE mismatch";
                default -> throw new IllegalArgumentException("Not an original boundary: " + point);
            };
            assertEquals(rejected, fixture.text(selector), "The exact intended observed value is corrupted, without changing the independent expected value");
            assertEquals(1, fixture.markers(), "One visible marker identifies original failure evidence");
            fault.beforeRead(selector);
            assertEquals(1, fixture.markedNodes(), "Repeated reads do not replay the fault");
            fault.restore();
            fault.restore();
            assertEquals(original, fixture.text(selector), "Owned cleanup sees the restored original identity");
            assertEquals(0, fixture.markers(), "The fixture removes only its marker");
            assertEquals("about:blank", fixture.url(), "Corruption and restoration never navigate or dispatch a mutation");
        }
    }

    @ParameterizedTest @EnumSource(value = AutoExBusinessFault.Point.class, names = {"RECALCULATION", "REMOVAL"})
    void guestCartFaultWaitsForItsOwnReadAndNeverChangesTheExpectedValue(AutoExBusinessFault.Point point) {
        try (var fixture = new AutoExBusinessFault.LocalFixture()) {
            var fault = fixture.fault(point);
            String selector = point == AutoExBusinessFault.Point.RECALCULATION ? AutoExSelectors.CART_QUANTITY : AutoExSelectors.EMPTY_CART;
            String expected = fixture.text(selector);
            fault.beforeRead(AutoExSelectors.ORDER_SUCCESS);
            assertFalse(fault.injected(), "Another boundary cannot arm the guest-cart fault");
            if (point == AutoExBusinessFault.Point.RECALCULATION) {
                fault.beforeRead(selector);
                assertFalse(fault.injected(), "The initial cart remains healthy before the second add");
                assertEquals(expected, fixture.text(selector), "The first quantity read retains the original value");
            }
            fault.beforeRead(selector);
            assertTrue(fault.injected(), "The selected guest-cart boundary is reached");
            assertFalse(expected.equals(fixture.text(selector)), "Only observed DOM changes; expected stays independent");
            fault.beforeRead(selector);
            assertEquals(1, fixture.markers(), "Repeated reads do not duplicate failure evidence");
            fault.restore();
            assertEquals(expected, fixture.text(selector), "Restoration preserves the original cart text");
            assertEquals("about:blank", fixture.url(), "The fixture performs no navigation or replay");
        }
    }

    @org.junit.jupiter.api.Test
    void genericFailureIsArmedOnlyAfterTheAccountCreationCheckpointReturns() {
        try (var fixture = new AutoExBusinessFault.LocalFixture()) {
            var fault = fixture.fault(AutoExBusinessFault.Point.AFTER_SETUP);
            fault.beforeRead(AutoExSelectors.LOGGED_IN_NAME);
            fault.afterCheckpoint(atf.autoex.support.journey.AutoExPurchaseTraceStep.STORE_OPEN);
            assertFalse(fault.injected(), "Earlier reads and checkpoints remain healthy");
            var failure = assertThrows(IllegalStateException.class,
                    () -> fault.afterCheckpoint(atf.autoex.support.journey.AutoExPurchaseTraceStep.ACCOUNT_CREATED),
                    "The generic error follows the delegated PASS checkpoint");
            assertEquals(AutoExBusinessFault.AFTER_SETUP_MESSAGE, failure.getMessage(), "The exact controlled diagnostic remains available");
            fault.afterCheckpoint(atf.autoex.support.journey.AutoExPurchaseTraceStep.ACCOUNT_CREATED);
            assertEquals(1, fixture.markers(), "The post-PASS fault is injected only once");
            assertEquals(0, fixture.markedNodes(), "No account value was corrupted to manufacture the generic failure");
            fault.restore();
            assertEquals(0, fixture.markers(), "Owned cleanup can remove the screenshot marker");
        }
    }
}
