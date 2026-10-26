package atf.autoex.api;

/** Stable JSON field paths verified across Automation Exercise API scenarios. */
public enum AutoExApiJsonField {
    RESPONSE_CODE("responseCode"),
    MESSAGE("message"),
    PRODUCTS("products"),
    NAME("name"),
    PRICE("price"),
    BRANDS("brands"),
    USER("user"),
    USER_EMAIL("user.email");

    private final String path;

    AutoExApiJsonField(String path) { this.path = path; }

    public String path() { return path; }
}
