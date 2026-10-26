package atf.core.diagnostics;

/** Produces bounded, single-line failure diagnostics safe for reports and logs. */
public final class ThrowableDiagnostics {
    public static final int DEFAULT_MAX_LENGTH = 210;

    private ThrowableDiagnostics() { }

    public static String unavailable(Throwable error) {
        return unavailable(error, DEFAULT_MAX_LENGTH);
    }

    public static String unavailable(Throwable error, int maxLength) {
        if (maxLength < 2) throw new IllegalArgumentException("maxLength must be at least 2");
        if (error == null) return "unavailable";
        String type = error.getClass().getSimpleName();
        String message = error.getMessage();
        if (message == null || message.isBlank()) return bounded("unavailable: " + type, maxLength);
        String compact = message.replaceAll("\\s+", " ").trim();
        return bounded("unavailable: " + type + " — " + compact, maxLength);
    }

    private static String bounded(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }
}
