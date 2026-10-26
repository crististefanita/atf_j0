package atf.shop.api;

import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.response.ApiOperationLabel;

/**
 * Stable Practice Shop operation vocabulary used by request creation,
 * network selection and report titles. Dynamic IDs and query values remain in
 * the request/path, never in this enum.
 */
public enum ShopApiOperation {
    LOGIN(HttpMethod.POST, ShopApiRoutes.LOGIN, "establish API session", ShopApiRoutes.LOGIN),
    USER_REGISTER(HttpMethod.POST, ShopApiRoutes.USER_REGISTER,
            "register checkout account", ShopApiRoutes.USER_REGISTER),
    SEARCH(HttpMethod.GET, ShopApiRoutes.PRODUCT_SEARCH,
            "find configured product", ShopApiRoutes.PRODUCT_SEARCH),
    PRODUCT_DETAIL(HttpMethod.GET, ShopApiRoutes.PRODUCTS,
            "read selected product", ShopApiRoutes.PRODUCT_DETAIL_TEMPLATE),
    CART_CREATE(HttpMethod.POST, ShopApiRoutes.CARTS,
            "create cart", ShopApiRoutes.CARTS),
    CART_ADD(HttpMethod.POST, ShopApiRoutes.CART_RESOURCE_PREFIX,
            "add product to cart", ShopApiRoutes.CART_RESOURCE_TEMPLATE),
    CART_READ(HttpMethod.GET, ShopApiRoutes.CART_RESOURCE_PREFIX,
            "verify cart contents", ShopApiRoutes.CART_RESOURCE_TEMPLATE),
    CART_UPDATE(HttpMethod.PUT, ShopApiRoutes.CART_RESOURCE_PREFIX,
            "update cart quantity", ShopApiRoutes.CART_QUANTITY_TEMPLATE),
    CART_REMOVE(HttpMethod.DELETE, ShopApiRoutes.CART_RESOURCE_PREFIX,
            "remove cart product", ShopApiRoutes.CART_PRODUCT_TEMPLATE),
    POSTCODE_LOOKUP(HttpMethod.GET, ShopApiRoutes.POSTCODE_LOOKUP,
            "resolve checkout address", ShopApiRoutes.POSTCODE_LOOKUP),
    PAYMENT_CHECK(HttpMethod.POST, ShopApiRoutes.PAYMENT_CHECK,
            "validate payment selection", ShopApiRoutes.PAYMENT_CHECK),
    ORDER_CREATE(HttpMethod.POST, ShopApiRoutes.INVOICE_CREATE,
            "create order", ShopApiRoutes.INVOICE_CREATE),
    UNKNOWN(null, "", "API request", "");

    public static final String OBSERVED_QUERY_METHOD = "QUERY";

    private final HttpMethod method;
    private final String routePrefix;
    private final String description;
    private final String routeTemplate;

    ShopApiOperation(HttpMethod method, String routePrefix,
                             String description, String routeTemplate) {
        this.method = method;
        this.routePrefix = routePrefix;
        this.description = description;
        this.routeTemplate = routeTemplate;
    }

    /** The actual HTTP verb sent over the wire. */
    public String wireValue() {
        return method == null ? "" : method.value();
    }

    /** Configured request verb; captured evidence must retain its own method instead. */
    public String reportMethod() {
        return wireValue();
    }

    public String description() {
        return description;
    }

    public String routeTemplate() {
        return routeTemplate;
    }

    public boolean matches(String actualMethod, String actualPath) {
        if (method == null || actualMethod == null || actualPath == null) return false;
        if (!method.value().equalsIgnoreCase(actualMethod)
                && !(this == SEARCH && OBSERVED_QUERY_METHOD.equalsIgnoreCase(actualMethod))) {
            return false;
        }
        return switch (this) {
            case LOGIN, USER_REGISTER, SEARCH, CART_CREATE, POSTCODE_LOOKUP, PAYMENT_CHECK ->
                    actualPath.equals(routePrefix) || actualPath.startsWith(routePrefix + "?");
            case ORDER_CREATE -> actualPath.equals(ShopApiRoutes.INVOICE_CREATE)
                    || actualPath.equals(ShopApiRoutes.GUEST_INVOICE_CREATE)
                    || actualPath.startsWith(ShopApiRoutes.INVOICE_CREATE + "?");
            case PRODUCT_DETAIL -> actualPath.startsWith(routePrefix)
                    && !actualPath.equals(ShopApiRoutes.PRODUCT_COLLECTION)
                    && !actualPath.startsWith(ShopApiRoutes.PRODUCT_SEARCH);
            case CART_ADD, CART_READ -> actualPath.startsWith(routePrefix)
                    && !actualPath.equals(ShopApiRoutes.CARTS);
            case CART_UPDATE -> actualPath.startsWith(routePrefix)
                    && actualPath.endsWith(ShopApiRoutes.CART_PRODUCT_QUANTITY);
            case CART_REMOVE -> actualPath.startsWith(routePrefix)
                    && actualPath.contains(ShopApiRoutes.CART_PRODUCT_PREFIX)
                    && !actualPath.endsWith(ShopApiRoutes.CART_PRODUCT_QUANTITY);
            case UNKNOWN -> false;
        };
    }

    public static ShopApiOperation resolve(String method, String path) {
        for (ShopApiOperation operation : values()) {
            if (operation.matches(method, path)) return operation;
        }
        return UNKNOWN;
    }

    public static String reportTitle(String method, String path) {
        ShopApiOperation operation = resolve(method, path);
        return ApiOperationLabel.format(method, path, operation.description());
    }

    /** Uses an explicit invocation intent when a caller has causal context. */
    public static String reportTitle(String method, String path, String intent) {
        return intent == null || intent.isBlank()
                ? reportTitle(method, path)
                : ApiOperationLabel.format(method, path, intent);
    }
}
