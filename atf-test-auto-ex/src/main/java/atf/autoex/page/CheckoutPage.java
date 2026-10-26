package atf.autoex.page;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.business.invoice.AutoExInvoiceSnapshot;
import atf.autoex.flow.checkout.AutoExInvoiceReader;
import atf.autoex.support.journey.AutoExPurchaseSession;
import com.microsoft.playwright.Download;

/** Checkout review, demo payment, order confirmation and invoice download. */
public final class CheckoutPage extends AutoExPage {
    private final AutoExPurchaseSession run;
    public CheckoutPage(AutoExPurchaseSession run) { super(run); this.run = run; }

    public String orderTotal() {
        return text(AutoExSelectors.ORDER_TOTAL, "checkout total amount is read");
    }

    public String placeOrder(String comment) {
        fill(AutoExSelectors.ORDER_COMMENT, comment, "synthetic order comment is entered");
        click(AutoExSelectors.PLACE_ORDER, "order review is submitted to demo payment");
        boolean vignetteDismissed = run.dismissGoogleVignetteIfPresent();
        if (vignetteDismissed && !visible(AutoExSelectors.CARD_NAME)
                && visible(AutoExSelectors.PLACE_ORDER)) {
            click(AutoExSelectors.PLACE_ORDER, "checkout continues after dismissing the visible vignette");
            run.dismissGoogleVignetteIfPresent();
        }
        return text(AutoExSelectors.PAYMENT_HEADING, "demo payment page is displayed");
    }

    public String payWithDemoCard(AutoExConfiguration.Payment payment) {
        fill(AutoExSelectors.CARD_NAME, payment.nameOnCard(), "demo cardholder name is entered");
        fill(AutoExSelectors.CARD_NUMBER, payment.cardNumber(), "demo card number is entered");
        fill(AutoExSelectors.CARD_CVC, payment.cvc(), "demo card security code is entered");
        fill(AutoExSelectors.CARD_EXPIRY_MONTH, payment.expiryMonth(), "demo card month is entered");
        fill(AutoExSelectors.CARD_EXPIRY_YEAR, payment.expiryYear(), "demo card year is entered");
        click(AutoExSelectors.PAY_AND_CONFIRM, "demo order payment is confirmed");
        return text(AutoExSelectors.ORDER_SUCCESS, "demo order confirmation is read");
    }

    public String orderConfirmationMessage() {
        return text(AutoExSelectors.ORDER_CONFIRMATION, "order confirmation message is read");
    }

    public AutoExInvoiceSnapshot downloadInvoice() {
        Download download = run.page().waitForDownload(
                () -> click(AutoExSelectors.DOWNLOAD_INVOICE, "invoice download is requested"));
        return AutoExInvoiceReader.read(download.suggestedFilename(), download.path());
    }

    public void continueAfterInvoice() {
        click(AutoExSelectors.CONTINUE, "invoice review returns to the store");
    }
}
