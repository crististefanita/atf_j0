package atf.shop.flow;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopConfiguration;
import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopHomePage;
import atf.shop.pages.ShopProductPage;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Page;

/** Catalog flow: deterministic search followed by product contract checks. */
public final class ShopCatalogFlow {
    private final ShopLocators locators;

    public ShopCatalogFlow(ShopLocators locators) {
        this.locators = locators;
    }

    public void searchAndOpenConfiguredProduct(Page page, PlaywrightActionSequence actions,
                                                ShopConfiguration configuration) {
        ShopHomePage home = new ShopHomePage(page, locators, actions);
        home.open(configuration);
        home.search(configuration.productName());
        AtfLocator configuredProduct = configuration.usesDynamicProductPath()
                ? locators.productLinkFirst(page, configuration.productName())
                : locators.productLinkByPath(page, configuration.productPath());
        boolean configuredPathFound = configuration.usesDynamicProductPath()
                || configuredProduct.locator().count() > 0;
        AtfLocator product = configuredProduct;
        String selectedPath = product.locator().getAttribute("href");
        if (!configuredPathFound || !configuration.matchesProductPath(selectedPath)) {
            throw new IllegalStateException("Configured productPath '" + configuration.productPath()
                    + "' was not found for product '" + configuration.productName()
                    + "'; selected route='" + selectedPath + "'");
        }
        actions.waitForVisible(product);
        actions.click(product, "matching product opens from search results");
        new ShopProductPage(page, locators, actions).assertLoaded(configuration);
    }
}


