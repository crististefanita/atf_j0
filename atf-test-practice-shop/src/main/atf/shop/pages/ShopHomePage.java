package atf.shop.pages;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;
import java.net.URI;

/** Catalog landing page object. */
@ShopPageContract(screen = ShopScreen.HOME, route = ShopUiRoutes.HOME,
        requiredControls = {ShopControl.HOME_NAVIGATION, ShopControl.HOME_BRAND,
                ShopControl.HOME_BANNER, ShopControl.HOME_SORT,
                ShopControl.HOME_PRICE_RANGE, ShopControl.SEARCH_QUERY,
                ShopControl.HOME_SEARCH_RESET, ShopControl.HOME_SEARCH_SUBMIT,
                ShopControl.HOME_CATEGORY_FILTERS, ShopControl.HOME_BRAND_FILTERS,
                ShopControl.HOME_ECO_FILTER, ShopControl.HOME_PRODUCT_GRID,
                ShopControl.HOME_PRODUCT_NAMES, ShopControl.HOME_PRODUCT_IMAGES,
                ShopControl.HOME_PAGINATION, ShopControl.HOME_FOOTER})
public final class ShopHomePage {
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopHomePage(Page page, ShopLocators locators,
                                PlaywrightActionSequence actions) {
        this.page = page; this.locators = locators; this.actions = actions;
    }

    public void open(URI homeUrl) {
        actions.navigate(page, homeUrl, "catalog home is loaded");
        actions.waitForVisible(locators.searchQuery(page));
        actions.waitForVisible(locators.homeProductGridFirst(page));
    }

    public void search(String productName) {
        var searchQuery = locators.searchQuery(page);
        actions.fill(searchQuery, productName, "product search term is entered");
        actions.pressKey(page, "Enter", searchQuery, "catalog search is submitted");
    }
}


