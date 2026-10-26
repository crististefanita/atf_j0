package atf.autoex.support.trace;

import atf.reporting.contract.AtfGherkinKeyword;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;

/** Observed cart/session outcomes; no synthetic advertisement or failure checkpoints. */
public enum AutoExCustomerTraceStep implements AutoExTraceStep {
    CART_RECALCULATED("adding the same product updates its quantity and calculated total"),
    CART_EMPTY("removing the selected product leaves the cart empty"),
    CUSTOMER_SIGNED_IN("the disposable customer signs in with the same identity"),
    CUSTOMER_SIGNED_OUT("the customer signs out before starting a guest cart"),
    CART_SESSION_RESTORED("signing in preserves the guest cart and its calculated total"),
    ACCOUNT_DELETION_RESUMED("the same customer resumes account deletion after the advert cancelled navigation"),
    CLEANUP_RECOVERED("account cleanup returns from the observed vignette to the store"),
    CLEANUP_DOCUMENT_RECOVERED("account cleanup reopens the store after its document became unavailable");

    private final String title;

    AutoExCustomerTraceStep(String title) { this.title = title; }

    @Override public AtfGherkinKeyword keyword() { return THEN; }
    @Override public String title() { return title; }
}
