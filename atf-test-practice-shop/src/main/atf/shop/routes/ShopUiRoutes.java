package atf.shop.routes;

/** Stable relative route vocabulary for the Practice Shop UI. */
public final class ShopUiRoutes {
    public static final String HOME = "/";
    public static final String CHECKOUT = "/checkout";
    public static final String ACCOUNT = "/account";
    public static final String AUTH_LOGIN = "/auth/login";
    public static final String AUTH_REGISTER = "/auth/register";
    public static final String PRODUCT_PREFIX = "/product/";
    public static final String PRODUCT_PATTERN = PRODUCT_PREFIX + "*";
    public static final String CATEGORY_PREFIX = "/categories/";

    private ShopUiRoutes() { }

    public static String product(String productId) {
        return PRODUCT_PREFIX + required(productId, "product id");
    }

    public static String category(String categorySlug) {
        return CATEGORY_PREFIX + required(categorySlug, "category slug");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
