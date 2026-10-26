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
                                                ShopConfiguration.CatalogSelection selection) {
        ShopHomePage home = new ShopHomePage(page, locators, actions);
        home.open(selection.homeUrl());
        home.search(selection.productName());
        AtfLocator configuredProduct = selection.usesDynamicProductPath()
                ? locators.productLinkFirst(page, selection.productName())
                : locators.productLinkByPath(page, selection.productPath());
        boolean configuredPathFound = selection.usesDynamicProductPath()
                || configuredProduct.locator().count() > 0;
        AtfLocator product = configuredProduct;
        String selectedPath = product.locator().getAttribute("href");
        if (!configuredPathFound || !selection.matchesProductPath(selectedPath)) {
            throw new IllegalStateException("Configured productPath '" + selection.productPath()
                    + "' was not found for product '" + selection.productName()
                    + "'; selected route='" + selectedPath + "'");
        }
        actions.waitForVisible(product);
        actions.click(product, "matching product opens from search results");
        new ShopProductPage(page, locators, actions).assertLoaded(selection.productName());
    }
}


