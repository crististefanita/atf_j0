package atf.reporting.markup;

/** Canonical metadata keys and values shared by report evidence publishers. */
public final class AtfReportEvidenceKeys {
    /** Internal checkpoint identity; excluded from public request/response tables. */
    public static final String REQUEST_ACTION = "request.action";
    /** Internal checkpoint target; selected UI actions retain their own public targets. */
    public static final String REQUEST_TARGET = "request.target";
    public static final String OUTCOME_TYPE = "outcome.type";
    public static final String ARTIFACT_PUBLICATION = "artifact-publication";
    public static final String EVIDENCE_TYPE = "evidence.type";
    public static final String EVIDENCE_POLICY = "evidence.policy";
    public static final String REPORT_SETUP = "report-setup";
    public static final String UI_VALIDATION = "ui-validation";
    public static final String API_TRACE = "api-trace";
    public static final String UI_FAILURE = "ui-failure";
    public static final String UNIFIED_UI_FLOW = "unified-ui-flow";
    public static final String FLOW_MODE = "flow.mode";
    public static final String FLOW_SCENARIO = "flow.scenario";
    public static final String FLOW_PHASE = "flow.phase";
    /** Selects the UI-only cross-phase projection without changing the evidence model. */
    public static final String FLOW_PRESENTATION = "flow.presentation";
    public static final String COMPACT_UI_ONLY = "compact-ui-only";
    public static final String FLOW_CHECKPOINT_COUNT = "flow.checkpoint.count";
    public static final String FLOW_CHECKPOINT_PREFIX = "flow.checkpoint.";
    public static final String SETUP_RESULT = "setup.result";
    public static final String SETUP_CHECK_PREFIX = "setup.check.";

    private AtfReportEvidenceKeys() { }

    public static String setupCheck(int index) {
        if (index < 1) throw new IllegalArgumentException("index must be >= 1");
        return SETUP_CHECK_PREFIX + index;
    }

    public static String flowCheckpoint(int index) {
        if (index < 1) throw new IllegalArgumentException("index must be >= 1");
        return FLOW_CHECKPOINT_PREFIX + index;
    }
}
