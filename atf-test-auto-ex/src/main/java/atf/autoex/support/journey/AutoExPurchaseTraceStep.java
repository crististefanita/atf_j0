package atf.autoex.support.journey;

import atf.reporting.contract.AtfGherkinKeyword;
import atf.autoex.support.trace.AutoExTraceStep;

/** Stable business vocabulary for every reported checkpoint in the AutoEx purchase journey. */
public enum AutoExPurchaseTraceStep implements AutoExTraceStep {
    STORE_OPEN(AtfGherkinKeyword.GIVEN, "a new browser session opens the public store"),
    CATALOG_API_PRECONDITION(AtfGherkinKeyword.GIVEN,
            "the catalog API confirms the configured product before UI purchase"),
    PRODUCT_SEARCH(AtfGherkinKeyword.WHEN,
            "the customer searches the catalog for the configured product"),
    PRODUCT_DETAILS(AtfGherkinKeyword.THEN,
            "product details show its price and availability"),
    CART_ADD(AtfGherkinKeyword.WHEN, "the selected quantity is added to the cart"),
    CART_VERIFIED(AtfGherkinKeyword.THEN,
            "the cart preserves the product, quantity and calculated total"),
    ACCOUNT_CREATED(AtfGherkinKeyword.THEN, "the disposable customer account is created"),
    CHECKOUT_REVIEW(AtfGherkinKeyword.THEN,
            "checkout shows the registered addresses and the selected order"),
    PAYMENT_FORM(AtfGherkinKeyword.THEN, "the demo payment form is displayed"),
    ORDER_CONFIRMED(AtfGherkinKeyword.THEN, "the demo checkout confirms the order"),
    INVOICE_DOWNLOADED(AtfGherkinKeyword.THEN,
            "the completed order provides a downloadable invoice"),
    ACCOUNT_DELETED(AtfGherkinKeyword.THEN, "the disposable customer account is deleted"),
    TERMINAL_STATE(AtfGherkinKeyword.THEN,
            "the deleted-account flow returns to the store");

    private final AtfGherkinKeyword keyword;
    private final String title;

    AutoExPurchaseTraceStep(AtfGherkinKeyword keyword, String title) {
        this.keyword = keyword;
        this.title = title;
    }

    public AtfGherkinKeyword keyword() { return keyword; }
    public String title() { return title; }
}
