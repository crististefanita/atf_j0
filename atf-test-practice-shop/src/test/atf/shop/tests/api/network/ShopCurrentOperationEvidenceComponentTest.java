package atf.shop.tests.api.network;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.shop.api.ShopApiRoutes;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.api.validation.ShopApiValidationResult;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Map;
import static atf.shop.tests.api.network.ShopApiJournalFixture.capture;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopCurrentOperationEvidenceComponentTest {
    private static final String PAYMENT_REQUEST = "{\"payment_method\":\"cash-on-delivery\"}";
    private static final String PAYMENT_SUCCESS = "{\"message\":\"Payment was successful\"}";
    private static final String CART = "/carts/current";
    private static final String ADD = "{\"product_id\":\"p1\",\"quantity\":2}";
    private static final String READ = "{\"cart_items\":[{\"product_id\":\"p1\",\"quantity\":2}]}";

    @Test
    void latestPaymentFailureCannotBorrowAnEarlierSuccess() {
        var journal = journal("payment");
        capture(journal, payment(200));
        capture(journal, payment(500));
        var assertions = journal.paymentConfirmed("Cash on Delivery");

        assertFalse(assertions.stream().allMatch(AtfAssertion::passed), "A newer HTTP 500 cannot inherit the old payment PASS");
        assertEquals("E2", journal.evidence(ShopApiCheckpoint.PAYMENT).getFirst().trace().eventId());
    }

    @ParameterizedTest(name = "Current payment HTTP {0} cannot inherit PASS")
    @ValueSource(ints = {400, 500})
    void currentFailureIsSelectedBeforeStatusAndBodyAreEvaluated(int status) {
        var journal = journal("payment");
        capture(journal, payment(200));
        long fence = journal.sequenceMarker();
        capture(journal, payment(status));

        var checks = journal.paymentConfirmed(fence, "Cash on Delivery");
        var validation = ShopApiValidationResult.payment(journal.evidence(ShopApiCheckpoint.PAYMENT), checks).reportingEvidence();

        assertEquals(List.of("E2"), eventIds(journal, ShopApiCheckpoint.PAYMENT));
        assertEquals(status, validation.exchanges().getFirst().evidence().response().status());
        assertFalse(checks.get(1).passed(), "Even a success-shaped body must not make HTTP " + status + " pass");
        assertEquals(AtfReportStatus.FAIL, validation.status());
        assertEquals(2, journal.exchanges().size(), "Evaluation cannot replay payment");
    }

    @Test
    void missingCurrentPaymentClearsThePreviouslySelectedEvidenceAndReportsMissingChecks() {
        var journal = journal("payment");
        capture(journal, payment(200));
        journal.paymentConfirmed("Cash on Delivery");
        long fence = journal.sequenceMarker();

        var checks = journal.paymentConfirmed(fence, "Cash on Delivery");
        var validation = ShopApiValidationResult.payment(journal.evidence(ShopApiCheckpoint.PAYMENT), checks).reportingEvidence();

        assertTrue(journal.selectedEvidence().isEmpty(), "The previous checkpoint selection must be cleared");
        assertTrue(checks.stream().noneMatch(AtfAssertion::passed));
        assertTrue(validation.exchanges().isEmpty(), "Missing capture cannot invent an HTTP exchange");
        assertEquals(4, validation.assertions().size());
        assertEquals(AtfReportStatus.FAIL, validation.status());
        assertEquals(1, journal.exchanges().size());
    }

    @Test
    void newestMatchingPaymentWinsWithoutBorrowingAnotherRouteOrMethod() {
        var journal = journal("payment");
        capture(journal, payment(200));
        long fence = journal.sequenceMarker();
        capture(journal, payment(200));
        capture(journal, payment(200));
        capture(journal, observed("GET", ShopApiRoutes.PAYMENT_CHECK, 200, PAYMENT_REQUEST, PAYMENT_SUCCESS));
        capture(journal, observed("POST", ShopApiRoutes.PAYMENT_CHECK + "/other", 200, PAYMENT_REQUEST, PAYMENT_SUCCESS));

        var checks = journal.paymentConfirmed(fence, "Cash on Delivery");

        assertTrue(checks.stream().allMatch(AtfAssertion::passed));
        assertEquals(List.of("E3"), eventIds(journal, ShopApiCheckpoint.PAYMENT));
        assertTrue(checks.getLast().actual().contains("sequence=3"));
        assertEquals(5, journal.exchanges().size());
    }

    @Test
    void unrelatedTrafficCannotSupplyTheMissingCurrentPayment() {
        var journal = journal("payment");
        capture(journal, payment(200));
        long fence = journal.sequenceMarker();
        capture(journal, observed("GET", ShopApiRoutes.PAYMENT_CHECK, 200, PAYMENT_REQUEST, PAYMENT_SUCCESS));
        capture(journal, observed("POST", "/other", 200, PAYMENT_REQUEST, PAYMENT_SUCCESS));

        var checks = journal.paymentConfirmed(fence, "Cash on Delivery");

        assertTrue(checks.stream().noneMatch(AtfAssertion::passed));
        assertTrue(journal.evidence(ShopApiCheckpoint.PAYMENT).isEmpty());
    }

    @Test
    void currentHttp200StillNeedsTheExpectedPaymentMethodAndResponseBody() {
        var journal = journal("payment");
        capture(journal, payment(200));
        long fence = journal.sequenceMarker();
        capture(journal, observed("POST", ShopApiRoutes.PAYMENT_CHECK, 200, "{}", "{\"message\":\"rejected\"}"));

        var checks = journal.paymentConfirmed(fence, "Cash on Delivery");

        assertFalse(checks.get(0).passed());
        assertFalse(checks.get(1).passed());
        assertEquals(List.of("E2"), eventIds(journal, ShopApiCheckpoint.PAYMENT));
    }

    @ParameterizedTest(name = "Current cart failure belongs to read={0}")
    @ValueSource(booleans = {false, true})
    void currentCartFailureIsNotSkippedInFavorOfSuccessfulOlderAddOrRead(boolean failedRead) {
        var journal = cartJournal();
        capture(journal, observed("POST", CART, 200, ADD, "{}"));
        capture(journal, observed("GET", CART, 200, "", READ));
        long fence = journal.sequenceMarker();
        capture(journal, observed("POST", CART, failedRead ? 200 : 500, ADD, "{}"));
        capture(journal, observed("GET", CART, failedRead ? 500 : 200, "", READ));

        var checks = journal.cartReady(fence, 2);
        var validation = ShopApiValidationResult.cart(journal.evidence(ShopApiCheckpoint.CART), checks).reportingEvidence();

        assertEquals(List.of("E5", "E6"), eventIds(journal, ShopApiCheckpoint.CART));
        assertFalse(checks.get(failedRead ? 2 : 1).passed());
        assertEquals(AtfReportStatus.FAIL, validation.status());
        assertEquals(AtfReportStatus.FAIL, validation.exchanges().get(failedRead ? 1 : 0).status());
        assertEquals(6, journal.exchanges().size(), "Validation cannot replay a cart mutation or read");
    }

    @Test
    void missingReadAfterCurrentAddKeepsTheAddAndReportsTheMissingReadAsACorrelation() {
        var journal = cartJournal();
        capture(journal, observed("GET", CART, 200, "", READ));
        long fence = journal.sequenceMarker();
        capture(journal, observed("POST", CART, 200, ADD, "{}"));
        capture(journal, observed("GET", "/carts/other", 200, "", READ));

        var checks = journal.cartReady(fence, 2);
        var validation = ShopApiValidationResult.cart(journal.evidence(ShopApiCheckpoint.CART), checks).reportingEvidence();

        assertEquals(List.of("E4"), eventIds(journal, ShopApiCheckpoint.CART));
        assertEquals(1, validation.exchanges().size());
        assertEquals(3, validation.assertions().size());
        assertFalse(checks.get(2).passed());
        assertEquals(AtfReportStatus.FAIL, validation.status());
    }

    @Test
    void anotherProductsAddCannotSupplyTheMissingCartOperation() {
        var journal = cartJournal();
        capture(journal, observed("POST", CART, 200, ADD, "{}"));
        capture(journal, observed("GET", CART, 200, "", READ));
        journal.cartReady(2);
        long fence = journal.sequenceMarker();
        capture(journal, observed("POST", CART, 200, ADD.replace("p1", "p1-other"), "{}"));
        capture(journal, observed("GET", CART, 200, "", READ));

        var checks = journal.cartReady(fence, 2);
        var validation = ShopApiValidationResult.cart(journal.evidence(ShopApiCheckpoint.CART), checks).reportingEvidence();

        assertTrue(journal.evidence(ShopApiCheckpoint.CART).isEmpty());
        assertTrue(checks.stream().noneMatch(AtfAssertion::passed));
        assertTrue(validation.exchanges().isEmpty());
        assertEquals(AtfReportStatus.FAIL, validation.status());
    }

    @Test
    void twoCurrentCartAddsRequireAReadAfterTheLatestAdd() {
        var journal = cartJournal();
        long fence = journal.sequenceMarker();
        capture(journal, observed("POST", CART, 200, ADD, "{}"));
        capture(journal, observed("GET", CART, 200, "", READ));
        capture(journal, observed("POST", CART, 200, ADD, "{}"));

        var checks = journal.cartReady(fence, 2);

        assertEquals(List.of("E5"), eventIds(journal, ShopApiCheckpoint.CART));
        assertFalse(checks.get(2).passed(), "A read before the latest add cannot confirm that add");
    }

    @Test
    void currentCartReadRemainsBoundToTheCurrentAddsResource() {
        var journal = cartJournal();
        long fence = journal.sequenceMarker();
        capture(journal, observed("POST", CART, 200, ADD, "{}"));
        capture(journal, observed("GET", CART, 200, "", READ));
        capture(journal, observed("GET", "/carts/other", 200, "", READ));

        var checks = journal.cartReady(fence, 2);

        assertTrue(checks.stream().allMatch(AtfAssertion::passed));
        assertEquals(List.of("E3", "E4"), eventIds(journal, ShopApiCheckpoint.CART));
    }

    @Test
    void disabledCheckpointsPreserveTheUiOnlyContract() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_ONLY);

        assertTrue(journal.paymentConfirmed(10, "Cash on Delivery").isEmpty());
        assertTrue(journal.awaitPaymentConfirmed(null, 10, "Cash on Delivery").isEmpty(),
                "Disabled API capture must not access or wait on Page");
        assertTrue(journal.cartReady(10, 2).isEmpty());
        assertTrue(journal.selectedEvidence().isEmpty());
    }

    private static ShopApiNetworkJournal cartJournal() {
        var journal = journal("product,cart");
        capture(journal, ShopApiJournalFixture.exchange("GET", "/products/search?q=Widget", "",
                "{\"data\":[{\"id\":\"p1\",\"name\":\"Widget\"}]}"));
        capture(journal, ShopApiJournalFixture.exchange("GET", "/products/p1", "", "{\"id\":\"p1\",\"name\":\"Widget\"}"));
        journal.productReady("Widget", "/products/p1");
        return journal;
    }

    private static List<String> eventIds(ShopApiNetworkJournal journal, ShopApiCheckpoint checkpoint) {
        return journal.evidence(checkpoint).stream().map(evidence -> evidence.trace().eventId()).toList();
    }

    private static ShopApiNetworkJournal journal(String checkpoints) {
        return new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, checkpoints));
    }

    private static ShopApiNetworkJournal.Exchange payment(int status) {
        return observed("POST", ShopApiRoutes.PAYMENT_CHECK, status, PAYMENT_REQUEST, PAYMENT_SUCCESS);
    }

    private static ShopApiNetworkJournal.Exchange observed(String method, String path, int status, String request, String response) {
        return new ShopApiNetworkJournal.Exchange(method, path, "https://shop.test" + path, status, request, response,
                Map.of(), Map.of("content-type", "application/json"));
    }
}
