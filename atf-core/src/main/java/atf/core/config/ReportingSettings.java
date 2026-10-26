package atf.core.config;

/** Immutable settings consumed by an artifact renderer. Console settings live in atf-console. */
public record ReportingSettings(int assertionMaxDisplayCharacters, int bodyMaxDisplayCharacters) {
    public static final int DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS = 240;
    public static final int DEFAULT_BODY_MAX_DISPLAY_CHARACTERS = 12_000;

    /** Backward-compatible constructor for callers that only configure assertions. */
    public ReportingSettings(int assertionMaxDisplayCharacters) {
        this(assertionMaxDisplayCharacters, DEFAULT_BODY_MAX_DISPLAY_CHARACTERS);
    }

    public ReportingSettings {
        if (assertionMaxDisplayCharacters <= 0) {
            throw new IllegalArgumentException("assertionMaxDisplayCharacters must be positive");
        }
        if (bodyMaxDisplayCharacters <= 0) {
            throw new IllegalArgumentException("bodyMaxDisplayCharacters must be positive");
        }
    }

    public static ReportingSettings defaults() {
        return new ReportingSettings(DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS,
                DEFAULT_BODY_MAX_DISPLAY_CHARACTERS);
    }
}
