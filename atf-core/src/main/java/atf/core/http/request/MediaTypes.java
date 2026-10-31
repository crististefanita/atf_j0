package atf.core.http.request;

import java.util.Locale;
import java.util.regex.Pattern;

/** Canonical media types used by API fixtures and contracts. */
public final class MediaTypes {
    private static final Pattern JSON_MEDIA_TYPE = Pattern.compile(
            "[!#$%&'+.^_`|~0-9a-z-]+/(?:json|[!#$%&'+.^_`|~0-9a-z-]+\\+json)");
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_CSV = "text/csv";

    private MediaTypes() { }

    /** Classifies the concrete type/subtype, independently of parameters or body validity. */
    public static boolean isJson(String value) {
        if (value == null) return false;
        int parameters = value.indexOf(';');
        String mediaType = (parameters < 0 ? value : value.substring(0, parameters)).strip();
        return JSON_MEDIA_TYPE.matcher(mediaType.toLowerCase(Locale.ROOT)).matches();
    }
}
