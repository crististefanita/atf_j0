package atf.reporting.api;

/**
 * Diagnostic failure raised at the legacy reflection boundary.
 *
 * <p>The reporting model must never silently replace a missing or malformed
 * legacy value with a plausible default. Keeping the source type and field in
 * the exception makes a broken adapter actionable without coupling reporting
 * production code to the legacy ATF module.</p>
 */
public final class ReportModelAdapterException extends IllegalArgumentException {
    private final String sourceType;
    private final String field;

    public ReportModelAdapterException(String message, String sourceType, String field,
                                       Throwable cause) {
        super(message, cause);
        this.sourceType = sourceType;
        this.field = field;
    }

    public String sourceType() {
        return sourceType;
    }

    public String field() {
        return field;
    }
}
