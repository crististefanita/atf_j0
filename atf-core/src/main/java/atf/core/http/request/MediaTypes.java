package atf.core.http.request;

import java.util.Locale;

/** Canonical media types used by API fixtures and contracts. */
public final class MediaTypes {
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_CSV = "text/csv";

    private MediaTypes() { }

    /** Accepts vendor JSON variants such as {@code application/problem+json}. */
    public static boolean isJson(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("json");
    }
}
