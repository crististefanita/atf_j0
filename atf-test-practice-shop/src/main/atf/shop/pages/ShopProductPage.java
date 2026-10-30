package atf.shop.pages;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopPageContract;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.support.validation.ShopProductAvailability;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;

/** Product detail page object. */
@ShopPageContract(screen = ShopScreen.PRODUCT, route = ShopUiRoutes.PRODUCT_PATTERN,
        requiredControls = {ShopControl.PRODUCT_NAME, ShopControl.PRODUCT_PRICE,
                ShopControl.PRODUCT_DESCRIPTION, ShopControl.PRODUCT_QUANTITY,
                ShopControl.ADD_TO_CART})
public final class ShopProductPage {
    private final java.net.URI apiEndpoint;
    private final Page page;
    private final ShopLocators locators;
    private final PlaywrightActionSequence actions;

    public ShopProductPage(Page page, ShopLocators locators,
                                   PlaywrightActionSequence actions) {
        this(page, locators, actions, atf.shop.config.api.ShopApiConfiguration.defaults().endpoint());
    }

    public ShopProductPage(Page page, ShopLocators locators, PlaywrightActionSequence actions, java.net.URI apiEndpoint) {
        this.page = page; this.locators = locators; this.actions = actions;
        this.apiEndpoint = java.util.Objects.requireNonNull(apiEndpoint);
    }

    public String assertLoaded(String expectedProductName) {
        actions.waitForVisible(locators.productName(page));
        String productName = actions.expectText(locators.productName(page), expectedProductName);
        actions.assertVisible(locators.productPrice(page));
        actions.assertVisible(locators.productDescription(page));
        actions.assertVisible(locators.productQuantity(page));
        actions.waitForVisible(locators.productAvailability(page));
        actions.assertVisible(locators.addToCart(page));
        return productName;
    }

    public ShopOrderExpectation orderExpectation(int quantity) {
        return ShopOrderExpectation.fromUi(actions.getText(locators.productPrice(page),
                "unit price is captured before adding the configured quantity"), quantity);
    }

    public void addConfiguredQuantity(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
        actions.waitForVisible(locators.productAvailability(page));
        ShopProductAvailability.observe(page, locators).requirePurchasable();
        actions.fill(locators.productQuantity(page), Integer.toString(quantity),
                "configured product quantity is set");
        actions.expectValue(locators.productQuantity(page), Integer.toString(quantity));
        String productId = atf.shop.support.api.ShopApiExchangeSelector.productIdFromPath(java.net.URI.create(page.url()).getPath());
        new atf.shop.support.cart.ShopCartAdditionConfirmation(page, apiEndpoint, productId, quantity)
                .activate(actions, locators.addToCart(page));
    }
}


