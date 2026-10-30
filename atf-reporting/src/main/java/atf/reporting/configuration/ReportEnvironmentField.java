package atf.reporting.configuration;

/** Allowlisted semantic fields that may appear in Overview/System metadata. */
public enum ReportEnvironmentField {
    ENVIRONMENT("Environment", "Run"),
    TEST_PACKAGE("Test package", "Run"),
    SUITE("Suite", "Run"),
    PROFILE("Profile", "Run"),
    SOURCE_BUILD("Source/build", "Run"),
    UI_HOST("UI host", "Targets"),
    API_ENDPOINT("API endpoint", "Targets"),
    TRANSPORT_MODE("Transport mode", "Targets"),
    PRECONDITION("Precondition", "Targets"),
    UI_JOURNEY_PRECONDITION("UI journey precondition", "Targets"),
    CLIENT("Client", "Browser"),
    BROWSER("Browser", "Browser"),
    HEADLESS("Headless", "Browser"),
    VIEWPORT("Viewport", "Browser"),
    UI_TIMEOUT("UI action timeout", "Time"),
    NAVIGATION_TIMEOUT("Page navigation timeout", "Time"),
    API_TIMEOUT("API request timeout", "Time"),
    FLOW("Flow", "Flow"),
    FIXTURE("Fixture", "Flow"),
    AUTHENTICATION("Authentication", "Auth"),
    NETWORK_INSPECTOR("Network inspector", "Network"),
    RETRY_POLICY("Retry policy", "Network"),
    BODY_PREVIEW("Body preview", "Evidence"),
    ASSERTION_PREVIEW("Assertion preview", "Evidence"),
    API_EVIDENCE("API evidence", "Evidence"),
    UI_EVIDENCE_LAYOUT("UI evidence layout", "Evidence"),
    UI_ACTION_DETAILS("UI action details", "Evidence"),
    SCREENSHOT_POLICY("Screenshot policy", "Evidence"),
    REPORTING("Reporting", "Reporting"),
    REPORT_ARTIFACT("Report artifact", "Reporting");

    private final String label;
    private final String group;

    ReportEnvironmentField(String label, String group) {
        this.label = label;
        this.group = group;
    }

    public String label() {
        return label;
    }

    public String group() {
        return group;
    }

    /**
     * Whether this field belongs in the compact Overview/System surface.
     *
     * <p>Run identity and business-flow labels remain useful in the typed
     * composition snapshot, but they belong to Tests/trace context rather
     * than to operational System metadata.</p>
     */
    public boolean isSystemVisible() {
        return switch (this) {
            case TEST_PACKAGE, SUITE, FLOW, FIXTURE, UI_JOURNEY_PRECONDITION -> false;
            default -> true;
        };
    }
}
