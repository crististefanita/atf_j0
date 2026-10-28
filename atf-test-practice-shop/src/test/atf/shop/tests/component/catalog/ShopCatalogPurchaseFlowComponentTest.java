package atf.shop.tests.component.catalog;

import atf.reporting.contract.AtfAssertion;
import atf.shop.flow.catalog.ShopCatalogPurchaseFlow;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@ShopComponentTest
class ShopCatalogPurchaseFlowComponentTest {
    private static final List<ShopPurchaseTraceStep> SCREENS = List.of(ShopPurchaseTraceStep.CATALOG_HOME,
            ShopPurchaseTraceStep.HOME_FILTERS, ShopPurchaseTraceStep.HOME_PRODUCT_GRID, ShopPurchaseTraceStep.PRODUCT_PAGE);

    @Test
    void publishesEveryScreenBeforeSelectionAndCartMutation() {
        var calls = new ArrayList<String>();
        var port = new Probe(calls, -1, null, null);
        new ShopCatalogPurchaseFlow().prepareCart(port, checkpoint -> calls.add("publish:" + checkpoint.title()));
        var expected = new ArrayList<String>();
        for (var step : SCREENS) { expected.add("observe:" + step.title()); expected.add("publish:" + step.title()); }
        expected.add("add");
        assertEquals(expected, calls);
    }

    @Test
    void eachWrongOrMissingObservationPublishesFailAndPreventsLaterActions() {
        for (int screen = 0; screen < SCREENS.size(); screen++) {
            for (String observed : List.of("", "wrong value")) {
                var calls = new ArrayList<String>();
                var published = new ArrayList<ShopPurchaseCheckpoint>();
                var port = new Probe(calls, screen, observed, null);
                var failure = assertThrows(IllegalStateException.class,
                        () -> new ShopCatalogPurchaseFlow().prepareCart(port, published::add));
                assertEquals("Page validation failed: " + SCREENS.get(screen).title(), failure.getMessage());
                assertEquals(screen + 1, published.size());
                assertEquals(observed, published.getLast().assertions().iterator().next().actual());
                assertEquals(false, published.getLast().passed());
                assertEquals(SCREENS.subList(0, screen + 1).stream().map(s -> "observe:" + s.title()).toList(), calls);
            }
        }
    }

    @Test
    void timeoutsAndPublicationFailuresKeepTheirIdentityAndNeverAddToCart() {
        for (int screen = 0; screen < SCREENS.size(); screen++) {
            var timeout = new IllegalStateException("missing href or observation timeout");
            var calls = new ArrayList<String>();
            var port = new Probe(calls, screen, null, timeout);
            var published = new ArrayList<ShopPurchaseCheckpoint>();
            assertSame(timeout, assertThrows(IllegalStateException.class,
                    () -> new ShopCatalogPurchaseFlow().prepareCart(port, published::add)));
            assertEquals(screen, published.size());
            assertEquals(screen + 1, calls.size());

            calls.clear();
            var publication = new IllegalStateException("publisher failed");
            var good = new Probe(calls, -1, null, null);
            int target = screen;
            int[] publicationCount = {0};
            assertSame(publication, assertThrows(IllegalStateException.class,
                    () -> new ShopCatalogPurchaseFlow().prepareCart(good, checkpoint -> {
                        if (publicationCount[0]++ == target) throw publication;
                    })));
            assertEquals(screen + 1, calls.size());
        }
    }

    private record Probe(List<String> calls, int failureIndex, String observed, RuntimeException failure)
            implements ShopCatalogPurchaseFlow.Port {
        private ShopPurchaseCheckpoint observe(int index) {
            var step = SCREENS.get(index);
            calls.add("observe:" + step.title());
            if (index == failureIndex && failure != null) throw failure;
            String actual = index == failureIndex ? observed : "configured value";
            return new ShopPurchaseCheckpoint(step.keyword(), step.title(), List.of(
                    new AtfAssertion("Screen contract", "configured value", actual, "configured value".equals(actual))));
        }
        @Override public ShopPurchaseCheckpoint openHome() { return observe(0); }
        @Override public ShopPurchaseCheckpoint inspectFilters() { return observe(1); }
        @Override public ShopPurchaseCheckpoint inspectProductGrid() { return observe(2); }
        @Override public ShopPurchaseCheckpoint openSelectedProduct() { return observe(3); }
        @Override public void addConfiguredQuantity() { calls.add("add"); }
    }
}
