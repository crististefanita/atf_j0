package atf.autoex.support.journey;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.business.cart.AutoExCartFlow;
import atf.autoex.flow.cart.AutoExCartUiFlow;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.catalog.AutoExCatalogAssertions;
import atf.autoex.support.checkout.AutoExCheckoutAssertions;
import atf.autoex.support.checkout.AutoExInvoiceAssertions;
import atf.autoex.support.validation.AutoExStoreAssertions;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.page.AccountPage;
import atf.autoex.page.CartPage;
import atf.autoex.page.CheckoutPage;
import atf.autoex.page.HomePage;
import atf.autoex.page.ProductPage;
import atf.autoex.page.ProductsPage;

import java.util.List;
import java.util.ArrayList;

/** One end-to-end business journey on the actual public store. */
public final class AutoExPurchaseJourney {
    private AutoExPurchaseJourney() { }

    public static void execute(AutoExPurchaseSession run) {
        AutoExConfiguration configuration = run.configuration();
        AutoExConfiguration.Catalog product = configuration.catalog();
        String expectedTotal = configuration.expectedCartTotal();
        ProductsPage products = searchConfiguredProduct(run);

        ProductPage details = products.openFirstProduct();
        String detailText = details.details();
        run.checkpoint(AutoExPurchaseTraceStep.PRODUCT_DETAILS,
                List.of(
                        new AtfAssertion("Product name", product.productName(), detailText,
                                detailText.contains(product.productName())),
                        new AtfAssertion("Product price", product.unitPrice(), detailText,
                                AutoExCatalogAssertions.containsExactLine(
                                        detailText, product.unitPrice())),
                        AutoExStoreAssertions.stockAvailability(detailText)));

        details.addQuantityToCart(product.quantity());
        String added = details.addConfirmation();
        if (run.observesApiTraffic()) run.awaitCartExchange(product.productId());
        run.checkpoint(AutoExPurchaseTraceStep.CART_ADD,
                List.of(AutoExStoreAssertions.cartAddition(added)));

        CartPage cart = details.viewCartFromConfirmation();
        AutoExCartFlow.checkout(new AutoExCartUiFlow(cart, product),
                AutoExCheckoutAssertions.expectedLine(product, expectedTotal),
                result -> run.checkpoint(AutoExPurchaseTraceStep.CART_VERIFIED,
                        AutoExCheckoutAssertions.cart(result)));
        AutoExConfiguration.Registration registration = configuration.registration();
        AutoExTestAccount account = AutoExTestAccount.create(registration);
        new AccountPage(run).registerFromCheckout(account);

        cart = new CartPage(run);
        cart.openFromHeader();
        cart.proceedToCheckout();
        AccountPage accountPage = new AccountPage(run);
        String expectedDisplayName = registration.displayName();
        String deliveryAddress = accountPage.deliveryAddress(expectedDisplayName);
        String billingAddress = accountPage.billingAddress(expectedDisplayName);
        CheckoutPage checkout = new CheckoutPage(run);
        var checkoutAssertions = new ArrayList<>(AutoExCheckoutAssertions.address(
                "Delivery", registration, deliveryAddress));
        checkoutAssertions.addAll(AutoExCheckoutAssertions.address(
                "Billing", registration, billingAddress));
        checkoutAssertions.addAll(AutoExCheckoutAssertions.orderLine(
                product, expectedTotal, cart.orderLine(product)));
        String orderTotal = checkout.orderTotal();
        checkoutAssertions.add(new AtfAssertion("Order total", expectedTotal, orderTotal,
                expectedTotal.equals(orderTotal)));
        run.checkpoint(AutoExPurchaseTraceStep.CHECKOUT_REVIEW, checkoutAssertions);
        String paymentHeading = checkout.placeOrder("Synthetic ATF E2E order; demo environment only.");
        run.checkpoint(AutoExPurchaseTraceStep.PAYMENT_FORM,
                List.of(AutoExStoreAssertions.paymentPage(paymentHeading)));
        String confirmation = checkout.payWithDemoCard(configuration.payment());
        String confirmationMessage = checkout.orderConfirmationMessage();
        run.checkpoint(AutoExPurchaseTraceStep.ORDER_CONFIRMED,
                List.of(
                        AutoExStoreAssertions.orderPlaced(confirmation),
                        AutoExStoreAssertions.orderConfirmation(confirmationMessage)));

        var invoice = checkout.downloadInvoice();
        run.checkpoint(AutoExPurchaseTraceStep.INVOICE_DOWNLOADED, AutoExInvoiceAssertions.validate(
                registration.displayName(), expectedTotal, invoice));
        checkout.continueAfterInvoice();
        String deletion = accountPage.accountDeletionResult(account);
        String landingUrl = run.currentUrl();
        String expectedLandingUrl = AutoExRoutes.HOME.resolve(configuration.baseUrl()).toString();
        List<AtfAssertion> finalAssertions = List.of(
                new AtfAssertion("Post-cleanup landing page", expectedLandingUrl,
                        landingUrl, expectedLandingUrl.equals(landingUrl)),
                AutoExStoreAssertions.accountDeleted(deletion));
        run.finalScreenshot(AutoExPurchaseTraceStep.TERMINAL_STATE, finalAssertions);
    }

    /** Opens the store, submits the configured search and validates the public UI result and route. */
    public static ProductsPage searchConfiguredProduct(AutoExPurchaseSession run) {
        AutoExConfiguration.Catalog product = run.configuration().catalog();
        String homeHeading = new HomePage(run).open();
        run.checkpoint(AutoExPurchaseTraceStep.STORE_OPEN,
                List.of(AutoExStoreAssertions.storeAvailability(homeHeading)));

        if (run.observesApiTraffic()) {
            AutoExCatalogApiPrecondition.verifyAndMarkBaseline(product.productName(), product.unitPrice(),
                    run::inspectCatalogApi,
                    assertions -> run.apiCheckpoint(AutoExPurchaseTraceStep.CATALOG_API_PRECONDITION, assertions),
                    run::markCausalApiTrafficBaseline);
        }

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
