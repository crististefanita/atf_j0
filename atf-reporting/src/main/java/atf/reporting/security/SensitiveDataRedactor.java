package atf.reporting.security;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative renderer-boundary redactor; deliberately independent of ATF core. */
public final class SensitiveDataRedactor {
    private static final String SECRET_NAME =
            "password|token|access[_-]?token|refresh[_-]?token|client[_-]?secret|"
                    + "api[_-]?key|x[_-]?api[_-]?key|x[_-]?auth[_-]?token|secret";
    private static final Pattern AUTH = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)(['\"]?)(bearer\\s+|basic\\s+)?([^,;\\s'\"\\]}]+)");
    private static final Pattern BEARER_CREDENTIAL = Pattern.compile(
            "(?i)(\\b(?:bearer|basic)\\s+)[^\\s,;&'\"\\]}]+");
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\\"(?:authorization|cookie|set-cookie|" + SECRET_NAME
                    + ")\\\"\\s*:\\s*\\\")((?:\\\\.|[^\"\\\\])*)(\\\")");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(\\b(?:" + SECRET_NAME + ")\\s*[:=]\\s*)[^,;\\s&#'\"\\]}]+");
    private static final Pattern QUERY_SECRET = Pattern.compile(
            "(?i)([?&](?:" + SECRET_NAME + ")=)[^&#\\s'\"\\]}]+");
    private static final Pattern PATH_SECRET = Pattern.compile(
            "(?i)(/+(?:token|api[-_]?key|secret|password)/)[^/?#\\s'\"\\]}]+" );
    private static final Pattern SECRET_FIELD_NAME = Pattern.compile("(?i)(?:" + SECRET_NAME + ")");
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "authorization", "cookie", "set-cookie", "token", "access_token", "accesstoken",
            "refresh_token", "refreshtoken", "password", "secret", "client_secret",
            "clientsecret", "api-key", "api_key", "apikey", "x-api-key", "x_api_key",
            "x-auth-token", "x_auth_token", "proxy-authorization", "proxy_authorization");
    private final String replacement;

    public SensitiveDataRedactor() { this("***REDACTED***"); }

    public SensitiveDataRedactor(String replacement) {
        this.replacement = replacement == null || replacement.isBlank()
                ? "***REDACTED***" : replacement;
    }

    public String redact(String value) { return redactText(value); }

    public String redactText(String value) {
        if (value == null) return null;
        if (looksLikeJson(value)) {
            return JsonEvidenceRedactor.redact(value, this::sensitiveField, this::redactPlain, replacement)
                    .orElseGet(() -> redactPlain(value));
        }
        return redactPlain(value);
    }

    private String redactPlain(String value) {
        String mask = Matcher.quoteReplacement(replacement);
        String multipartSafe = MultipartEvidenceRedactor.redact(value, replacement,
                this::sensitiveField);
        String safe = JSON_SECRET.matcher(multipartSafe).replaceAll("$1" + mask + "$3");
        safe = AUTH.matcher(safe).replaceAll("$1$2$3" + mask);
        safe = BEARER_CREDENTIAL.matcher(safe).replaceAll("$1" + mask);
        safe = KEY_VALUE_SECRET.matcher(safe).replaceAll("$1" + mask);
        safe = QUERY_SECRET.matcher(safe).replaceAll("$1" + mask);
        return PATH_SECRET.matcher(safe).replaceAll("$1" + mask);
    }

    public String redactField(String field, String value) {
        if (sensitiveField(field)) return replacement;
        return redactText(value);
    }

    public String redactJson(String json) {
        if (json == null) return null;
        return JsonEvidenceRedactor.redact(json, this::sensitiveField, this::redactPlain, replacement)
                .orElseGet(() -> looksLikeJson(json) ? "[invalid JSON omitted]" : redactPlain(json));
    }

    private boolean sensitiveField(String field) {
        return field != null && (SENSITIVE_FIELDS.contains(field.trim().toLowerCase(Locale.ROOT))
                || SECRET_FIELD_NAME.matcher(field.trim()).matches());
    }

    private static boolean looksLikeJson(String value) {
        String trimmed = value.stripLeading();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
