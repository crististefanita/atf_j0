package atf.reporting.api;

/** Where the credential-free auth observation was obtained. */
public enum ReportAuthSource {
    NONE("none"),
    PROFILE("profile"),
    HEADER("header"),
    QUERY("query");

    private final String wireValue;

    ReportAuthSource(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
