package atf.reporting.ui.audit.plan;

/** Stable report surfaces that can be audited independently. */
public enum ExtentSection {
    OVERVIEW("Overview"),
    TESTS("Tests"),
    CATEGORIES("Categories"),
    FAILS("Fails"),
    EVIDENCE("Evidence"),
    SCREEN_VALIDATION("UI validation");

    private final String displayName;

    ExtentSection(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

