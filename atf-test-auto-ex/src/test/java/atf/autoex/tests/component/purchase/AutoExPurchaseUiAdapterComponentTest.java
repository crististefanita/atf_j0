package atf.autoex.tests.component.purchase;

import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.config.AutoExConfiguration.Payment;
import atf.autoex.config.AutoExConfiguration.Registration;
import atf.autoex.flow.purchase.AutoExPurchaseInputs;
import atf.autoex.flow.purchase.AutoExPurchaseUiFlow;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.ProductsPage;
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.reporting.contract.AtfAssertion;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Exercises the actual UI stage adapter and page objects without a public-store request. */
@AutoExComponentTest
class AutoExPurchaseUiAdapterComponentTest {
    private static final Catalog PRODUCT = new Catalog("Blue Top", "1", "Blue Top", "Rs. 500", 2);
    private static final AutoExPurchaseInputs INPUTS = new AutoExPurchaseInputs(PRODUCT,
            new Registration("10", "May", "1990", "ATF", "Customer", "Demo", "Street", "Area",
                    "United States", "State", "City", "10001", "5550100"),
            new Payment("ATF Demo", "4111111111111111", "123", "12", "2030"), URI.create("https://fixture.test/"));

    @Test
    void missingNamePriceOrStockPublishesIndependentFailedRowsBeforeAdd() {
        for (String actual : List.of("", "Other\nRs. 500\nIn Stock", "Blue Top\nRs. 5000\nIn Stock",
                "Blue Top\nRs. 500\nSold out")) {
            var probe = new Probe(false, 200, actual);
            var adapter = probe.adapter();
            assertSame(probe.gateFailure, assertThrows(IllegalStateException.class, adapter::verifySelectedProduct));
            assertEquals(List.of("openProduct:Blue Top", "checkpoint:PRODUCT_DETAILS"), probe.events);
            assertFalse(probe.rows.stream().allMatch(AtfAssertion::passed));
            assertEquals("Blue Top", probe.rows.getFirst().expected());
            assertEquals(actual, probe.rows.getFirst().actual());
        }
    }

    @Test
    void unconfirmedProductTransitionPreservesTheCauseAndCannotReachTheBusinessCheckpoint() {
        var probe = new Probe(false, 200, "Blue Top\nRs. 500\nIn Stock");
        probe.transitionFailure = new IllegalStateException("owned product document not confirmed");
        assertSame(probe.transitionFailure, assertThrows(IllegalStateException.class, probe.adapter()::verifySelectedProduct));
        assertEquals(List.of("openProduct:Blue Top"), probe.events);
        assertTrue(probe.rows.isEmpty(), "No business verdict or cart mutation can precede a confirmed product transition");
    }

    @Test
    void rejectedOrUnobservedMutationReadsOnlyVisibleConfirmationWithoutWaiting() {
        for (int status : List.of(401, -1)) {
            var probe = new Probe(true, status, "Blue Top\nRs. 500\nIn Stock");
            var adapter = probe.adapter();
            adapter.verifySelectedProduct();
            probe.events.clear();
            assertSame(probe.gateFailure, assertThrows(IllegalStateException.class, adapter::addAndConfirmProduct));
            assertEquals(List.of("fill:2", "add", "await:1", "visible", "cartCheckpoint"), probe.events);
            assertEquals("confirmation absent", probe.rows.getFirst().actual());
            assertTrue(probe.rows.getFirst().passed());
            // The transport gate still fails even though absence of a false success modal is correct.
        }
    }

    @Test
    void successfulTransportWithModalTimeoutPublishesFailedUiInsteadOfLosingTheCheckpoint() {
        var probe = new Probe(true, 200, "Blue Top\nRs. 500\nIn Stock");
        var adapter = probe.adapter();
        adapter.verifySelectedProduct();
        probe.events.clear();
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            browser.page().setDefaultTimeout(10);
            probe.modalTimeout = assertThrows(RuntimeException.class,
                    () -> browser.page().locator("#missing-confirmation").innerText());
        }
        assertSame(probe.gateFailure, assertThrows(IllegalStateException.class, adapter::addAndConfirmProduct));
        assertEquals(List.of("fill:2", "add", "await:1", "waitModal", "cartCheckpoint"), probe.events);
        assertFalse(probe.rows.getFirst().passed());
    }

    @Test
    void uiOnlyUsesTheSameConfiguredQuantityAndNeverWaitsForApi() {
        var probe = new Probe(false, 200, "Blue Top\nRs. 500\nIn Stock");
        var adapter = probe.adapter();
        adapter.verifySelectedProduct();
        probe.events.clear();
        adapter.addAndConfirmProduct();
        assertEquals(List.of("fill:2", "add", "waitModal", "checkpoint:CART_ADD"), probe.events);
        assertTrue(probe.rows.stream().allMatch(AtfAssertion::passed));
    }

    private static final class Probe {
        private final boolean api;
        private final int status;
        private final String detailText;
        private final List<String> events = new ArrayList<>();
        private final RuntimeException gateFailure = new IllegalStateException("checkpoint gate");
        private List<AtfAssertion> rows = List.of();
        private RuntimeException modalTimeout;
        private RuntimeException transitionFailure;

        private Probe(boolean api, int status, String detailText) {
            this.api = api; this.status = status; this.detailText = detailText;
        }

        @SuppressWarnings("unchecked")
        private AutoExPurchaseUiFlow adapter() {
            var run = (AutoExPurchaseSession) Proxy.newProxyInstance(AutoExPurchaseSession.class.getClassLoader(),
                    new Class<?>[]{AutoExPurchaseSession.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "waitForText":
                                if (AutoExSelectors.CART_ADD_CONFIRMATION.equals(args[0])) {
                                    events.add("waitModal");
                                    if (modalTimeout != null) throw modalTimeout;
                                }
                                return null;
                            case "openFirstProduct":
                                assertEquals(PRODUCT.productName(), args[0]);
                                events.add("openProduct:" + args[0]);
                                if (transitionFailure != null) throw transitionFailure;
                                return null;
                            case "text": return AutoExSelectors.PRODUCT_INFORMATION.equals(args[0])
                                    ? detailText : "Added! Your product has been added to cart.";
                            case "fill": events.add("fill:" + args[1]); return null;
                            case "click": events.add("add"); return null;
                            case "observesApiTraffic": return api;
                            case "awaitCartExchange":
                                events.add("await:" + args[0]);
                                return new AutoExPurchaseSession.CartMutationObservation("1", status > 0,
                                        status, "fixture transport", status > 0 ? 17 : -1);
                            case "visible": events.add("visible"); return false;
                            case "checkpoint":
                                events.add("checkpoint:" + ((AutoExPurchaseTraceStep) args[0]).name());
                                rows = (List<AtfAssertion>) args[1];
                                if (rows.stream().anyMatch(row -> !row.passed())) throw gateFailure;
                                return null;
                            case "cartAddCheckpoint":
                                events.add("cartCheckpoint");
                                rows = (List<AtfAssertion>) args[2];
                                if (status < 200 || status >= 300 || rows.stream().anyMatch(row -> !row.passed())) throw gateFailure;
                                return null;
                            default: throw new AssertionError("Unexpected capability: " + method.getName());
                        }
                    });
            return new AutoExPurchaseUiFlow(run, new ProductsPage(run, PRODUCT.productName()), INPUTS);
        }
    }
}
