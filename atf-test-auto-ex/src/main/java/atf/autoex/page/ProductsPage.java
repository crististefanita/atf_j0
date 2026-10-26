package atf.autoex.page;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.ui.AutoExUiActions;
import java.util.Objects;

/** Product search and catalog selection behavior. */
public final class ProductsPage extends AutoExPage {
    private final String expectedProductName;

    public ProductsPage(AutoExUiActions ui, String expectedProductName) {
        super(ui);
        this.expectedProductName = Objects.requireNonNull(expectedProductName, "expectedProductName");
    }

    public void search(String query) {
        fill(AutoExSelectors.SEARCH_INPUT, query, "search query is entered");
        click(AutoExSelectors.SEARCH_BUTTON, "catalog search is submitted");
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
