package atf.shop.api;

/** API route vocabulary observed from the Practice Shop application. */
public final class ShopApiRoutes {
    public static final String LOGIN = "/users/login";
    public static final String USER_REGISTER = "/users/register";
    public static final String PRODUCT_SEARCH = "/products/search";
    public static final String PRODUCTS = "/products/";
    public static final String PRODUCT_COLLECTION = "/products";
    public static final String CARTS = "/carts";
    public static final String CART_RESOURCE_PREFIX = CARTS + "/";
    public static final String CART_PRODUCT_PREFIX = "/product/";
    public static final String CART_PRODUCT_QUANTITY = "/product/quantity";
    public static final String CATEGORIES = "/categories";
    public static final String POSTCODE_LOOKUP = "/postcode-lookup";
    public static final String PAYMENT_CHECK = "/payment/check";
    public static final String INVOICE_CREATE = "/invoices";
    public static final String GUEST_INVOICE_CREATE = INVOICE_CREATE + "/guest";

    public static final String PRODUCT_DETAIL_TEMPLATE = "/products/{productId}";
    public static final String CART_RESOURCE_TEMPLATE = "/carts/{cartId}";
    public static final String CART_QUANTITY_TEMPLATE = CART_RESOURCE_TEMPLATE + CART_PRODUCT_QUANTITY;
    public static final String CART_PRODUCT_TEMPLATE = CART_RESOURCE_TEMPLATE + "/product/{productId}";

    private ShopApiRoutes() { }

    public static String product(String id) {
        return PRODUCTS + required(id, "product id");
    }

    public static String cart(String id) {
        return CARTS + "/" + required(id, "cart id");
    }

    public static String updateCartQuantity(String cartId) {
        return cart(cartId) + CART_PRODUCT_QUANTITY;
    }

    public static String removeCartProduct(String cartId, String productId) {
        return cart(cartId) + CART_PRODUCT_PREFIX + required(productId, "product id");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
