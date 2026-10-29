package atf.shop.tests.component.api;

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
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkCapturePolicy;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.UiNetworkObservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopOrderEvidenceComponentTest {
    private final Map<ShopApiNetworkJournal, UiNetworkInspector> captures = new java.util.IdentityHashMap<>();
    @org.junit.jupiter.api.AfterEach void closeCaptures() { captures.values().forEach(UiNetworkInspector::close); }
    private static final ShopOrderExpectation EXPECTED = ShopOrderExpectation.fromUi("12.01", 2);
    private static final String CONFIRMATION = "Thanks for your order! Your invoice number is INV-12.";
    private static final String SUCCESS = "{\"id\":\"order-1\",\"invoice_number\":\"INV-12\",\"total\":24.02}";

    @ParameterizedTest(name = "{0} · {1}")
    @MethodSource("cases")
    void selectedEvidenceAndChecksDescribeTheSameCurrentOperation(String route, OrderCase variant) {
        var journal = journal("order");
        long fence = 0;
        if (variant.previous) {
            capture(journal, route, 201, SUCCESS);
            journal.orderCreated(0, CONFIRMATION, EXPECTED);
            fence = journal.sequenceMarker();
        }
        switch (variant) {
            case SUCCESS -> capture(journal, route, 201, SUCCESS);
            case FIRST_500, OLD_THEN_500 -> capture(journal, route, 500, "{\"message\":\"current order rejected\"}");
            case CURRENT_201_THEN_500 -> {
                capture(journal, route, 201, SUCCESS);
                capture(journal, route, 500, "{\"message\":\"latest order rejected\"}");
            }
            case TRANSPORT_FAILED -> capture(journal, route, -1, "<unavailable: connection reset>");
            case UNRELATED -> capture(journal, "/payment/check", 200, "{\"message\":\"Payment was successful\"}");
            case OLD_THEN_MISSING -> { }
        }
        var checks = journal.orderCreated(fence, CONFIRMATION, EXPECTED);
        var evidence = journal.evidence(ShopApiCheckpoint.ORDER);
        var projection = ShopApiValidationResult.order(evidence, checks).reportingEvidence();

        assertEquals(variant == OrderCase.SUCCESS ? AtfReportStatus.PASS : AtfReportStatus.FAIL, projection.status());
        assertEquals(5, checks.size(), "Order value and trace checks remain intact");
        assertTrue(checks.stream().noneMatch(row -> row.expected().isBlank()), "Missing API values must leave an explicit expected criterion");
        if (variant == OrderCase.OLD_THEN_MISSING || variant == OrderCase.UNRELATED) {
            assertTrue(evidence.isEmpty(), "Missing current capture clears the old checkpoint");
            assertEquals(5, projection.assertions().size(), "Missing evidence retains all failed comparisons");
            assertFalse(checks.stream().anyMatch(AtfAssertion::passed));
        } else {
            assertEquals(1, evidence.size(), "Only the current exchange is bound");
            assertTrue(evidence.getFirst().trace().sequence() > fence);
            int status = variant == OrderCase.SUCCESS ? 201 : variant == OrderCase.TRANSPORT_FAILED ? -1 : 500;
            assertEquals(status, evidence.getFirst().response().status());
            assertEquals(status, projection.exchanges().getFirst().evidence().response().status());
            if (status == -1) assertEquals("Unavailable", evidence.getFirst().response().statusLabel());
        }
        assertEquals("24.02", checks.get(3).expected(), "The independent monetary oracle is preserved");
    }

    static Stream<Arguments> cases() {
        return Stream.of(ShopApiRoutes.GUEST_INVOICE_CREATE, ShopApiRoutes.INVOICE_CREATE)
                .flatMap(route -> Stream.of(OrderCase.values()).map(variant -> Arguments.of(route, variant)));
    }

    @Test
    void clearingOrderDoesNotClearAPaymentSiblingAndDisabledApiNeverWaitsOnPage() {
        var journal = journal("order,payment");
        capture(journal, ShopApiRoutes.PAYMENT_CHECK, 200, "{\"message\":\"Payment was successful\"}");
        journal.paymentConfirmed(0, "Cash on Delivery");
        var payment = journal.evidence(ShopApiCheckpoint.PAYMENT);
        capture(journal, ShopApiRoutes.GUEST_INVOICE_CREATE, 201, SUCCESS);
        journal.orderCreated(0, CONFIRMATION, EXPECTED);
        long fence = journal.sequenceMarker();
        var checks = journal.orderCreated(fence, CONFIRMATION, EXPECTED);

        assertEquals(payment, journal.evidence(ShopApiCheckpoint.PAYMENT), "Sibling selection is immutable");
        assertTrue(journal.evidence(ShopApiCheckpoint.ORDER).isEmpty());
        assertEquals(AtfReportStatus.FAIL, ShopApiValidationResult.order(List.of(), checks).reportingEvidence().status());
        var disabled = new ShopApiNetworkJournal(ShopValidationProfile.UI_ONLY);
        assertTrue(disabled.awaitOrderCreated(null, 10, CONFIRMATION, EXPECTED).isEmpty());
        assertTrue(disabled.selectedEvidence().isEmpty());
    }

    enum OrderCase {
        SUCCESS(false), FIRST_500(false), OLD_THEN_500(true), CURRENT_201_THEN_500(false),
        TRANSPORT_FAILED(true), OLD_THEN_MISSING(true), UNRELATED(true);
        final boolean previous;
        OrderCase(boolean previous) { this.previous = previous; }
    }

    private static ShopApiNetworkJournal journal(String checkpoints) {
        return new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, checkpoints));
    }

    private void capture(ShopApiNetworkJournal journal, String route, int status, String body) {
        var inspector = captures.computeIfAbsent(journal, key -> {
            var value = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of("shop.fixture.test")));
            key.attach(value); return value;
        });
        var observed = new UiNetworkObservation(1, Instant.EPOCH, "POST", "https://shop.fixture.test" + route,
                route, status, Map.of(), Map.of("content-type", "application/json"), "{}", body, "none", "",
                "shop.fixture.test", "COMPONENT_CAPTURE", "POST " + route + "#body=present", status > 0,
                status < 0 ? "net::ERR_CONNECTION_RESET" : "", UiNetworkCapturePolicy.ResourceKind.FETCH);
        assertTrue(inspector.capture(observed.withRequestIdentity(inspector.requestStarted())).isPresent(), "The controlled observation reaches the public subscription");
    }
}

