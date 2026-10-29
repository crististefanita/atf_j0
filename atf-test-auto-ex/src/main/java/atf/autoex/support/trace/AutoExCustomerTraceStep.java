package atf.autoex.support.trace;

import atf.reporting.contract.AtfGherkinKeyword;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.contract.AtfGherkinKeyword.WHEN;
import static atf.reporting.contract.AtfGherkinKeyword.AND;

/** Observed cart/session outcomes; no synthetic advertisement or failure checkpoints. */
public enum AutoExCustomerTraceStep implements AutoExTraceStep {
    CART_RECALCULATED(WHEN, "adding the same product updates its quantity and calculated total"),
    CART_EMPTY(THEN, "removing the selected product leaves the cart empty"),
    CUSTOMER_SIGNED_IN(WHEN, "the disposable customer signs in with the same identity"),
    CUSTOMER_SIGNED_OUT(WHEN, "the customer signs out before starting a guest cart"),
    CART_SESSION_RESTORED(THEN, "signing in preserves the guest cart and its calculated total"),
    ACCOUNT_DELETION_RESUMED(AND, "the same customer resumes account deletion after the advert cancelled navigation"),
    CLEANUP_RECOVERED(AND, "account cleanup returns from the observed vignette to the store"),
    CLEANUP_DOCUMENT_RECOVERED(AND, "account cleanup reopens the store after its document became unavailable");

    private final String title;
    private final AtfGherkinKeyword keyword;

    AutoExCustomerTraceStep(AtfGherkinKeyword keyword, String title) { this.keyword = keyword; this.title = title; }

    @Override public AtfGherkinKeyword keyword() { return keyword; }
    @Override public String title() { return title; }
}
