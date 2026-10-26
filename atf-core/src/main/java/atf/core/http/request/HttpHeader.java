package atf.core.http.request;

/** Canonical HTTP header names used in requests, responses and evidence. */
public enum HttpHeader {
    ACCEPT("Accept"),
    AUTHORIZATION("Authorization"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_TYPE("Content-Type"),
    COOKIE("Cookie"),
    CACHE_CONTROL("Cache-Control"),
    ETAG("ETag"),
    IF_NONE_MATCH("If-None-Match"),
    RETRY_AFTER("Retry-After"),
    X_ATF_CLIENT("X-Atf-Client"),
    X_API_KEY("X-Api-Key"),
    X_CLIENT("X-Client"),
    X_CLIENT_VERSION("X-Client-Version"),
    X_CORRELATION_ID("X-Correlation-Id"),
    X_MOCK_SCENARIO("X-Mock-Scenario"),
    X_RATE_LIMIT_REMAINING("X-RateLimit-Remaining"),
    X_REQUEST_ID("X-Request-Id");

    private final String value;

    HttpHeader(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
