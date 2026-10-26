package atf.core.security;

import java.util.Set;

    /** Redaction policies applied at the evidence/report boundary. */
public record RedactionSettings(boolean enabled, String replacement, boolean redactLocalPaths,
                                boolean piiEnabled, Set<String> sensitiveFields) {
    public RedactionSettings {
        replacement = replacement == null || replacement.isBlank() ? "***REDACTED***" : replacement;
        sensitiveFields = sensitiveFields == null ? Set.of() : sensitiveFields.stream()
                .map(value -> value.toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static RedactionSettings defaults() {
        return new RedactionSettings(true, "***REDACTED***", true, false,
                Set.of("password", "token", "csrfmiddlewaretoken", "csrf_token", "csrf-token",
                        "access_token", "refresh_token", "client_secret", "api_key",
                        "authorization", "x-api-key", "x-auth-token", "proxy-authorization"));
    }
}
