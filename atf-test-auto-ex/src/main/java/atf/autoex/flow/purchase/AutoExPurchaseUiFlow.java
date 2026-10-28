package atf.autoex.flow.purchase;

import atf.autoex.business.purchase.AutoExPurchaseFlow;
import atf.autoex.business.cart.AutoExCartFlow;
import atf.autoex.flow.cart.AutoExCartUiFlow;
import atf.autoex.page.AccountPage;
import atf.autoex.page.CartPage;
import atf.autoex.page.CheckoutPage;
import atf.autoex.page.ProductPage;
import atf.autoex.page.ProductsPage;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.checkout.AutoExCheckoutAssertions;
import atf.autoex.support.checkout.AutoExInvoiceAssertions;
import atf.autoex.support.checkout.AutoExPurchaseAssertions;
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.support.validation.AutoExStoreAssertions;

import java.util.List;
import java.util.Objects;

/** Per-purchase UI adapter; configuration resolution and business stage order live outside it. */
public final class AutoExPurchaseUiFlow implements AutoExPurchaseFlow.Port {
    private final AutoExPurchaseSession run;
    private final ProductsPage products;
    private final AutoExPurchaseInputs inputs;
    private ProductPage details;
    private CartPage cart;
    private AutoExTestAccount account;
    private AccountPage accountPage;
    private CheckoutPage checkout;

    public AutoExPurchaseUiFlow(AutoExPurchaseSession run, ProductsPage products, AutoExPurchaseInputs inputs) {
        this.run = Objects.requireNonNull(run, "run");
        this.products = Objects.requireNonNull(products, "products");
        this.inputs = Objects.requireNonNull(inputs, "inputs");
    }

    @Override public void verifySelectedProduct() {
        details = products.openFirstProduct();
        run.checkpoint(AutoExPurchaseTraceStep.PRODUCT_DETAILS,
                AutoExPurchaseAssertions.product(inputs.product(), details.details()));
    }

    @Override public void addAndConfirmProduct() {
        var product = inputs.product();
        details.addQuantityToCart(product.quantity());
        if (run.observesApiTraffic()) {
            var mutation = run.awaitCartExchange(product.productId());
            var confirmation = mutation.successfulResponse()
                    ? details.addConfirmationAfterResponse() : details.addConfirmationIfVisible();
            run.cartAddCheckpoint(AutoExPurchaseTraceStep.CART_ADD, mutation,
                    List.of(AutoExStoreAssertions.cartMutationConfirmation(mutation.successfulResponse(), confirmation)));
        } else {
            run.checkpoint(AutoExPurchaseTraceStep.CART_ADD,
                    List.of(AutoExStoreAssertions.cartAddition(details.addConfirmation())));
        }
    }

    @Override public void verifyCartAndProceed() {
        cart = details.viewCartFromConfirmation();
        var product = inputs.product();
        AutoExCartFlow.checkout(new AutoExCartUiFlow(cart, product),
                AutoExCheckoutAssertions.expectedLine(product, product.expectedTotal()),
                result -> run.checkpoint(AutoExPurchaseTraceStep.CART_VERIFIED, AutoExCheckoutAssertions.cart(result)));
    }

    @Override public void registerOwnedCustomer() {
        account = AutoExTestAccount.create(inputs.registration());
        new AccountPage(run).registerFromCheckout(account);
    }

    @Override public void reviewCheckout() {
        cart = new CartPage(run);
        cart.openFromHeader();
        cart.proceedToCheckout();
        accountPage = new AccountPage(run);
        String name = inputs.registration().displayName();
        String delivery = accountPage.deliveryAddress(name);
        String billing = accountPage.billingAddress(name);
        checkout = new CheckoutPage(run);
        var line = cart.orderLine(inputs.product());
        String total = checkout.orderTotal();
        run.checkpoint(AutoExPurchaseTraceStep.CHECKOUT_REVIEW, AutoExPurchaseAssertions.checkout(
                inputs.product(), inputs.registration(), delivery, billing, line, total));
    }

    @Override public void openPaymentForm() {
        String heading = checkout.placeOrder("Synthetic ATF E2E order; demo environment only.");
        run.checkpoint(AutoExPurchaseTraceStep.PAYMENT_FORM, List.of(AutoExStoreAssertions.paymentPage(heading)));
    }

    @Override public void confirmDemoOrder() {
        String confirmation = checkout.payWithDemoCard(inputs.payment());
        String message = checkout.orderConfirmationMessage();
        run.checkpoint(AutoExPurchaseTraceStep.ORDER_CONFIRMED, List.of(
                AutoExStoreAssertions.orderPlaced(confirmation), AutoExStoreAssertions.orderConfirmation(message)));
    }

    @Override public void verifyInvoice() {
        var invoice = checkout.downloadInvoice();
        run.checkpoint(AutoExPurchaseTraceStep.INVOICE_DOWNLOADED, AutoExInvoiceAssertions.validate(
                inputs.registration().displayName(), inputs.product().expectedTotal(), invoice));
    }

    @Override public void deleteOwnedCustomer() {
        checkout.continueAfterInvoice();
        String deletion = accountPage.accountDeletionResult(account);
        run.finalScreenshot(AutoExPurchaseTraceStep.TERMINAL_STATE, AutoExPurchaseAssertions.cleanup(
                inputs.cleanupLanding().toString(), run.currentUrl(), deletion));
    }
}
