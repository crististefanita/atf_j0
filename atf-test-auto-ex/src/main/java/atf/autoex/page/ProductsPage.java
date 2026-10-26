package atf.autoex.page;

import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.journey.AutoExPurchaseSession;

/** Product search and catalog selection behavior. */
public final class ProductsPage extends AutoExPage {
    public ProductsPage(AutoExPurchaseSession run) { super(run); }

    public void search(String query) {
        fill(AutoExSelectors.SEARCH_INPUT, query, "search query is entered");
        click(AutoExSelectors.SEARCH_BUTTON, "catalog search is submitted");
    }

    public String firstProductName() {
        return text(AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME, "first matching product name is read");
    }

    public ProductPage openFirstProduct() {
        run.waitForText(AutoExSelectors.SEARCH_RESULT_PRODUCT_NAME + " >> nth=0",
                run.configuration().catalog().productName(), "configured search result is ready");
        clickFirst(AutoExSelectors.PRODUCT_DETAILS_LINK, "first matching product details open");
        return new ProductPage(run);
    }
}
