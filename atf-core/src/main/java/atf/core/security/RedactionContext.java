package atf.core.security;

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
}
