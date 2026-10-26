package atf.autoex.support.journey;

import atf.autoex.support.catalog.AutoExCatalogSnapshot;
import atf.autoex.support.cart.AutoExCartJourneyPort;
import atf.autoex.ui.AutoExCheckoutPagePort;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.reporting.contract.AtfAssertion;
import java.util.List;

/** Journey facade composing page ports; the JUnit lifecycle is supplied by the test harness. */
public interface AutoExPurchaseSession extends AutoExCartJourneyPort, AutoExCheckoutPagePort {
    void checkpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void apiCheckpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void finalScreenshot(AutoExTraceStep step, List<AtfAssertion> assertions);
    AutoExCatalogSnapshot inspectCatalogApi(String productName);
    boolean observesApiTraffic();
    void markCausalApiTrafficBaseline();
    CartMutationObservation awaitCartExchange(String productId);
    void cartAddCheckpoint(AutoExTraceStep step, CartMutationObservation mutation,
                           List<AtfAssertion> uiAssertions);

    /** Bounded, passive read-back of the one browser request caused by the add action. */
    record CartMutationObservation(String productId, boolean responseObserved, int status,
                                   String diagnostic, long sequence) {
        public CartMutationObservation {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId must not be blank");
            }
            productId = productId.trim();
            diagnostic = diagnostic == null ? "" : diagnostic.trim();
            if (responseObserved && sequence < 1) {
                throw new IllegalArgumentException("an observed exchange must have a positive sequence");
            }
            if (!responseObserved && sequence != -1) {
                throw new IllegalArgumentException("a missing exchange must use sequence -1");
            }
        }

        public boolean successfulResponse() {
            return responseObserved && status >= 200 && status < 300;
        }
    }
}
