package atf.core.reporting.contract;

/** Stable vocabulary for executable Given/When/Then report steps. */
public enum AtfGherkinKeyword {
    GIVEN,
    WHEN,
    THEN,
    AND,
    BUT;

    public String text() {
        return name();
    }
}
