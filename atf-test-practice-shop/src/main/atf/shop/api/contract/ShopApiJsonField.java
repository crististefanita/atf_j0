package atf.shop.api.contract;

/** JSON field vocabulary shared by request construction and contract assertions. */
public enum ShopApiJsonField {
    EMAIL("email"),
    GUEST_EMAIL("guest_email"),
    GUEST_FIRST_NAME("guest_first_name"),
    GUEST_LAST_NAME("guest_last_name"),
    PASSWORD("password"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    PRODUCT_ID("product_id"),
    QUANTITY("quantity"),
    PAYMENT_METHOD("payment_method"),
    PAYMENT_DETAILS("payment_details"),
    CART_ID("cart_id"),
    BILLING_STREET("billing_street"),
    BILLING_CITY("billing_city"),
    BILLING_STATE("billing_state"),
    BILLING_COUNTRY("billing_country"),
    BILLING_POSTAL_CODE("billing_postal_code"),
    COUNTRY("country"),
    DATA("data"),
    ID("id"),
    NAME("name"),
    CATEGORY("category"),
    IN_STOCK("in_stock"),
    PRICE("price"),
    MESSAGE("message"),
    INVOICE_NUMBER("invoice_number"),
    TOTAL("total"),
    RESULT("result"),
    POSTCODE("postcode"),
    CART_ITEMS("cart_items"),
    ACCESS_TOKEN("access_token"),
    ACCESS_TOKEN_CAMEL("accessToken"),
    TOKEN("token");

    private final String wireValue;

    ShopApiJsonField(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
