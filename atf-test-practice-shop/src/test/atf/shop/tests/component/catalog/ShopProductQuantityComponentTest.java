package atf.shop.tests.component.catalog;

import atf.core.reporting.contract.AtfReportStatus;
import atf.shop.support.validation.ShopPurchaseScreenValidator;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.quantity.ShopQuantityFixture;
import atf.shop.tests.support.quantity.ShopQuantityFixture.State;
import com.microsoft.playwright.TimeoutError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopProductQuantityComponentTest {
    @ParameterizedTest @EnumSource(value = State.class, names = {"AVAILABLE", "LATE_AVAILABLE"})
    void configuredQuantityIsObservedBeforeExactlyOneCartAddition(State state) {
        try (var fixture = new ShopQuantityFixture(state)) {
            assertEquals("Pliers", fixture.product().assertLoaded("Pliers"));
            assertTrue(new ShopPurchaseScreenValidator(fixture.locators()).product(fixture.page())
                    .stream().allMatch(row -> row.passed()));
            fixture.product().addConfiguredQuantity(2);
            assertEquals("2", fixture.addedQuantity());
            assertEquals(1, fixture.fills());
            assertEquals(1, fixture.additions());
            assertTrue(fixture.actions().snapshot().stream().anyMatch(row ->
                    row.operation().equals("Expect value") && row.observed().equals("2")));
        }
    }

    @ParameterizedTest @EnumSource(value = State.class, names = {"OUT_OF_STOCK", "LATE_OUT_OF_STOCK"})
    void settledOutOfStockPublishesAnExplicitComparisonAndNeverMutates(State state) {
        try (var fixture = new ShopQuantityFixture(state)) {
            assertEquals("Pliers", fixture.product().assertLoaded("Pliers"));
            var availability = new ShopPurchaseScreenValidator(fixture.locators()).product(fixture.page())
                    .stream().filter(row -> row.name().equals("Product availability")).findFirst().orElseThrow();
            assertFalse(availability.passed());
            assertEquals("in stock; quantity and add-to-cart enabled", availability.expected(),
                    "The stock precondition remains independent of the observed state");
            assertEquals("out of stock; quantity enabled=false; add-to-cart enabled=false", availability.actual(),
                    "The report retains the explicit observed stock and control state");
            var failure = assertThrows(IllegalStateException.class, () -> fixture.product().addConfiguredQuantity(2));
            assertTrue(failure.getMessage().contains("out of stock"));
            assertEquals(0, fixture.fills());
            assertEquals(0, fixture.additions());
        }
    }

    @Test
    void unresolvedDisabledControlKeepsTheExistingNativeDeadline() {
        try (var fixture = new ShopQuantityFixture(State.UNRESOLVED)) {
            var failure = assertThrows(TimeoutError.class, () -> fixture.product().assertLoaded("Pliers"));
            assertTrue(failure.getMessage().contains("Timeout 500ms exceeded"),
                    "Fixture preparation does not enlarge the native product-readiness deadline");
            assertEquals(0, fixture.fills());
            assertEquals(0, fixture.additions());
        }
    }

    @Test
    void clampedValueStopsBeforeAddToCartAndPreservesTheFailedAction() {
        try (var fixture = new ShopQuantityFixture(State.AVAILABLE)) {
            fixture.product().assertLoaded("Pliers");
            assertThrows(AssertionError.class, () -> fixture.product().addConfiguredQuantity(100));
            assertEquals(0, fixture.additions());
            var last = fixture.actions().snapshot().getLast();
            assertEquals("Expect value", last.operation());
            assertEquals(AtfReportStatus.FAIL, last.status());
        }
    }

    @Test
    void stockLostAfterTheProductCheckpointStopsBeforeFillAndAdd() {
        try (var fixture = new ShopQuantityFixture(State.AVAILABLE)) {
            fixture.product().assertLoaded("Pliers");
            fixture.removeStock();
            assertThrows(IllegalStateException.class, () -> fixture.product().addConfiguredQuantity(2));
            assertEquals(0, fixture.fills());
            assertEquals(0, fixture.additions());
        }
    }

    @Test
    void invalidQuantityIsRejectedBeforeBrowserMutation() {
        try (var fixture = new ShopQuantityFixture(State.AVAILABLE)) {
            assertThrows(IllegalArgumentException.class, () -> fixture.product().addConfiguredQuantity(0));
            assertEquals(0, fixture.fills());
            assertEquals(0, fixture.additions());
        }
    }
}
