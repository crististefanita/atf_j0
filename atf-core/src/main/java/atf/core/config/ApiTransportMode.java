package atf.core.config;

import java.util.Locale;

/** Typed transport profile; acceptance is always the safe default. */
public enum ApiTransportMode {
    ACCEPTANCE,
    LIVE;

    public static ApiTransportMode parse(String value) {
        if (value == null || value.isBlank()) return ACCEPTANCE;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("MOCK")) return ACCEPTANCE;
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unsupported API transport mode: " + value
                    + ". Expected acceptance or live", error);
        }
    }
}
