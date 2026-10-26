package atf.autoex.support.cart;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.page.AccountPage;
import atf.autoex.page.CartPage;
import atf.autoex.page.HomePage;
import atf.autoex.page.ProductsPage;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.checkout.AutoExCheckoutAssertions;
import atf.autoex.support.journey.AutoExPurchaseJourney;
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.support.trace.AutoExCustomerTraceStep;
import atf.autoex.support.validation.AutoExStoreAssertions;

import java.util.List;

/** Distinct real-store cart stories; no checkout/payment repetition for an evidence profile. */
public final class AutoExCartJourney {
    private AutoExCartJourney() { }

    public static void recalculatesAndRemovesProduct(AutoExPurchaseSession run) {
        Catalog configured = run.configuration().catalog();
        Catalog initial = configured.withQuantity(1);
        ProductsPage products = AutoExPurchaseJourney.searchConfiguredProduct(run);
        CartPage cart = addProduct(run, products, initial);
        run.checkpoint(AutoExPurchaseTraceStep.CART_VERIFIED,
                AutoExCheckoutAssertions.orderLine(initial, initial.expectedTotal(), cart.orderLine(initial)));

        new HomePage(run).openProducts();
        products = new ProductsPage(run);
        products.search(configured.searchTerm());
        cart = addProduct(run, products, configured);
        Catalog updated = configured.withQuantity(Math.addExact(initial.quantity(), configured.quantity()));
        run.checkpoint(AutoExCustomerTraceStep.CART_RECALCULATED,
                AutoExCheckoutAssertions.orderLine(updated, updated.expectedTotal(), cart.orderLine(updated)));

        String emptyMessage = cart.removeProductAndReadEmptyCart(updated);
        var emptyAssertion = AutoExStoreAssertions.emptyCart(emptyMessage);
        run.finalScreenshot(AutoExCustomerTraceStep.CART_EMPTY, List.of(emptyAssertion));
    }

    public static void preservesGuestCartAfterLogin(AutoExPurchaseSession run) {
        String heading = new HomePage(run).open();
        run.checkpoint(AutoExPurchaseTraceStep.STORE_OPEN,
                List.of(AutoExStoreAssertions.storeAvailability(heading)));
        AutoExTestAccount account = AutoExTestAccount.create(run.configuration().registration());
        var accountPage = new AccountPage(run);
        accountPage.registerFromStore(account);
        verifyIdentity(run, accountPage, account);
        boolean signedOut = accountPage.logout();
        run.checkpoint(AutoExCustomerTraceStep.CUSTOMER_SIGNED_OUT,
                List.of(new AtfAssertion("Guest session", "login form visible; delete-account action absent",
                        Boolean.toString(signedOut), signedOut)));

        Catalog product = run.configuration().catalog();
        new HomePage(run).openProducts();
        var products = new ProductsPage(run);
        products.search(product.searchTerm());
        CartPage cart = addProduct(run, products, product);
        run.checkpoint(AutoExPurchaseTraceStep.CART_VERIFIED,
                AutoExCheckoutAssertions.orderLine(product, product.expectedTotal(), cart.orderLine(product)));
        accountPage.login(account);
        verifyIdentity(run, accountPage, account);
        cart.openFromHeader();
        run.checkpoint(AutoExCustomerTraceStep.CART_SESSION_RESTORED,
                AutoExCheckoutAssertions.orderLine(product, product.expectedTotal(), cart.orderLine(product)));
        String deletion = accountPage.accountDeletionResult(account);
        run.finalScreenshot(AutoExPurchaseTraceStep.TERMINAL_STATE,
                List.of(AutoExStoreAssertions.accountDeleted(deletion)));
    }

    private static CartPage addProduct(AutoExPurchaseSession run, ProductsPage products, Catalog product) {
        var details = products.openFirstProduct();
        details.addQuantityToCart(product.quantity());
        String confirmation = details.addConfirmation();
        run.checkpoint(AutoExPurchaseTraceStep.CART_ADD,
                List.of(AutoExStoreAssertions.cartAddition(confirmation)));
        return details.viewCartFromConfirmation();
    }

    private static void verifyIdentity(AutoExPurchaseSession run, AccountPage page, AutoExTestAccount account) {
        String identity = page.loggedInName();
        run.checkpoint(AutoExCustomerTraceStep.CUSTOMER_SIGNED_IN,
                List.of(new AtfAssertion("Customer identity", account.signupName(), identity,
                        account.signupName().equals(identity))));
    }
}
