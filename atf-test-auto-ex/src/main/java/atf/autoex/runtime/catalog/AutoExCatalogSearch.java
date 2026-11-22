package atf.autoex.runtime.catalog;

import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.catalog.AutoExCatalogAssertions;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** One owned GET and a fresh healthy DOM confirm one catalog click within the original budget. */
public final class AutoExCatalogSearch {
    private static final String CONTENT = """
        state => {
          if (performance.timeOrigin === state.previous || document.readyState === 'loading') return false;
          const visible = e => e && e.getClientRects().length > 0 && !['hidden','collapse'].includes(getComputedStyle(e).visibility);
          const normalize = text => text?.replace(/[\\u200b\\u00ad]/g, '').trim().replace(/\\s+/g, ' ');
          const busy = [...document.body.querySelectorAll('*')].find(e => visible(e) && normalize(e.innerText) === state.busy);
          if (busy) return {kind:'busy',text:normalize(busy.innerText)};
          const first = document.querySelector(state.products);
          const text = normalize(first?.innerText);
          return visible(first) && text === normalize(state.expected) ? {kind:'product',text} : false;
        }
        """;
    private final Page page;
    private final PlaywrightActionSequence actions;
    private final URI products;

    public AutoExCatalogSearch(Page page, PlaywrightActionSequence actions, URI baseUri) {
        this.page = Objects.requireNonNull(page);
        this.actions = Objects.requireNonNull(actions);
        products = AutoExRoutes.PRODUCTS.resolve(Objects.requireNonNull(baseUri));
    }

    public void submit(String query, String expectedProduct) {
        if (query == null || query.isBlank() || expectedProduct == null || expectedProduct.isBlank()) {
            throw new IllegalArgumentException("Search requires a query and expected product");
        }
        try (var document = new AutoExSearchDocument(page)) {
            var readiness = new SearchReadiness(query.trim(), expectedProduct, document);
            try {
                actions.clickAndAwait(new AtfLocator(page.locator(AutoExSelectors.SEARCH_BUTTON), AutoExSelectors.SEARCH_BUTTON),
                    "catalog search is submitted and the configured product is ready", readiness);
            } catch (IllegalStateException failure) {
                throw new IllegalStateException("Catalog search not confirmed; " + readiness.description(), failure);
            }
        }
    }

    /** Native DOM polling follows document replacement without replaying the pointer action. */
    private final class SearchReadiness implements Consumer<PlaywrightActionSequence.ObservationBudget> {
        private final String query;
        private final String expectedProduct;
        private final AutoExSearchDocument document;
        private String location = "<not observed>";
        private String awaiting = "owned search document response";
        private String dom = "<not inspected>";

        private SearchReadiness(String query, String expectedProduct, AutoExSearchDocument document) {
            this.query = query;
            this.expectedProduct = expectedProduct;
            this.document = document;
        }

        @Override public void accept(PlaywrightActionSequence.ObservationBudget budget) {
            try {
                document.awaitResponse(budget);
                location = document.url();
                awaiting = "matching search URL and successful HTTP";
                document.requireSearch(products, query);
                awaiting = "visible first product " + expectedProduct + " in a fresh document, or terminal store state";
                dom = "<no qualifying visible state confirmed>";
                var observed = page.waitForFunction(CONTENT,
                        Map.of("previous", document.previousOrigin(), "busy", AutoExSelectors.STORE_BUSY_MESSAGE,
                            "products", AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME, "expected", expectedProduct),
                        new Page.WaitForFunctionOptions().setTimeout(budget.remainingMilliseconds()));
                try {
                    var content = (Map<?, ?>) observed.jsonValue();
                    dom = (String) content.get("text");
                    if ("busy".equals(content.get("kind")))
                        throw new IllegalStateException("Store unavailable during search: queue full; " + document.description());
                    if (!"product".equals(content.get("kind"))) throw new IllegalStateException("Unexpected search document state");
                } finally { observed.dispose(); }
                document.requireSingleRequest();
                requireCurrentLocation();
                awaiting = "confirmed";
            } finally {
                location = page.url();
                actions.information("Catalog search response", AutoExRoutes.PRODUCTS.path(),
                    "owned GET, new document and configured first product; one click, no replay", description());
            }
        }

        private void requireCurrentLocation() {
            location = page.url();
            if (!AutoExCatalogAssertions.matchesSearchLocation(products, query, location))
                throw new IllegalStateException("Search location changed during document confirmation");
        }

        private String description() {
            return "awaiting=" + awaiting + "; " + document.description() + "; DOM=" + dom + "; observed page URL=" + location;
        }
    }
}
