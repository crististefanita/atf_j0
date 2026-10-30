package atf.shop.tests.api.network.causality;

import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.causality.ShopLegacyMarkerFixture;
import atf.reporting.contract.AtfAssertion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopLegacyMarkerComponentTest {
    private static final ShopOrderExpectation EXPECTED = ShopOrderExpectation.fromUi("12.01", 2);
    private static final String CONFIRMATION = "Thanks for your order! Your invoice number is INV-12.";

    @ParameterizedTest(name = "receipt snapshot · inspector={0} · pending response={1}")
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void theReturnedReceiptOwnsTheFenceCapturedAfterPendingDelivery(boolean generic, boolean pending) {
        try (var fixture = new ShopLegacyMarkerFixture(generic)) {
            if (pending) fixture.pendingResponse();
            long marker = fixture.journal().sequenceMarker();
            assertEquals(pending ? 1L : 0L, marker, "The numeric receipt includes every response delivered by the passive barrier");
            fixture.confirm(201);
            var checks = fixture.journal().orderCreated(marker, CONFIRMATION, EXPECTED);
            assertEquals(5, checks.size(), "The current order retains all independent business comparisons");
            assertTrue(checks.stream().allMatch(AtfAssertion::passed), "The returned receipt resolves its own current-order request fence");
            assertEquals(1, fixture.activations(), "The observer never dispatches or repeats Confirm");
            assertTrue(fixture.journal().evidence(ShopApiCheckpoint.ORDER).getFirst().response().body().contains("current-order"),
                    "Selected evidence belongs to the new order rather than the pending response");
        }
    }

    @ParameterizedTest(name = "idempotent numeric window · inspector={0}") @ValueSource(booleans = {false, true})
    void identicalRepeatedMarkersRemainAnIdempotentWindow(boolean generic) {
        try (var fixture = new ShopLegacyMarkerFixture(generic)) {
            long first = fixture.journal().sequenceMarker(); long repeated = fixture.journal().sequenceMarker();
            assertEquals(first, repeated, "An unchanged receipt and request-start position represent the same window");
            fixture.confirm(201);
            assertTrue(fixture.journal().orderCreated(first, CONFIRMATION, EXPECTED).stream().allMatch(AtfAssertion::passed),
                    "Idempotent reads preserve a valid numeric window");
            assertEquals(1, fixture.activations(), "Repeated observation never repeats the mutation");
        }
    }

    @ParameterizedTest(name = "ambiguous numeric window · inspector={0}") @ValueSource(booleans = {false, true})
    void theSameNumericReceiptCannotSilentlyRepresentTwoDifferentRequestWindows(boolean generic) {
        try (var fixture = new ShopLegacyMarkerFixture(generic)) {
            long first = fixture.journal().sequenceMarker();
            var inFlight = fixture.begin();
            long second = fixture.journal().sequenceMarker();
            assertEquals(first, second, "A request start alone does not advance the response receipt counter");
            fixture.complete(inFlight, 201, "old-order");
            assertFalse(fixture.journal().orderCreated(second, CONFIRMATION, EXPECTED).stream().allMatch(AtfAssertion::passed),
                    "An ambiguous numeric window never accepts an already-started response");
            assertTrue(fixture.journal().evidence(ShopApiCheckpoint.ORDER).isEmpty(), "Ambiguous numeric ownership publishes no selected exchange");
            var typed = fixture.journal().requestMarker();
            fixture.confirm(201);
            assertFalse(fixture.journal().orderCreated(first, CONFIRMATION, EXPECTED).stream().allMatch(AtfAssertion::passed),
                    "A numeric collision remains invalid after a new response and cannot silently change its original meaning");
            assertTrue(fixture.journal().orderCreated(typed, CONFIRMATION, EXPECTED).stream().allMatch(AtfAssertion::passed),
                    "The independent typed fence still owns the actual new request");
            assertEquals(1, fixture.activations(), "Fail-closed legacy ownership does not replay Confirm");
        }
    }
}
