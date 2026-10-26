package atf.core.security;

/** Conservative display redaction; it never changes assertion input values. */
public final class SensitiveDataRedactor implements Redactor {
    private final RedactionSettings settings;
    private final RedactionContext context;
    private final SecretPatternRedactor secretPatterns;
    private final PiiRedactor pii;
    private final LocalPathRedactor localPaths;
    private final StructuredJsonRedactor structuredJson;

    public SensitiveDataRedactor() {
        this(RedactionSettings.defaults());
    }

    public SensitiveDataRedactor(RedactionSettings settings) {
        this.settings = settings == null ? RedactionSettings.defaults() : settings;
        this.context = new RedactionContext(this.settings);
        this.secretPatterns = new SecretPatternRedactor(context);
        this.pii = new PiiRedactor(context);
        this.localPaths = new LocalPathRedactor(context);
        this.structuredJson = new StructuredJsonRedactor(context, this::redactText);
    }

    @Override
    public String redact(String value) {
        if (value == null || !context.enabled()) return value;
        String safe = secretPatterns.redact(value);
        safe = pii.redact(safe);
        return localPaths.redact(safe);
    }

    /** Explicit name for free-form diagnostics at the report boundary. */
    public String redactText(String value) {
        return redact(value);
    }

    public String redactField(String field, String value) {
        String normalizedField = field == null ? "" : field.trim().toLowerCase(java.util.Locale.ROOT);
        if (settings.sensitiveFields().contains(normalizedField)) {
            return settings.replacement();
        }
        return redact(value);
    }

    public String redactJson(String json) {
        return structuredJson.redact(json);
    }
}
