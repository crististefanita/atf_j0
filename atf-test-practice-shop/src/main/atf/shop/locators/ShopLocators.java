package atf.shop.locators;

import atf.ui.dom.CssPseudoClass;
import atf.ui.dom.HtmlElement;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;
import java.util.Objects;

import static atf.ui.dom.HtmlAttribute.ALT;
import static atf.ui.dom.HtmlAttribute.CONTENTEDITABLE;
import static atf.ui.dom.HtmlAttribute.DATA_TEST;
import static atf.ui.dom.HtmlAttribute.HREF;
import static atf.ui.dom.HtmlAttribute.ID;
import static atf.ui.dom.HtmlAttribute.NAME;
import static atf.ui.dom.HtmlAttribute.ROLE;
import static atf.ui.dom.HtmlAttribute.TYPE;

/** The only selector owner for the Practice Software Testing application. */
public final class ShopLocators {
    private static final String SENSITIVE_FIELDS = HtmlElement.selectorList(
            HtmlElement.INPUT, HtmlElement.TEXTAREA, HtmlElement.SELECT)
            + ", " + CONTENTEDITABLE.equalsSelector("true");
    private static final String LOGIN_EMAIL = HtmlElement.INPUT.attributeEquals(ID, "email");
    private static final String LOGIN_PASSWORD = HtmlElement.INPUT.attributeEquals(ID, "password");
    // The live contract guarantees the data-test hook; the rendered tag may be input or button.
    private static final String LOGIN_SUBMIT = DATA_TEST.equalsSelector("login-submit");
    private static final String REGISTER_FIRST_NAME =
            HtmlElement.INPUT.attributeEquals(ID, "first_name");
    private static final String REGISTER_LAST_NAME =
            HtmlElement.INPUT.attributeEquals(ID, "last_name");
    private static final String REGISTER_DOB = HtmlElement.INPUT.attributeEquals(ID, "dob");
    private static final String REGISTER_COUNTRY = HtmlElement.SELECT.attributeEquals(ID, "country");
    private static final String REGISTER_POSTAL_CODE =
            HtmlElement.INPUT.attributeEquals(ID, "postal_code");
    private static final String REGISTER_HOUSE_NUMBER =
            HtmlElement.INPUT.attributeEquals(ID, "house_number");
    private static final String REGISTER_STREET = HtmlElement.INPUT.attributeEquals(ID, "street");
    private static final String REGISTER_CITY = HtmlElement.INPUT.attributeEquals(ID, "city");
    private static final String REGISTER_STATE = HtmlElement.INPUT.attributeEquals(ID, "state");
    private static final String REGISTER_PHONE = HtmlElement.INPUT.attributeEquals(ID, "phone");
    private static final String REGISTER_EMAIL = HtmlElement.INPUT.attributeEquals(ID, "email");
    private static final String REGISTER_PASSWORD = HtmlElement.INPUT.attributeEquals(ID, "password");
    private static final String REGISTER_SUBMIT = DATA_TEST.equalsSelector("register-submit");
    private static final String SEARCH_QUERY = HtmlElement.INPUT.attributeEquals(ID, "search-query");
    private static final String HOME_NAVIGATION = HtmlElement.NAVIGATION.selector() + ".navbar";
    private static final String HOME_BRAND = HtmlElement.ANCHOR.selector() + ".navbar-brand";
    private static final String HOME_BANNER = HtmlElement.IMAGE.attributeEquals(ALT, "Banner");
    private static final String HOME_SORT = HtmlElement.SELECT.attributeEquals(DATA_TEST, "sort");
    private static final String HOME_PRICE_RANGE = HtmlElement.SPAN.attributeEquals(ROLE, "slider");
    private static final String HOME_SEARCH_RESET =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "search-reset");
    private static final String HOME_SEARCH_SUBMIT =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "search-submit");
    private static final String HOME_CATEGORY_FILTERS =
            HtmlElement.INPUT.attributeEquals(NAME, "category_id");
    private static final String HOME_BRAND_FILTERS =
            HtmlElement.INPUT.attributeEquals(NAME, "brand_id");
    private static final String HOME_ECO_FILTER =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "eco-friendly-filter");
    private static final String HOME_PRODUCT_GRID =
            HtmlElement.ANCHOR.attributeStartsWith(DATA_TEST, "product-");
    private static final String HOME_PRODUCT_NAMES =
            HOME_PRODUCT_GRID + " " + DATA_TEST.equalsSelector("product-name");
    private static final String HOME_PRODUCT_IMAGES = HOME_PRODUCT_GRID + " "
            + HtmlElement.IMAGE.selector() + ".card-img-top";
    private static final String HOME_PAGINATION = HtmlElement.NAVIGATION.selector()
            + ":has(" + DATA_TEST.equalsSelector("pagination-next") + ")";
    private static final String HOME_FOOTER = HtmlElement.FOOTER.selector();
    private static final String PRODUCT_LINK = ShopProductSelector.PRODUCT_CARD;
    // The external Practice Shop contract guarantees data-test, not a stable tag.
    private static final String PRODUCT_NAME = DATA_TEST.equalsSelector("product-name");
    private static final String PRODUCT_PRICE = DATA_TEST.equalsSelector("unit-price");
    private static final String PRODUCT_DESCRIPTION = DATA_TEST.equalsSelector("product-description");
    private static final String PRODUCT_QUANTITY =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "quantity");
    private static final String PRODUCT_OUT_OF_STOCK = DATA_TEST.equalsSelector("out-of-stock");
    private static final String PRODUCT_AVAILABILITY = PRODUCT_QUANTITY + ":enabled:visible, "
            + PRODUCT_OUT_OF_STOCK + ":visible";
    private static final String ADD_TO_CART =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "add-to-cart");
    private static final String CART = HtmlElement.ANCHOR.attributeEquals(DATA_TEST, "nav-cart");
    private static final String CART_PRODUCT_TITLE = DATA_TEST.equalsSelector("product-title");
    private static final String CART_PRODUCT_PRICE = DATA_TEST.equalsSelector("product-price");
    private static final String CART_LINE_TOTAL = DATA_TEST.equalsSelector("line-price");
    private static final String CART_TOTAL = DATA_TEST.equalsSelector("cart-total");
    private static final String CART_PRODUCT_QUANTITY =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "product-quantity");
    private static final String CART_PROCEED =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "proceed-1");
    private static final String GUEST_TAB = HtmlElement.ANCHOR.attributeEquals(HREF, "#guest-tab");
    private static final String GUEST_EMAIL =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "guest-email");
    private static final String GUEST_FIRST_NAME =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "guest-first-name");
    private static final String GUEST_LAST_NAME =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "guest-last-name");
    private static final String GUEST_SUBMIT =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "guest-submit");
    private static final String GUEST_PROCEED =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "proceed-2-guest");
    private static final String AUTHENTICATED_PROCEED =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "proceed-2");
    private static final String COUNTRY = HtmlElement.SELECT.attributeEquals(DATA_TEST, "country");
    private static final String COUNTRY_SELECTED_OPTION = CssPseudoClass.CHECKED.applyTo(
            COUNTRY + " " + HtmlElement.OPTION.selector());
    private static final String POSTAL_CODE =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "postal_code");
    private static final String HOUSE_NUMBER =
            HtmlElement.INPUT.attributeEquals(DATA_TEST, "house_number");
    private static final String STREET = HtmlElement.INPUT.attributeEquals(DATA_TEST, "street");
    private static final String CITY = HtmlElement.INPUT.attributeEquals(DATA_TEST, "city");
    private static final String STATE = HtmlElement.INPUT.attributeEquals(DATA_TEST, "state");
    private static final String BILLING_PROCEED =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "proceed-3");
    private static final String PAYMENT_METHOD =
            HtmlElement.SELECT.attributeEquals(DATA_TEST, "payment-method");
    private static final String PAYMENT_METHOD_SELECTED_OPTION = CssPseudoClass.CHECKED.applyTo(
            PAYMENT_METHOD + " " + HtmlElement.OPTION.selector());
    private static final String PAYMENT_CHECK =
            HtmlElement.BUTTON.attributeEquals(DATA_TEST, "finish");
    private static final String ACCOUNT_HEADING = HtmlElement.HEADING_1.selector();
    private static final String PAYMENT_SUCCESS =
            "xpath=//*[contains(normalize-space(text()), 'Payment was successful')]";

    public AtfLocator loginEmail(Page page) { return css(page, LOGIN_EMAIL); }
    public AtfLocator loginPassword(Page page) { return css(page, LOGIN_PASSWORD); }
    public AtfLocator loginSubmit(Page page) { return css(page, LOGIN_SUBMIT); }
    public AtfLocator registerFirstName(Page page) { return css(page, REGISTER_FIRST_NAME); }
    public AtfLocator registerLastName(Page page) { return css(page, REGISTER_LAST_NAME); }
    public AtfLocator registerDob(Page page) { return css(page, REGISTER_DOB); }
    public AtfLocator registerCountry(Page page) { return css(page, REGISTER_COUNTRY); }
    public AtfLocator registerPostalCode(Page page) { return css(page, REGISTER_POSTAL_CODE); }
    public AtfLocator registerHouseNumber(Page page) { return css(page, REGISTER_HOUSE_NUMBER); }
    public AtfLocator registerStreet(Page page) { return css(page, REGISTER_STREET); }
    public AtfLocator registerCity(Page page) { return css(page, REGISTER_CITY); }
    public AtfLocator registerState(Page page) { return css(page, REGISTER_STATE); }
    public AtfLocator registerPhone(Page page) { return css(page, REGISTER_PHONE); }
    public AtfLocator registerEmail(Page page) { return css(page, REGISTER_EMAIL); }
    public AtfLocator registerPassword(Page page) { return css(page, REGISTER_PASSWORD); }
    public AtfLocator registerSubmit(Page page) { return css(page, REGISTER_SUBMIT); }
    public AtfLocator searchQuery(Page page) { return css(page, SEARCH_QUERY); }
    public AtfLocator homeNavigation(Page page) { return css(page, HOME_NAVIGATION); }
    public AtfLocator homeBrand(Page page) { return css(page, HOME_BRAND); }
    public AtfLocator homeBanner(Page page) { return css(page, HOME_BANNER); }
    public AtfLocator homeSort(Page page) { return css(page, HOME_SORT); }
    public AtfLocator homePriceRange(Page page) { return css(page, HOME_PRICE_RANGE); }
    public AtfLocator homeSearchReset(Page page) { return css(page, HOME_SEARCH_RESET); }
    public AtfLocator homeSearchSubmit(Page page) { return css(page, HOME_SEARCH_SUBMIT); }
    public AtfLocator homeCategoryFilters(Page page) { return css(page, HOME_CATEGORY_FILTERS); }
    public AtfLocator homeBrandFilters(Page page) { return css(page, HOME_BRAND_FILTERS); }
    public AtfLocator homeEcoFilter(Page page) { return css(page, HOME_ECO_FILTER); }
    public AtfLocator homeProductGrid(Page page) { return css(page, HOME_PRODUCT_GRID); }
    /** First card is used only for strict synchronization; collection stays available for counts. */
    public AtfLocator homeProductGridFirst(Page page) {
        return new AtfLocator(page.locator(HOME_PRODUCT_GRID).first(), HOME_PRODUCT_GRID);
    }
    public AtfLocator homeProductNames(Page page) { return css(page, HOME_PRODUCT_NAMES); }
    public AtfLocator homeProductImages(Page page) { return css(page, HOME_PRODUCT_IMAGES); }
    public AtfLocator homePagination(Page page) { return css(page, HOME_PAGINATION); }
    public AtfLocator homeFooter(Page page) { return css(page, HOME_FOOTER); }
    public AtfLocator productLink(Page page, String name) {
        return css(page, ShopProductSelector.exactProductCard(name));
    }
    /** First exact-name card is the deterministic interaction target; the collection remains available for diagnostics. */
    public AtfLocator productLinkFirst(Page page, String name) {
        AtfLocator matchingProducts = productLink(page, name);
        return new AtfLocator(matchingProducts.locator().first(), matchingProducts.selector());
    }
    public AtfLocator productLinkByPath(Page page, String productPath) {
        return css(page, HtmlElement.ANCHOR.attributeEquals(HREF, productPath));
    }
    public AtfLocator accountBody(Page page) { return pageBody(page); }
    public AtfLocator accountHeading(Page page) { return css(page, ACCOUNT_HEADING); }
    public AtfLocator pageBody(Page page) { return css(page, HtmlElement.BODY.selector()); }
    public AtfLocator productName(Page page) { return css(page, PRODUCT_NAME); }
    public AtfLocator productPrice(Page page) { return css(page, PRODUCT_PRICE); }
    public AtfLocator productDescription(Page page) { return css(page, PRODUCT_DESCRIPTION); }
    public AtfLocator productQuantity(Page page) { return css(page, PRODUCT_QUANTITY); }
    /** Either purchasable controls or an explicit stock verdict; a disabled input alone is unresolved. */
    public AtfLocator productAvailability(Page page) { return css(page, PRODUCT_AVAILABILITY); }
    public AtfLocator productOutOfStock(Page page) { return css(page, PRODUCT_OUT_OF_STOCK); }
    public AtfLocator addToCart(Page page) { return css(page, ADD_TO_CART); }
    public AtfLocator cart(Page page) { return css(page, CART); }
    public AtfLocator cartProductTitle(Page page) { return css(page, CART_PRODUCT_TITLE); }
    public AtfLocator cartProductPrice(Page page) { return css(page, CART_PRODUCT_PRICE); }
    public AtfLocator cartLineTotal(Page page) { return css(page, CART_LINE_TOTAL); }
    public AtfLocator cartTotal(Page page) { return css(page, CART_TOTAL); }
    public AtfLocator cartProductQuantity(Page page) { return css(page, CART_PRODUCT_QUANTITY); }
    public AtfLocator cartProceed(Page page) { return css(page, CART_PROCEED); }
    public AtfLocator guestTab(Page page) { return css(page, GUEST_TAB); }
    public AtfLocator guestEmail(Page page) { return css(page, GUEST_EMAIL); }
    public AtfLocator guestFirstName(Page page) { return css(page, GUEST_FIRST_NAME); }
    public AtfLocator guestLastName(Page page) { return css(page, GUEST_LAST_NAME); }
    public AtfLocator guestSubmit(Page page) { return css(page, GUEST_SUBMIT); }
    public AtfLocator guestProceed(Page page) { return css(page, GUEST_PROCEED); }
    public AtfLocator authenticatedProceed(Page page) { return css(page, AUTHENTICATED_PROCEED); }
    public AtfLocator country(Page page) { return css(page, COUNTRY); }
    public AtfLocator selectedCountry(Page page) { return css(page, COUNTRY_SELECTED_OPTION); }
    public AtfLocator postalCode(Page page) { return css(page, POSTAL_CODE); }
    public AtfLocator houseNumber(Page page) { return css(page, HOUSE_NUMBER); }
    public AtfLocator street(Page page) { return css(page, STREET); }
    public AtfLocator city(Page page) { return css(page, CITY); }
    public AtfLocator state(Page page) { return css(page, STATE); }
    public AtfLocator billingProceed(Page page) { return css(page, BILLING_PROCEED); }
    public AtfLocator paymentMethod(Page page) { return css(page, PAYMENT_METHOD); }
    public AtfLocator selectedPaymentMethod(Page page) {
        return css(page, PAYMENT_METHOD_SELECTED_OPTION);
    }
    public AtfLocator paymentCheck(Page page) { return css(page, PAYMENT_CHECK); }
    public AtfLocator paymentSuccess(Page page) { return css(page, PAYMENT_SUCCESS); }
    public AtfLocator confirmOrder(Page page) {
        return new AtfLocator(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Confirm").setExact(true)),
                "role=button[name='Confirm']");
    }
    public AtfLocator orderConfirmation(Page page) {
        String selector = "xpath=//*[contains(normalize-space(text()), 'Thanks for your order!')]";
        return new AtfLocator(page.locator(selector), selector);
    }
    /** Resolves a semantic control for annotation-driven page validation. */
    public AtfLocator control(Page page, ShopControl control) {
        Objects.requireNonNull(control, "control");
        return switch (control) {
            case PAGE_BODY -> pageBody(page);
            case HOME_NAVIGATION -> homeNavigation(page);
            case HOME_BRAND -> homeBrand(page);
            case HOME_BANNER -> homeBanner(page);
            case HOME_SORT -> homeSort(page);
            case HOME_PRICE_RANGE -> homePriceRange(page);
            case SEARCH_QUERY -> searchQuery(page);
            case HOME_SEARCH_RESET -> homeSearchReset(page);
            case HOME_SEARCH_SUBMIT -> homeSearchSubmit(page);
            case HOME_CATEGORY_FILTERS -> homeCategoryFilters(page);
            case HOME_BRAND_FILTERS -> homeBrandFilters(page);
            case HOME_ECO_FILTER -> homeEcoFilter(page);
            case HOME_PRODUCT_GRID -> homeProductGrid(page);
            case HOME_PRODUCT_NAMES -> homeProductNames(page);
            case HOME_PRODUCT_IMAGES -> homeProductImages(page);
            case HOME_PAGINATION -> homePagination(page);
            case HOME_FOOTER -> homeFooter(page);
            case LOGIN_EMAIL -> loginEmail(page);
            case LOGIN_PASSWORD -> loginPassword(page);
            case LOGIN_SUBMIT -> loginSubmit(page);
            case REGISTER_FIRST_NAME -> registerFirstName(page);
            case REGISTER_LAST_NAME -> registerLastName(page);
            case REGISTER_DOB -> registerDob(page);
            case REGISTER_COUNTRY -> registerCountry(page);
            case REGISTER_POSTAL_CODE -> registerPostalCode(page);
            case REGISTER_HOUSE_NUMBER -> registerHouseNumber(page);
            case REGISTER_STREET -> registerStreet(page);
            case REGISTER_CITY -> registerCity(page);
            case REGISTER_STATE -> registerState(page);
            case REGISTER_PHONE -> registerPhone(page);
            case REGISTER_EMAIL -> registerEmail(page);
            case REGISTER_PASSWORD -> registerPassword(page);
            case REGISTER_SUBMIT -> registerSubmit(page);
            case PRODUCT_NAME -> productName(page);
            case PRODUCT_PRICE -> productPrice(page);
            case PRODUCT_DESCRIPTION -> productDescription(page);
            case PRODUCT_QUANTITY -> productQuantity(page);
            case ADD_TO_CART -> addToCart(page);
            case CART_PRODUCT_TITLE -> cartProductTitle(page);
            case CART_PRODUCT_PRICE -> cartProductPrice(page);
            case CART_LINE_TOTAL -> cartLineTotal(page);
            case CART_TOTAL -> cartTotal(page);
            case CART_PRODUCT_QUANTITY -> cartProductQuantity(page);
            case CART_PROCEED -> cartProceed(page);
            case GUEST_TAB -> guestTab(page);
            case GUEST_EMAIL -> guestEmail(page);
            case GUEST_FIRST_NAME -> guestFirstName(page);
            case GUEST_LAST_NAME -> guestLastName(page);
            case GUEST_SUBMIT -> guestSubmit(page);
            case GUEST_PROCEED -> guestProceed(page);
            case AUTHENTICATED_PROCEED -> authenticatedProceed(page);
            case COUNTRY -> country(page);
            case POSTAL_CODE -> postalCode(page);
            case HOUSE_NUMBER -> houseNumber(page);
            case STREET -> street(page);
            case CITY -> city(page);
            case STATE -> state(page);
            case BILLING_PROCEED -> billingProceed(page);
            case PAYMENT_METHOD -> paymentMethod(page);
            case PAYMENT_CHECK -> paymentCheck(page);
            case PAYMENT_SUCCESS -> paymentSuccess(page);
            case CONFIRM_ORDER -> confirmOrder(page);
            case ORDER_CONFIRMATION -> orderConfirmation(page);
            case ACCOUNT_MARKER -> accountHeading(page);
        };
    }
    public AtfLocator searchSubmit(Page page) {
        String selector = HtmlElement.FORM.selector() + ":has(" + SEARCH_QUERY + ") "
                + HtmlElement.BUTTON.attributeEquals(TYPE, "submit");
        return css(page, selector);
    }

    private static AtfLocator css(Page page, String selector) {
        return new AtfLocator(page.locator(selector), selector);
    }

    /** Form controls are masked in screenshots so entered account data cannot leak into evidence. */
    public List<Locator> sensitiveFields(Page page) {
        return List.of(Objects.requireNonNull(page, "page").locator(SENSITIVE_FIELDS));
    }

}


