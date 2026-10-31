package atf.core.security;

import java.util.regex.Pattern;

/** Redacts user profile paths before local diagnostics cross the report boundary. */
final class LocalPathRedactor implements Redactor {
    private static final Pattern USER_PATH = Pattern.compile(
            "(?i)([A-Z]:\\\\Users\\\\)[^\\s\\\"']+");

    private final RedactionContext context;

    LocalPathRedactor(RedactionContext context) {
        this.context = context;
    }

    @Override
    public String redact(String value) {
        if (value == null || !context.enabled() || !context.settings().redactLocalPaths()) {
            return value;
        }
        return USER_PATH.matcher(value).replaceAll("$1" + context.quotedReplacement());
    }
}
