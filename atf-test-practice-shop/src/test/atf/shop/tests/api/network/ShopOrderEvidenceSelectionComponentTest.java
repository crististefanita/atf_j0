package atf.shop.tests.api.network;

import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.response.ApiBodyEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.shop.api.ShopApiRoutes;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.api.validation.ShopApiValidationResult;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Current selection and projection through the public browser-observation subscription; no remote writes. */
@ShopComponentTest
class ShopOrderEvidenceSelectionComponentTest {
    private final Map<ShopApiNetworkJournal, atf.ui.network.UiNetworkInspector> captures = new java.util.IdentityHashMap<>();
    @org.junit.jupiter.api.AfterEach void closeCaptures() { captures.values().forEach(atf.ui.network.UiNetworkInspector::close); }
    private void capture(ShopApiNetworkJournal journal, ShopApiNetworkJournal.Exchange exchange) {
        var inspector = captures.computeIfAbsent(journal, key -> {
            var value = new atf.ui.network.UiNetworkInspector(atf.ui.config.UiNetworkInspectionConfiguration.forHosts(
                    List.of(java.net.URI.create(exchange.url()).getHost())));
            key.attach(value); return value;
        });
        ShopApiJournalFixture.capture(journal, exchange, inspector);
    }
    private static final ShopOrderExpectation EXPECTED = ShopOrderExpectation.fromUi("12.01", 2);
    private static final String CONFIRMATION = "Thanks for your order! Your invoice number is INV-12.";
    private static final String BODY = "{\"id\":\"order-current\",\"invoice_number\":\"INV-12\",\"total\":24.02}";

    @ParameterizedTest(name = "{0} · {1}") @MethodSource("failures")
    void newestEligibleObservationOwnsTheChecksAndClearsAnEmptySelection(String route, Case variant) {
        var journal = journal("order");
        long fence = 0L;
        if (variant.prior) {
            capture(journal, order(route, 201, BODY.replace("order-current", "order-old")));
            if (variant.cached) {
                assertTrue(journal.orderCreated(0L, CONFIRMATION, EXPECTED).stream().allMatch(AtfAssertion::passed));
                fence = journal.sequenceMarker();
            }
        }
        if (variant.status != null) capture(journal, order(route, variant.status,
                variant.status < 0 ? ApiBodyEvidence.unavailable("Browser observation", "net::ERR_FAILED") : BODY));

        var checks = journal.orderCreated(fence, CONFIRMATION, EXPECTED);
        var selected = journal.evidence(ShopApiCheckpoint.ORDER);
        var validation = ShopApiValidationResult.order(selected, checks).reportingEvidence();

        assertEquals(5, checks.size(), "Order assertions survive both missing and failed captures");
        assertEquals(5, validation.assertions().size() + validation.exchanges().stream()
                .mapToInt(exchange -> exchange.assertions().size()).sum());
        assertEquals(AtfReportStatus.FAIL, validation.status(), "A success-shaped body cannot override HTTP failure");
        assertFalse(checks.getFirst().passed(), "Current response or missing capture owns the response verdict");
        assertEquals((variant.prior ? 1 : 0) + (variant.status == null ? 0 : 1), journal.exchanges().size(),
                "Evaluation never dispatches or replays an order");
        if (variant.status == null) {
            assertTrue(selected.isEmpty(), "An empty current window clears the cached checkpoint");
            assertTrue(journal.selectedEvidence().isEmpty());
            assertTrue(validation.exchanges().isEmpty(), "No fabricated HTTP exchange for a missing capture");
            assertTrue(checks.stream().noneMatch(AtfAssertion::passed));
        } else {
            assertEquals(1, selected.size());
            String event = variant.prior ? "E2" : "E1";
            assertEquals(event, selected.getFirst().trace().eventId());
            assertEquals(List.of(event), journal.selectedEvidence().stream().map(e -> e.trace().eventId()).toList());
            assertEquals(variant.status.intValue(), selected.getFirst().response().status());
            assertEquals(selected.getFirst().trace(), validation.exchanges().getFirst().evidence().trace());
            if (variant.status < 0) {
                assertEquals("Unavailable", selected.getFirst().attemptHistory().getFirst().statusLabel());
                assertEquals("net::ERR_FAILED", selected.getFirst().attemptHistory().getFirst().reason());
                assertTrue(ApiBodyEvidence.isUnavailable(selected.getFirst().response().body()));
            } else assertTrue(selected.getFirst().response().body().contains("order-current"));
        }
    }

    static Stream<Arguments> failures() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE)
                .flatMap(route -> Stream.of(Case.values()).map(variant -> Arguments.of(route, variant)));
    }

    @ParameterizedTest @ValueSource(strings = {ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE})
    void anotherOperationOrMethodCannotReplaceTheCurrentOrder(String route) {
        var journal = journal("order");
        capture(journal, order(route, 201, BODY));
        capture(journal, new ShopApiNetworkJournal.Exchange("GET", route, "https://shop.test" + route,
                500, "", "{}", Map.of(), HttpHeaderProfiles.JSON_CONTENT));
        capture(journal, order(route + "/other", 500, "{}"));
        var checks = journal.orderCreated(0L, CONFIRMATION, EXPECTED);
        var validation = ShopApiValidationResult.order(journal.evidence(ShopApiCheckpoint.ORDER), checks).reportingEvidence();
        assertEquals(AtfReportStatus.PASS, validation.status());
        assertEquals("E1", journal.evidence(ShopApiCheckpoint.ORDER).getFirst().trace().eventId());
        assertEquals(201, journal.evidence(ShopApiCheckpoint.ORDER).getFirst().response().status());
        assertEquals(3, journal.exchanges().size(), "Unrelated traffic remains in the ordered trace");
    }

    @Test
    void clearingOrderRetainsOtherCheckpointsAndIndependentScenarioSelections() {
        var first = journal("order,payment");
        var sibling = journal("order");
        capture(first, order(ShopApiRoutes.PAYMENT_CHECK, 200, "{}"));
        first.paymentConfirmed(0L, "Cash on Delivery");
        for (var journal : List.of(first, sibling)) {
            capture(journal, order(ShopApiRoutes.GUEST_INVOICE_CREATE, 201, BODY));
            journal.orderCreated(0L, CONFIRMATION, EXPECTED);
        }
        first.orderCreated(first.sequenceMarker(), CONFIRMATION, EXPECTED);
        assertTrue(first.evidence(ShopApiCheckpoint.ORDER).isEmpty());
        assertEquals(List.of("E1"), first.selectedEvidence().stream().map(e -> e.trace().eventId()).toList());
        assertEquals(ShopApiRoutes.PAYMENT_CHECK, first.selectedEvidence().getFirst().request().path());
        assertEquals("E1", sibling.evidence(ShopApiCheckpoint.ORDER).getFirst().trace().eventId());
        assertEquals(201, sibling.evidence(ShopApiCheckpoint.ORDER).getFirst().response().status());
    }

    private static ShopApiNetworkJournal journal(String checkpoints) {
        return new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, checkpoints));
    }

    private static ShopApiNetworkJournal.Exchange order(String route, int status, String body) {
        var exchange = new ShopApiNetworkJournal.Exchange("POST", route, "https://shop.test" + route,
                status, "{}", body, Map.of(), status < 0 ? Map.of() : HttpHeaderProfiles.JSON_CONTENT);
        return status < 0 ? exchange.withFailureReason("net::ERR_FAILED") : exchange;
    }

    enum Case {
        SAME_WINDOW_201_500(true, false, 500), INITIAL_500(false, false, 500),
        OLD_201_THEN_500(true, true, 500),
        INITIAL_MISSING(false, false, null), OLD_201_THEN_TRANSPORT_FAILURE(true, true, -1);
        final boolean prior;
        final boolean cached;
        final Integer status;
        Case(boolean prior, boolean cached, Integer status) { this.prior = prior; this.cached = cached; this.status = status; }
    }
}
