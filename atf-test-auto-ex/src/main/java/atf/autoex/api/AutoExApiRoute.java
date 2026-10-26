package atf.autoex.api;

/** Public API paths documented by Automation Exercise; UI routes remain in AutoExRoutes. */
public enum AutoExApiRoute {
    PRODUCTS_LIST("/api/productsList"),
    BRANDS_LIST("/api/brandsList"),
    SEARCH_PRODUCT("/api/searchProduct"),
    VERIFY_LOGIN("/api/verifyLogin"),
    CREATE_ACCOUNT("/api/createAccount"),
    DELETE_ACCOUNT("/api/deleteAccount"),
    UPDATE_ACCOUNT("/api/updateAccount"),
    USER_DETAIL_BY_EMAIL("/api/getUserDetailByEmail");

    public static final String PATH_PREFIX = "/api/";

    private final String path;

    AutoExApiRoute(String path) { this.path = path; }

    public String path() { return path; }
}
