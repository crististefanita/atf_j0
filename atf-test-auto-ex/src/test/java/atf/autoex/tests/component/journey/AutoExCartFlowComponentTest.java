package atf.autoex.tests.component.journey;

import atf.autoex.business.cart.AutoExCartFlow;
import atf.autoex.business.cart.AutoExCartPort;
import atf.autoex.business.cart.AutoExCartValidationException;
import atf.autoex.support.checkout.AutoExOrderLine;
import atf.autoex.tests.extension.AutoExComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** A fake business port proves gates and failure precedence without browser or Reporting. */
@AutoExComponentTest
class AutoExCartFlowComponentTest {
    private static final AutoExOrderLine EXPECTED = new AutoExOrderLine("Blue Top", "Rs. 500", "2", "Rs. 1000");

    @Test
    void observesThenPublishesThenEntersCheckoutExactlyOnce() {
        var calls = new ArrayList<String>();
        var result = AutoExCartFlow.checkout(port(EXPECTED, calls), EXPECTED,
                observed -> calls.add("publish:" + observed.passed()));
        assertTrue(result.passed());
        assertEquals(EXPECTED, result.observed());
        assertEquals(List.of("observe", "publish:true", "checkout"), calls);
        assertEquals(4, result.checks().size());
    }

    @Test
    void everyWrongOrMissingFieldPreventsCheckout() {
        var observations = new ArrayList<>(List.of(
                new AutoExOrderLine("Other", "Rs. 500", "2", "Rs. 1000"),
                new AutoExOrderLine("Blue Top", "Rs. 5000", "2", "Rs. 1000"),
                new AutoExOrderLine("Blue Top", "Rs. 500", "20", "Rs. 1000"),
                new AutoExOrderLine("Blue Top", "Rs. 500", "2", "Rs. 10000"),
                new AutoExOrderLine("Blue Top", "Rs. 500", "", "Rs. 1000")));
        observations.add(null);
        for (var actual : observations) {
            var calls = new ArrayList<String>();
            var failure = assertThrows(AutoExCartValidationException.class,
                    () -> AutoExCartFlow.checkout(port(actual, calls), EXPECTED,
                            result -> calls.add("publish:" + result.passed())));
            assertEquals(actual, failure.result().observed());
            assertEquals(List.of("observe", "publish:false"), calls);
        }
    }

    @Test
    void aBusinessFailureRemainsPrimaryWhenItsPublicationAlsoFails() {
        var publicationFailure = new IllegalStateException("publisher unavailable");
        var calls = new ArrayList<String>();
        var failure = assertThrows(AutoExCartValidationException.class,
                () -> AutoExCartFlow.checkout(port(null, calls), EXPECTED, result -> { throw publicationFailure; }));
        assertSame(publicationFailure, failure.getCause());
        assertEquals(List.of("observe"), calls);
    }

    @Test
    void observationAndSuccessfulGatePublicationFailuresNeverStartCheckout() {
        var timeout = new IllegalStateException("observation timed out");
        var port = new AutoExCartPort() {
            @Override public AutoExOrderLine observe() { throw timeout; }
            @Override public void proceedToCheckout() { throw new AssertionError("unsafe checkout"); }
        };
        assertSame(timeout, assertThrows(IllegalStateException.class,
                () -> AutoExCartFlow.checkout(port, EXPECTED, result -> { throw new AssertionError("no result"); })));
        var calls = new ArrayList<String>();
        var publicationFailure = new IllegalStateException("publisher unavailable");
        assertSame(publicationFailure, assertThrows(IllegalStateException.class,
                () -> AutoExCartFlow.checkout(port(EXPECTED, calls), EXPECTED,
                        result -> { throw publicationFailure; })));
        assertEquals(List.of("observe"), calls);
    }

    private static AutoExCartPort port(AutoExOrderLine actual, List<String> calls) {
        return new AutoExCartPort() {
            @Override public AutoExOrderLine observe() { calls.add("observe"); return actual; }
            @Override public void proceedToCheckout() { calls.add("checkout"); }
        };
    }
}
