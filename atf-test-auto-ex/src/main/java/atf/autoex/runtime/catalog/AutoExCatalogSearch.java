package atf.autoex.runtime.catalog;

import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.catalog.AutoExCatalogAssertions;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

/** One catalog submission; URL and visible first result confirm completion within the same budget. */
public final class AutoExCatalogSearch {
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
        var readiness = new SearchReadiness(query.trim(), expectedProduct);
        try {
            actions.clickAndAwait(new AtfLocator(page.locator(AutoExSelectors.SEARCH_BUTTON), AutoExSelectors.SEARCH_BUTTON),
                    "catalog search is submitted and the configured product is ready", readiness);
        } catch (IllegalStateException failure) {
            throw new IllegalStateException("Catalog search not confirmed; " + readiness.description(), failure);
        }
    }

    /** Native locator polling follows document replacement without replaying the pointer action. */
    private final class SearchReadiness implements Consumer<PlaywrightActionSequence.ObservationBudget> {
        private final String query;
        private final String expectedProduct;
        private final Locator result = page.locator(AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME).first();
        private String location = "<not observed>";
        private String awaiting = "matching search URL";

        private SearchReadiness(String query, String expectedProduct) {
            this.query = query;
            this.expectedProduct = expectedProduct;
        }

        @Override public void accept(PlaywrightActionSequence.ObservationBudget budget) {
            page.waitForURL(url -> AutoExCatalogAssertions.matchesSearchLocation(products, query, url),
                    new Page.WaitForURLOptions().setWaitUntil(WaitUntilState.COMMIT)
                            .setTimeout(budget.remainingMilliseconds()));
            location = page.url();
            awaiting = "visible first product " + expectedProduct;
            result.and(page.getByText(expectedProduct, new Page.GetByTextOptions().setExact(true)))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(budget.remainingMilliseconds()));
            location = page.url();
            if (!AutoExCatalogAssertions.matchesSearchLocation(products, query, location)) {
                throw new IllegalStateException("Search location changed during product confirmation");
            }
        }

        private String description() {
            return "awaiting=" + awaiting + "; last confirmed url=" + location;
        }
    }
}
