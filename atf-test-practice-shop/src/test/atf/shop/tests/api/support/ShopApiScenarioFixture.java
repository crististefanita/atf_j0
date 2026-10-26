package atf.shop.tests.api.support;

import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.model.ShopCredentials;
import atf.shop.api.ShopApiRequestBodies;
import atf.shop.api.contract.ShopPaymentMethod;

import java.net.URI;

/** Named scenario vocabulary shared by Practice Shop API component tests. */
public final class ShopApiScenarioFixture {
    public static final URI API_ENDPOINT = ShopApiConfiguration.defaults().endpoint();
    public static final String VALID_EMAIL = "atf@example.test";
    public static final String VALID_PASSWORD = "secret";
    public static final String VALID_FIRST_NAME = "ATF";
    public static final String VALID_LAST_NAME = "Practice";
    public static final String INVALID_EMAIL = "invalid@example.test";
    public static final String INVALID_PASSWORD = "invalid";
    public static final String INVALID_LAST_NAME = "Invalid";
    public static final String BEARER_TOKEN = "token-1";
    public static final String PRODUCT_NAME = "Combination Pliers";
    public static final String PRODUCT_ID = "01product";
    public static final String CART_ID = "01cart";
    public static final String CATEGORY_ID = "category-1";
    public static final String OUT_OF_STOCK_PRODUCT_ID = "out-of-stock";
    public static final String FILTERED_PRODUCT_ID = "oos";
    public static final String MUTATION_CART_ID = "cart-1";
    public static final String MUTATION_PRODUCT_ID = "product-1";
    public static final String INVALID_PRODUCT_ID = "missing-product";
    public static final String FILTER_BRAND_ID = "brand-1";
    public static final String FILTER_PRICE_RANGE = "price,10,30";
    public static final int FIRST_PAGE = 1;
    public static final int FILTER_PAGE = 2;
    public static final boolean FILTER_RENTAL = false;
    public static final String MISSING_PRODUCT_ID = "missing-product";
    public static final String COUNTRY = "RO";
    public static final String POSTCODE = "010101";
    public static final String HOUSE_NUMBER = "10";
    public static final String BILLING_STREET = "1 Test Street";
    public static final String BILLING_CITY = "Test City";
    public static final String BILLING_STATE = "Test State";
    public static final String BILLING_COUNTRY = "Test Country";
    public static final String BILLING_POSTAL_CODE = "TST 001";
    public static final String UNAUTHORIZED_BODY = "{\"message\":\"Unauthorized\"}";
    public static final String NOT_FOUND_BODY = "{\"message\":\"Not found\"}";
    public static final String UNPROCESSABLE_BODY = "{\"message\":\"The given data was invalid.\"}";
    public static final String PAYMENT_SUCCESS_BODY = "{\"message\":\"Payment was successful\"}";
    public static final String PAYMENT_SUCCESS_MESSAGE = "Payment was successful";
    public static final String REGISTERED_USER_ID = "user-1";
    public static final String CREATED_ORDER_ID = "order-1";
    public static final String CREATED_INVOICE_NUMBER = "INV-TEST-1";
    public static final double CREATED_ORDER_TOTAL = 24.02;
    public static final int QUANTITY = 2;
    public static final int UPDATED_QUANTITY = 3;
    public static final String LOGIN_TOKEN_BODY =
            "{\"access_token\":\"" + BEARER_TOKEN + "\"}";
    public static final String REGISTERED_USER_BODY =
            "{\"id\":\"" + REGISTERED_USER_ID + "\",\"email\":\"" + VALID_EMAIL + "\"}";
    public static final String CREATED_INVOICE_BODY =
            "{\"id\":\"" + CREATED_ORDER_ID + "\",\"invoice_number\":\""
                    + CREATED_INVOICE_NUMBER + "\",\"total\":" + CREATED_ORDER_TOTAL + "}";
    public static final String PRODUCT_SEARCH_BODY =
            "{\"data\":[{\"id\":\"" + PRODUCT_ID + "\",\"name\":\""
                    + PRODUCT_NAME + "\"}]}";
    public static final String PRODUCT_COLLECTION_BODY =
            "{\"data\":[{\"id\":\"" + FILTERED_PRODUCT_ID
                    + "\",\"name\":\"Out of stock\",\"in_stock\":false}],"
                    + "\"category\":\"" + CATEGORY_ID + "\"}";
    public static final String CATEGORIES_BODY = "[{\"id\":\"category-1\",\"name\":\"Hand Tools\"}]";
    public static final String OUT_OF_STOCK_BODY =
            "{\"id\":\"" + OUT_OF_STOCK_PRODUCT_ID
                    + "\",\"name\":\"Out of stock\",\"in_stock\":false}";
    public static final String PRODUCT_BODY = "{\"id\":\"" + PRODUCT_ID + "\",\"name\":\""
            + PRODUCT_NAME + "\"}";
    public static final String CART_BODY = "{\"id\":\"" + CART_ID + "\"}";
    public static final String CART_ITEM_BODY =
            "{\"cart_items\":[{\"product_id\":\"" + PRODUCT_ID + "\",\"quantity\":"
                    + QUANTITY + "}]}";
    public static final String CART_MUTATION_BODY = "{\"result\":\"item added or updated\"}";
    public static final String CART_UPDATE_BODY = "{\"result\":\"item updated\"}";
    public static final String CART_UPDATE_RESULT = "item updated";
    public static final String ADDRESS_BODY = "{\"country\":\"" + COUNTRY
            + "\",\"postcode\":\"" + POSTCODE + "\"}";
    public static final String UNEXPECTED_ROUTE_BODY = "{\"message\":\"unexpected route\"}";

    private ShopApiScenarioFixture() { }

    public static ShopCredentials validCredentials() {
        return new ShopCredentials(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
    }

    public static ShopCredentials invalidCredentials() {
        return new ShopCredentials(INVALID_EMAIL, INVALID_PASSWORD, VALID_FIRST_NAME, INVALID_LAST_NAME);
    }

    public static ShopApiRequestBodies.ShopInvoiceRequest validInvoice() {
        return new ShopApiRequestBodies.ShopInvoiceRequest(
                BILLING_STREET, BILLING_CITY, BILLING_STATE, BILLING_COUNTRY,
                BILLING_POSTAL_CODE, ShopPaymentMethod.CASH_ON_DELIVERY.wireValue(),
                java.util.Map.of(), CART_ID, null, null, null);
    }

    public static ShopApiRequestBodies.ShopInvoiceRequest validGuestInvoice() {
        return new ShopApiRequestBodies.ShopInvoiceRequest(
                BILLING_STREET, BILLING_CITY, BILLING_STATE,
                BILLING_COUNTRY, BILLING_POSTAL_CODE, ShopPaymentMethod.CASH_ON_DELIVERY.wireValue(),
                java.util.Map.of(), CART_ID, VALID_EMAIL, VALID_FIRST_NAME, VALID_LAST_NAME);
    }

    public static ShopApiRequestBodies.ShopInvoiceRequest invalidGuestInvoice() {
        return new ShopApiRequestBodies.ShopInvoiceRequest(
                "", "", "", "", "", "", java.util.Map.of(), "", "", "", "");
    }
}
