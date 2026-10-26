package atf.core.http.response;

import atf.core.diagnostics.ThrowableDiagnostics;

/** Represents an unavailable response body explicitly instead of as valid empty JSON. */
public final class ApiBodyEvidence {
    private static final String PREFIX = "<body unavailable: ";

    private ApiBodyEvidence() { }

    public static String unavailable(String source, Throwable error) {
        String owner = source == null || source.isBlank() ? "API transport" : source.trim();
        String cause = error == null
                ? "payload was not exposed"
                : ThrowableDiagnostics.unavailable(error).replaceFirst("^unavailable:\\s*", "");
        return PREFIX + owner + " — " + cause + ">";
    }

    public static String unavailable(String source, String detail) {
        String owner = source == null || source.isBlank() ? "API transport" : source.trim();
        String cause = detail == null || detail.isBlank() ? "payload was not exposed" : detail.trim();
        return PREFIX + owner + " — " + cause + ">";
    }

    public static boolean isUnavailable(String body) {
        return body != null && body.startsWith(PREFIX);
    }
}
