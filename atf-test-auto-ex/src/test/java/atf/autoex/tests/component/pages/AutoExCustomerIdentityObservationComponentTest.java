package atf.autoex.tests.component.pages;

import atf.autoex.business.checkout.*;
import atf.autoex.support.checkout.AutoExOrderLine;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.identity.AutoExCustomerIdentityFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Real page wiring: readiness observes the DOM, while the checkout gate alone authorizes its owner. */
@AutoExComponentTest
class AutoExCustomerIdentityObservationComponentTest {
    private static final AutoExOrderLine CART = new AutoExOrderLine("Blue Top", "Rs. 500", "2", "Rs. 1000");

    @ParameterizedTest @ValueSource(strings = {"delayed", "foreign", "absent", "empty", "ambiguous", "delayed-text", "replaced-document", "delayed-foreign", "late"})
    void boundedIdentityObservationCannotSubstituteTheExpectedOwnerOrReplayCheckout(String mode) {
        try (var fixture = new AutoExCustomerIdentityFixture(mode)) {
            var adapter = fixture.adapter();
            var publications = new AtomicInteger();
            Runnable checkout = () -> AutoExCheckoutFlow.enter(adapter, "owned customer", CART, result -> publications.incrementAndGet());
            long started = System.nanoTime();
            boolean owned = mode.equals("delayed") || mode.equals("delayed-text") || mode.equals("replaced-document");
            boolean foreign = mode.endsWith("foreign");
            if (owned) checkout.run();
            else if (foreign) {
                var failure = assertThrows(AutoExCheckoutValidationException.class, checkout::run,
                        "A visible foreign customer is rejected by the business gate");
                assertTrue(failure.getMessage().contains("customer"), "The identity failure retains its business diagnosis");
                assertEquals("foreign customer", failure.result().observedCustomer(),
                        "A delayed foreign identity remains the actual observed owner");
            } else {
                var failure = assertThrows(IllegalStateException.class, checkout::run,
                        "Unavailable or ambiguous identity cannot reach checkout");
                assertTrue(failure.getMessage().contains("url="), "The failure identifies the observed document");
                assertTrue(failure.getMessage().contains("matches="), "The failure reports the selector cardinality");
                assertNotNull(failure.getCause(), "The native readiness failure remains inspectable");
            }
            assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() < 4000,
                    "Observation uses the configured short fixture deadline without whole-flow replay");
            assertEquals(owned ? "1" : "0", fixture.checkoutActivations(),
                    "Only the owned ready customer activates checkout, exactly once");
            assertEquals(owned || foreign ? 1 : 0, publications.get(),
                    "Ready identity is evaluated and published once before permission to checkout");
        }
    }
}
