package atf.autoex.page;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.ui.AutoExCatalogPagePort;
import java.util.Objects;

/** Product search and catalog selection behavior. */
public final class ProductsPage extends AutoExPage {
    private final String expectedProductName;
    private final AutoExCatalogPagePort catalog;

    public ProductsPage(AutoExCatalogPagePort ui, String expectedProductName) {
        super(ui);
        catalog = ui;
        this.expectedProductName = Objects.requireNonNull(expectedProductName, "expectedProductName");
    }

    public void search(String query) {
        fill(AutoExSelectors.SEARCH_INPUT, query, "search query is entered");
        catalog.submitSearch(query, expectedProductName);
    }

    public String firstProductName() {
        return text(AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME, "first matching product name is read");
    }

    public ProductPage openFirstProduct() {
        ui.waitForText(AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME + " >> nth=0",
                expectedProductName, "configured search result is ready");
        clickFirst(AutoExSelectors.PRODUCT_DETAILS_LINK, "first matching product details open");
        return new ProductPage(ui);
    }
}
