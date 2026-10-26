package atf.reporting.configuration;

/** Immutable settings owned by the standalone reporting module. */
public record ReportingSettings(int assertionMaxDisplayCharacters, int bodyMaxDisplayCharacters) {
    public static final int DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS = 240;
    public static final int DEFAULT_BODY_MAX_DISPLAY_CHARACTERS = 100;
    public static final int DISABLE_BODY_DISPLAY_LIMIT = 0;
    public ReportingSettings(int assertionMaxDisplayCharacters) { this(assertionMaxDisplayCharacters, DEFAULT_BODY_MAX_DISPLAY_CHARACTERS); }
    public ReportingSettings {
        if (assertionMaxDisplayCharacters <= 0) {
            throw new IllegalArgumentException("assertion display limit must be positive");
        }
        if (bodyMaxDisplayCharacters < DISABLE_BODY_DISPLAY_LIMIT) {
            throw new IllegalArgumentException("body display limit must be zero or positive");
        }
    }
    public static ReportingSettings defaults() { return new ReportingSettings(DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS, DEFAULT_BODY_MAX_DISPLAY_CHARACTERS); }
}
