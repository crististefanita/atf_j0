package atf.shop.flow;

import atf.shop.pages.ShopHomePage;
import atf.shop.flow.catalog.ShopCatalogPurchaseFlow;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.pages.ShopProductPage;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.api.validation.ShopApiValidationResult;
import atf.shop.support.validation.ShopOrderExpectation;

import java.util.List;

import static atf.reporting.audit.ReportAuditChecks.check;

/** Catalog-side business stage: home, product selection and add-to-cart. */
final class ShopCatalogPurchaseStage implements ShopCatalogPurchaseFlow.Port {
    private final ShopPurchaseJourneyContext.CatalogInputs context;
    private ShopHomePage home;
    private ShopProductPage product;
    private long cartAdditionFence;
    private ShopOrderExpectation expectedOrder;
    ShopCatalogPurchaseStage(ShopPurchaseJourneyContext.CatalogInputs context) {
        this.context = java.util.Objects.requireNonNull(context, "context");
    }

    PreparedCart prepareCart() {
        new ShopCatalogPurchaseFlow().prepareCart(this, context.checkpointPublisher()::report);
        return new PreparedCart(cartAdditionFence, expectedOrder);
    }

    @Override public ShopPurchaseCheckpoint openHome() {
        var page = context.page();
        var locators = context.locators();
        var selection = context.selection();
        home = new ShopHomePage(page, locators, context.actions());
        home.open(selection.homeUrl(), context.catalogEndpoint());
        context.assertVisible(locators.homeNavigation(page), locators.homeBrand(page),
                locators.homeBanner(page));
        return context.checkpoint(ShopPurchaseTraceStep.CATALOG_HOME,
                context.homeScreenValidator().structure(page),
                context.homeScreenValidator().structureStyle(page), List.of());
    }

    @Override public ShopPurchaseCheckpoint inspectFilters() {
        var page = context.page();
        var locators = context.locators();

        context.assertVisible(locators.homeSort(page), locators.homePriceRange(page),
                locators.searchQuery(page), locators.homeSearchReset(page),
                locators.homeSearchSubmit(page), locators.homeCategoryFilters(page),
                locators.homeBrandFilters(page), locators.homeEcoFilter(page));
        return context.checkpoint(ShopPurchaseTraceStep.HOME_FILTERS,
                context.homeScreenValidator().filters(page),
                context.homeScreenValidator().filterIntegrity(page), List.of());
    }

    @Override public ShopPurchaseCheckpoint inspectProductGrid() {
        var page = context.page();
        var locators = context.locators();

        context.assertVisible(locators.homeProductGrid(page), locators.homeProductNames(page),
                locators.homeProductImages(page), locators.homePagination(page), locators.homeFooter(page));
        return context.checkpoint(ShopPurchaseTraceStep.HOME_PRODUCT_GRID,
                context.homeScreenValidator().productGrid(page),
                context.homeScreenValidator().productGridIntegrity(page), List.of());
    }

    @Override public ShopPurchaseCheckpoint openSelectedProduct() {
        var page = context.page();
        var actions = context.actions();
        var locators = context.locators();
        var selection = context.selection();

        home.search(selection.productName());
        var productLink = selection.usesDynamicProductPath()
                ? locators.productLinkFirst(page, selection.productName())
                : locators.productLinkByPath(page, selection.productPath());
        boolean configuredProductPathFound = selection.usesDynamicProductPath()
                || productLink.locator().count() > 0;
        if (!configuredProductPathFound) {
            throw new IllegalStateException("Configured Practice Shop product route was not found: "
                    + selection.productPath() + "; productName=" + selection.productName());
        }
        String selectedProductPath = productLink.locator().getAttribute("href");
        if (selectedProductPath == null || selectedProductPath.isBlank()) {
            throw new IllegalStateException("Selected Practice Shop product has no href: "
                    + productLink.selector());
        }
        actions.waitForVisible(productLink);
        actions.click(productLink, "matching product opens from search results");

        product = new ShopProductPage(page, locators, actions);
        String productName = product.assertLoaded(selection.productName());
        var productApiAssertions = context.apiNetwork().productReady(
                selection.productName(), selectedProductPath);
        var productEvidence = context.apiNetwork().evidence(ShopApiCheckpoint.PRODUCT);
        var apiValidation = ShopApiValidationResult.product(productEvidence, productApiAssertions)
                .reportingEvidence();
        return context.checkpoint(ShopPurchaseTraceStep.PRODUCT_PAGE,
                context.pageValidator().validate(page, ShopProductPage.class,
                        selection.productName()),
                List.of(
                        check("Product name", selection.productName(), productName,
                                productName.equalsIgnoreCase(selection.productName())),
                        check("Configured product route", selection.productPathExpectation(),
                                selectedProductPath,
                                configuredProductPathFound && selection.matchesProductPath(selectedProductPath)),
                        check("Selected product route loaded", selectedProductPath, page.url(),
                                selectedProductPath != null && page.url().contains(selectedProductPath))),
                List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().product(page))
                .withApiValidation(apiValidation);
    }

    @Override public void addConfiguredQuantity() {
        expectedOrder = product.orderExpectation(context.selection().quantity());
        cartAdditionFence = context.apiNetwork().sequenceMarker();
        product.addConfiguredQuantity(context.selection().quantity());
    }

    record PreparedCart(long additionFence, ShopOrderExpectation expectedOrder) { }
}
