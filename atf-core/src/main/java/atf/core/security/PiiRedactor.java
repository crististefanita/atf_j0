package atf.core.security;

import java.util.regex.Pattern;

/** Optional redaction strategy for email addresses and phone-like values. */
final class PiiRedactor implements Redactor {
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?\\d[\\d .()/-]{7,}\\d)(?!\\d)");

    private final RedactionContext context;

    PiiRedactor(RedactionContext context) {
        this.context = context;
    }

    @Override
    public String redact(String value) {
        if (value == null || !context.enabled() || !context.settings().piiEnabled()) {
            return value;
        }
        String mask = context.quotedReplacement();
        return PHONE.matcher(EMAIL.matcher(value).replaceAll(mask)).replaceAll(mask);
    }
}
