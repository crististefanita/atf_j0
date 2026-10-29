package atf.autoex.tests.component.purchase;

import atf.autoex.business.checkout.*;
import atf.autoex.support.checkout.AutoExOrderLine;
import atf.autoex.tests.extension.AutoExComponentTest;
import org.junit.jupiter.api.Test;
import java.util.*;
import static atf.testkit.assertions.AuditedAssertions.*;

@AutoExComponentTest
class AutoExCheckoutAccessComponentTest {
    private static final AutoExOrderLine EXPECTED = new AutoExOrderLine("Blue Top", "Rs. 500", "2", "Rs. 1000");

    @Test void aLostOrForeignIdentityOrWrongCurrentCartCannotActivateCheckout() {
        for (var customer : Arrays.asList("foreign owner", "", null, "owned")) {
            for (var cart : Arrays.asList(EXPECTED, null, new AutoExOrderLine("Blue Top", "Rs. 500", "3", "Rs. 1500"))) {
                if ("owned".equals(customer) && EXPECTED.equals(cart)) continue;
                var calls = new ArrayList<String>(); var observed = new ArrayList<AutoExCheckoutAccess>();
                var failure = assertThrows(AutoExCheckoutValidationException.class, () -> AutoExCheckoutFlow.enter(
                        port(customer, cart, calls), "owned", EXPECTED, result -> { calls.add("publish"); observed.add(result); }));
                assertEquals(List.of("customer", "cart", "publish"), calls);
                assertEquals(5, failure.result().checks().size()); assertFalse(failure.result().passed());
                assertEquals(List.of(failure.result()), observed); assertEquals("owned", failure.result().expectedCustomer());
            }
        }
    }

    @Test void allFiveIndependentChecksArePublishedBeforeTheOnlyActivation() {
        var calls = new ArrayList<String>();
        var result = AutoExCheckoutFlow.enter(port("owned", EXPECTED, calls), "owned", EXPECTED, value -> calls.add("publish"));
        assertTrue(result.passed()); assertEquals(5, result.checks().size());
        assertEquals(List.of("customer", "cart", "publish", "checkout"), calls);
    }

    @Test void publicationFailureCannotChangeTheBusinessCauseOrPermitAnActivation() {
        var secondary = new IllegalStateException("controlled publication failure"); var calls = new ArrayList<String>();
        var failure = assertThrows(AutoExCheckoutValidationException.class, () -> AutoExCheckoutFlow.enter(
                port("foreign", EXPECTED, calls), "owned", EXPECTED, value -> { calls.add("publish"); throw secondary; }));
        assertSame(secondary, failure.getSuppressed()[0]); assertEquals(List.of("customer", "cart", "publish"), calls);
        calls.clear();
        assertSame(secondary, assertThrows(IllegalStateException.class, () -> AutoExCheckoutFlow.enter(
                port("owned", EXPECTED, calls), "owned", EXPECTED, value -> { calls.add("publish"); throw secondary; })));
        assertEquals(List.of("customer", "cart", "publish"), calls);
    }

    @Test void aMismatchBeyondThePreviewLimitStillRefusesCheckout() {
        var calls = new ArrayList<String>(); String prefix = "owner".repeat(60);
        var failure = assertThrows(AutoExCheckoutValidationException.class, () -> AutoExCheckoutFlow.enter(
                port(prefix + "wrong", EXPECTED, calls), prefix + "owned", EXPECTED, result -> calls.add("publish")));
        assertEquals(prefix + "wrong", failure.result().observedCustomer()); assertFalse(failure.result().checks().getFirst().passed());
        assertEquals(List.of("customer", "cart", "publish"), calls);
    }

    private static AutoExCheckoutFlow.Port port(String customer, AutoExOrderLine cart, List<String> calls) {
        return new AutoExCheckoutFlow.Port() {
            public String customerName() { calls.add("customer"); return customer; }
            public AutoExOrderLine observe() { calls.add("cart"); return cart; }
            public void proceedToCheckout() { calls.add("checkout"); }
        };
    }
}
