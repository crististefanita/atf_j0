package atf.console;

/** Immutable policy for the diagnostic console projection. */
public record ConsoleSettings(boolean enabled, boolean detailsEnabled) {
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_DETAILS_ENABLED = true;

    public static ConsoleSettings defaults() {
        return new ConsoleSettings(DEFAULT_ENABLED, DEFAULT_DETAILS_ENABLED);
    }

    public static ConsoleSettings enabledWithDetails() {
        return new ConsoleSettings(true, true);
    }

    public static ConsoleSettings disabled() {
        return new ConsoleSettings(false, false);
    }
}
