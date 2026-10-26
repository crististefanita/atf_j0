package atf.shop.support;

import atf.api.reporting.contract.AtfGherkinKeyword;

/**
 * Stable vocabulary for the guest purchase trace.
 *
 * <p>The enum owns only static reporting semantics. Runtime values such as
 * product, quantity, route and API evidence remain in the flow assertions.</p>
 */
public enum ShopPurchaseTraceStep {
    GUEST_CONFIGURATION_READY(AtfGherkinKeyword.GIVEN,
            "the typed guest checkout configuration is ready", false),
    AUTHENTICATED_FIXTURE_READY(AtfGherkinKeyword.GIVEN,
            "the pre-registered Practice Shop account fixture is loaded", false),
    NEW_ACCOUNT_REGISTERED(AtfGherkinKeyword.GIVEN, "a new account is registered", false),
    CLASS_RUN_ACCOUNT_REUSED(AtfGherkinKeyword.GIVEN,
            "the account registered earlier in this class-run is reused", false),
    SUITE_ACCOUNT_REUSED(AtfGherkinKeyword.GIVEN,
            "the account registered earlier in this suite is reused", false),
    LOCAL_ACCOUNT_FIXTURE_LOADED(AtfGherkinKeyword.GIVEN,
            "the configured local account fixture is loaded", false),
    ACCOUNT_SESSION_CONFIRMED(AtfGherkinKeyword.AND,
            "the account is signed in and its session is confirmed", false),
    ACCOUNT_SETUP_FAILED(AtfGherkinKeyword.GIVEN,
            "authenticated account setup fails before the purchase flow", false),
    CATALOG_HOME(AtfGherkinKeyword.GIVEN,
            "catalog home structure displays navigation, branding and banner", true),
    HOME_FILTERS(AtfGherkinKeyword.AND,
            "catalog home filters expose sorting, price, search and category controls", true),
    HOME_PRODUCT_GRID(AtfGherkinKeyword.AND,
            "catalog home renders product cards, pagination and footer", true),
    PRODUCT_PAGE(AtfGherkinKeyword.AND,
            "product page displays the configured product and ready-to-buy fields", true),
    CART(AtfGherkinKeyword.WHEN,
            "cart displays the configured product with the requested quantity", true),
    AUTHENTICATED_CHECKOUT(AtfGherkinKeyword.AND,
            "authenticated checkout exposes the signed-in proceed action", false),
    GUEST_CHECKOUT(AtfGherkinKeyword.AND,
            "guest checkout form contains the configured identity", true),
    BILLING(AtfGherkinKeyword.AND,
            "billing form is completed with the configured address", true),
    PAYMENT_METHOD(AtfGherkinKeyword.AND,
            "payment form displays the configured demo method", true),
    PAYMENT_CHECK(AtfGherkinKeyword.AND,
            "payment check accepts the configured demo method", true),
    ORDER_CONFIRMATION(AtfGherkinKeyword.THEN,
            "order confirmation is displayed for the completed purchase", true);

    private final AtfGherkinKeyword keyword;
    private final String title;
    private final boolean purchaseJourneyStep;

    ShopPurchaseTraceStep(AtfGherkinKeyword keyword, String title, boolean purchaseJourneyStep) {
        this.keyword = keyword;
        this.title = title;
        this.purchaseJourneyStep = purchaseJourneyStep;
    }

    public AtfGherkinKeyword keyword() {
        return keyword;
    }

    public String title() {
        return title;
    }

    public boolean purchaseJourneyStep() { return purchaseJourneyStep; }
}
