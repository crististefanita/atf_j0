package atf.core.security;

import java.util.regex.Matcher;

/** Immutable policy passed to every redaction strategy. */
record RedactionContext(RedactionSettings settings) {
    RedactionContext {
        settings = settings == null ? RedactionSettings.defaults() : settings;
    }

    boolean enabled() {
        return settings.enabled();
    }

    String replacement() {
        return settings.replacement();
    }

    /** Encodes only the configured mask; structural group references remain owned by each strategy. */
    String quotedReplacement() {
        return Matcher.quoteReplacement(replacement());
    }
}
