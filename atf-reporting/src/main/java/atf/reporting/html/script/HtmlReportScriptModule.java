package atf.reporting.html.script;

/** Stable owners for the fallback JavaScript composition order. */
public enum HtmlReportScriptModule {
    TAB_QUERY("tab-query"),
    PANEL_QUERY("panel-query"),
    ACTIVATION("activation"),
    TAB_CLICK("tab-click"),
    DEFAULT_TAB("default-tab"),
    FAILS_DEEP_LINK("fails-deep-link"),
    RUNTIME_BOOT("runtime-boot"),
    RUNTIME_CORE("runtime-core"),
    RUNTIME_TESTS("runtime-tests"),
    RUNTIME_OVERVIEW("runtime-overview"),
    RUNTIME_EVIDENCE("runtime-evidence"),
    RUNTIME_NAVIGATION("runtime-navigation"),
    RUNTIME_DURATION("runtime-duration"),
    RUNTIME_FAILURES("runtime-failures"),
    RUNTIME_FAILURE_CAUSE("runtime-failure-cause"),
    RUNTIME_READY("runtime-ready");

    private final String owner;

    HtmlReportScriptModule(String owner) {
        this.owner = owner;
    }

    public String owner() {
        return owner;
    }
}
