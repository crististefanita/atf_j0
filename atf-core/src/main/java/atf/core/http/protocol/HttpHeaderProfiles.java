package atf.core.http.protocol;

import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;

import java.util.Map;

/** Immutable, reusable header profiles for common HTTP payload contracts. */
public final class HttpHeaderProfiles {
    public static final Map<String, String> JSON_CONTENT =
            Map.of(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON);

    public static final Map<String, String> JSON_REQUEST = Map.of(
            HttpHeader.ACCEPT.value(), MediaTypes.APPLICATION_JSON,
            HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON);

    private HttpHeaderProfiles() { }
}
