package atf.autoex.support.journey;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.business.purchase.AutoExPurchaseFlow;
import atf.autoex.flow.purchase.AutoExPurchaseInputs;
import atf.autoex.flow.purchase.AutoExPurchaseUiFlow;
import atf.autoex.support.catalog.AutoExCatalogAssertions;
import atf.autoex.support.validation.AutoExStoreAssertions;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.page.HomePage;
import atf.autoex.page.ProductsPage;

import java.util.List;

/** One end-to-end business journey on the actual public store. */
public final class AutoExPurchaseJourney {
    private AutoExPurchaseJourney() { }

    public static void execute(AutoExPurchaseSession run) {
        AutoExConfiguration configuration = run.configuration();
        var inputs = new AutoExPurchaseInputs(configuration.catalog(), configuration.registration(),
                configuration.payment(), AutoExRoutes.HOME.resolve(configuration.baseUrl()));
        ProductsPage products = searchConfiguredProduct(run);
        new AutoExPurchaseFlow().complete(new AutoExPurchaseUiFlow(run, products, inputs));
    }
    /** Opens the store, submits the configured search and validates the public UI result and route. */
    public static ProductsPage searchConfiguredProduct(AutoExPurchaseSession run) {
        AutoExConfiguration.Catalog product = openConfiguredStore(run);

        if (run.observesApiTraffic()) {
            AutoExCatalogApiPrecondition.verifyAndMarkBaseline(product.productName(), product.unitPrice(),
                    run::inspectCatalogApi,
                    assertions -> run.apiCheckpoint(AutoExPurchaseTraceStep.CATALOG_API_PRECONDITION, assertions),
                    run::markCausalApiTrafficBaseline);
        }

        return searchCatalog(run, product);
    }

    /** UI-only catalog path for journeys that do not own API observation. */
    public static ProductsPage searchConfiguredProduct(AutoExCatalogSearchPort run) {
        AutoExConfiguration.Catalog product = openConfiguredStore(run);
        return searchCatalog(run, product);
    }

    private static AutoExConfiguration.Catalog openConfiguredStore(AutoExCatalogSearchPort run) {
        AutoExConfiguration.Catalog product = run.configuration().catalog();
        String homeHeading = new HomePage(run).open();
        run.checkpoint(AutoExPurchaseTraceStep.STORE_OPEN,
                List.of(AutoExStoreAssertions.storeAvailability(homeHeading)));
        return product;
    }

    private static ProductsPage searchCatalog(AutoExCatalogSearchPort run, AutoExConfiguration.Catalog product) {
        new HomePage(run).openProducts();
        ProductsPage products = new ProductsPage(run, product.productName());
        products.search(product.searchTerm());
        String resultName = products.firstProductName();
        String searchUrl = run.currentUrl();
        run.checkpoint(AutoExPurchaseTraceStep.PRODUCT_SEARCH,
                List.of(
                new AtfAssertion("Search product", product.productName(), resultName,
                        product.productName().equals(resultName)),
                AutoExCatalogAssertions.searchLocation(
                        run.configuration().baseUrl(), product.searchTerm(), searchUrl)));
        return products;
    }
}
