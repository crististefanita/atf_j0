package atf.autoex.business.purchase;

import java.util.Objects;

/** Purchase order; each semantic stage must finish its gate before the next mutation. */
public final class AutoExPurchaseFlow {
    public interface Port {
        void verifySelectedProduct();
        void addAndConfirmProduct();
        void verifyCartAndProceed();
        void registerOwnedCustomer();
        void reviewCheckout();
        void openPaymentForm();
        void confirmDemoOrder();
        void verifyInvoice();
        void deleteOwnedCustomer();
    }

    public void complete(Port port) {
        Objects.requireNonNull(port, "port");
        port.verifySelectedProduct();
        port.addAndConfirmProduct();
        port.verifyCartAndProceed();
        port.registerOwnedCustomer();
        port.reviewCheckout();
        port.openPaymentForm();
        port.confirmDemoOrder();
        port.verifyInvoice();
        port.deleteOwnedCustomer();
    }
}
