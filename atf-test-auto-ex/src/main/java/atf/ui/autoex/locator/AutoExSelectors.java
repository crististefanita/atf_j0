package atf.ui.autoex.locator;

import atf.api.ui.dom.HtmlAttribute;
import atf.api.ui.dom.HtmlElement;

/** CSS vocabulary for the public practice site; tests and page objects do not embed selectors. */
public final class AutoExSelectors {
    public static final String STORE_BUSY_MESSAGE = "This website is under heavy load (queue full)";
    public static final String STORE_BUSY_MESSAGE_LOCATOR = "body :text-is('%s')".formatted(STORE_BUSY_MESSAGE);
    public static final String STORE_HEADING = "#slider-carousel h2";
    public static final String PRIVACY_DIALOG = ".fc-consent-root";
    public static final String PRIVACY_MANAGE_OPTIONS = ".fc-consent-root button:has-text('Manage options')";
    public static final String PRIVACY_SELECTED_OPTIONS = ".fc-consent-root input.fc-preference-slider-purpose:checked";
    public static final String PRIVACY_OPTION_LABEL = "label[for='%s']";
    public static final String PRIVACY_CONFIRM_CHOICES = ".fc-consent-root button:visible:has-text('Confirm choices')";
    public static final String VIGNETTE_CLOSE_CONTROL = "#dismiss-button";
    public static final String PRODUCTS_LINK = "#header " + linkTo(AutoExRoutes.PRODUCTS);
    public static final String CART_LINK = "#header " + linkTo(AutoExRoutes.CART);
    public static final String SEARCH_INPUT = "#search_product";
    public static final String SEARCH_BUTTON = "#submit_search";
    public static final String ACCOUNT_RESULT_HEADING = "h2.title.text-center";
    public static final String ACCOUNT_DELETED_HEADING = "h2[data-qa='account-deleted']";
    public static final String SEARCH_RESULT_PRODUCT_NAME = ".features_items .productinfo p";
    public static final String PRODUCT_DETAILS_LINK = "a[href^='%s']".formatted(AutoExRoutes.PRODUCT_DETAILS.path());
    public static final String PRODUCT_INFORMATION = ".product-information";
    public static final String QUANTITY = "#quantity";
    public static final String ADD_TO_CART = "button.cart";
    public static final String CART_ADD_CONFIRMATION = "#cartModal";
    public static final String VIEW_CART = linkTo(AutoExRoutes.CART);
    public static final String CART_PRODUCT_ROW_PREFIX = "#product-";
    public static final String CART_PRODUCT_NAME = "h4";
    public static final String CART_QUANTITY = ".cart_quantity button";
    public static final String CART_PRICE = ".cart_price";
    public static final String CART_TOTAL = ".cart_total";
    public static final String CART_REMOVE = ".cart_quantity_delete";
    public static final String EMPTY_CART = "#empty_cart:visible";
    public static final String CHECKOUT = "a.check_out";
    public static final String REGISTER_LOGIN = linkTo(AutoExRoutes.LOGIN);
    public static final String HEADER_LOGIN = "#header " + REGISTER_LOGIN;
    public static final String SIGNUP_NAME = "[data-qa='signup-name']";
    public static final String SIGNUP_EMAIL = "[data-qa='signup-email']";
    public static final String SIGNUP_BUTTON = "[data-qa='signup-button']";
    public static final String LOGIN_EMAIL = "[data-qa='login-email']";
    public static final String LOGIN_PASSWORD = "[data-qa='login-password']";
    public static final String LOGIN_BUTTON = "[data-qa='login-button']";
    public static final String LOGGED_IN_NAME = "#header a:has(i.fa-user) b";
    public static final String LOGOUT = "#header " + linkTo(AutoExRoutes.LOGOUT);
    public static final String ACCOUNT_PASSWORD = "[data-qa='password']";
    public static final String ACCOUNT_GENDER_MR = "#id_gender1";
    public static final String ACCOUNT_DAYS = "[data-qa='days']";
    public static final String ACCOUNT_MONTHS = "[data-qa='months']";
    public static final String ACCOUNT_YEARS = "[data-qa='years']";
    public static final String ACCOUNT_FIRST_NAME = "[data-qa='first_name']";
    public static final String ACCOUNT_LAST_NAME = "[data-qa='last_name']";
    public static final String ACCOUNT_COMPANY = "[data-qa='company']";
    public static final String ACCOUNT_ADDRESS_1 = "[data-qa='address']";
    public static final String ACCOUNT_ADDRESS_2 = "[data-qa='address2']";
    public static final String ACCOUNT_COUNTRY = "[data-qa='country']";
    public static final String ACCOUNT_STATE = "[data-qa='state']";
    public static final String ACCOUNT_CITY = "[data-qa='city']";
    public static final String ACCOUNT_POSTAL_CODE = "[data-qa='zipcode']";
    public static final String ACCOUNT_MOBILE = "[data-qa='mobile_number']";
    public static final String CREATE_ACCOUNT = "[data-qa='create-account']";
    public static final String CONTINUE = "[data-qa='continue-button']";
    public static final String DELETE_ACCOUNT = "#header " + linkTo(AutoExRoutes.DELETE_ACCOUNT);
    public static final String DELIVERY_ADDRESS = "#address_delivery";
    public static final String BILLING_ADDRESS = "#address_invoice";
    public static final String ORDER_REVIEW = "#cart_info";
    public static final String ORDER_TOTAL = ORDER_REVIEW + " tbody tr:last-child .cart_total_price";
    public static final String ORDER_COMMENT = "textarea[name='message']";
    public static final String PLACE_ORDER = linkTo(AutoExRoutes.PAYMENT);
    public static final String PAYMENT_HEADING = "h2.heading";
    public static final String CARD_NAME = "[data-qa='name-on-card']";
    public static final String CARD_NUMBER = "[data-qa='card-number']";
    public static final String CARD_CVC = "[data-qa='cvc']";
    public static final String CARD_EXPIRY_MONTH = "[data-qa='expiry-month']";
    public static final String CARD_EXPIRY_YEAR = "[data-qa='expiry-year']";
    public static final String PAY_AND_CONFIRM = "[data-qa='pay-button']";
    public static final String ORDER_SUCCESS = "[data-qa='order-placed']";
    public static final String ORDER_CONFIRMATION = "#form p";
    public static final String DOWNLOAD_INVOICE = "a[href^='%s']".formatted(AutoExRoutes.INVOICE_DOWNLOAD.path());
    public static final String SENSITIVE_FIELDS = HtmlElement.selectorList(
            HtmlElement.INPUT, HtmlElement.TEXTAREA, HtmlElement.SELECT)
            + ", " + HtmlAttribute.CONTENTEDITABLE.equalsSelector("true");

    private static String linkTo(AutoExRoutes route) {
        return "a[href='%s']".formatted(route.path());
    }

    private AutoExSelectors() { }
}
