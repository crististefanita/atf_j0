package atf.core.http.request;

/** Canonical HTTP verbs used by the API execution and test layers. */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE;

    public String value() {
        return name();
    }
}
