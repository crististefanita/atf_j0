package atf.autoex.tests.component.purchase;

import atf.autoex.business.purchase.AutoExPurchaseFlow;
import atf.autoex.tests.extension.AutoExComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@AutoExComponentTest
class AutoExPurchaseFlowComponentTest {
    private static final List<String> STAGES = List.of("product", "add", "cart", "register",
            "review", "payment", "order", "invoice", "cleanup");

    @Test
    void completesEachSemanticGateOnceBeforeTheNextMutation() {
        var calls = new ArrayList<String>();
        new AutoExPurchaseFlow().complete(new Probe(calls, -1, null));
        assertEquals(STAGES, calls);
    }

    @Test
    void anyFailedGateStopsLaterMutationsWithoutRetryOrFailureReplacement() {
        for (int gate = 0; gate < STAGES.size(); gate++) {
            for (RuntimeException failure : List.of(new IllegalStateException("checkpoint failed"),
                    new IllegalStateException("observation timed out", new java.util.concurrent.TimeoutException()),
                    new IllegalArgumentException("publication failed"))) {
                var calls = new ArrayList<String>();
                var probe = new Probe(calls, gate, failure);
                assertSame(failure, assertThrows(RuntimeException.class,
                        () -> new AutoExPurchaseFlow().complete(probe)));
                assertEquals(STAGES.subList(0, gate + 1), calls);
            }
        }
    }

    private record Probe(List<String> calls, int failedStage, RuntimeException failure) implements AutoExPurchaseFlow.Port {
        private void stage(String name) {
            calls.add(name);
            if (calls.size() - 1 == failedStage) throw failure;
        }
        @Override public void verifySelectedProduct() { stage("product"); }
        @Override public void addAndConfirmProduct() { stage("add"); }
        @Override public void verifyCartAndProceed() { stage("cart"); }
        @Override public void registerOwnedCustomer() { stage("register"); }
        @Override public void reviewCheckout() { stage("review"); }
        @Override public void openPaymentForm() { stage("payment"); }
        @Override public void confirmDemoOrder() { stage("order"); }
        @Override public void verifyInvoice() { stage("invoice"); }
        @Override public void deleteOwnedCustomer() { stage("cleanup"); }
    }
}
