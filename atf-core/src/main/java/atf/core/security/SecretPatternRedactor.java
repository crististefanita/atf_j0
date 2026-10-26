package atf.core.security;

import java.util.regex.Pattern;

/** Redacts credentials in authorization headers, query strings and path segments. */
final class SecretPatternRedactor implements Redactor {
    private static final String SENSITIVE_NAME =
            "password|token|access[_-]?token|refresh[_-]?token|client[_-]?secret|api[_-]?key";
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\\"(?:" + SENSITIVE_NAME + ")\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final Pattern AUTH_HEADER = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*(?:bearer|basic)\\s+)[^,;\\s]+");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(\\b(?:" + SENSITIVE_NAME + ")\\s*[:=]\\s*)[^,;\\s&#]+" );
    private static final Pattern QUERY_SECRET = Pattern.compile(
            "(?i)([?&](?:" + SENSITIVE_NAME + ")=)[^&#\\s]+");
    private static final Pattern PATH_SECRET = Pattern.compile(
            "(?i)(/+(?:token|api[-_]?key|secret|password)/)[^/?#\\s]+");

    private final RedactionContext context;

    SecretPatternRedactor(RedactionContext context) {
        this.context = context;
    }

    @Override
    public String redact(String value) {
        if (value == null || !context.enabled()) {
            return value;
        }
        String mask = context.replacement();
        String safe = AUTH_HEADER.matcher(JSON_SECRET.matcher(value).replaceAll("$1" + mask + "$2"))
                .replaceAll("$1" + mask);
        safe = KEY_VALUE_SECRET.matcher(safe).replaceAll("$1" + mask);
        safe = QUERY_SECRET.matcher(safe).replaceAll("$1" + mask);
        return PATH_SECRET.matcher(safe).replaceAll("$1" + mask);
    }
}
