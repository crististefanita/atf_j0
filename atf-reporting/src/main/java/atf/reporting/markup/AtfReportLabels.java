package atf.reporting.markup;

/**
 * Canonical labels shared by every report surface.
 *
 * <p>The Tests tab is the visual source of truth. Fails, Category, Overview
 * projections and the fallback renderer must reuse these labels instead of
 * inventing surface-specific variants.</p>
 */
public final class AtfReportLabels {
    public static final String REPORT_TITLE = "ATF Report";
    public static final String CATEGORY = "Category";
    public static final String FAILS = "Fails";
    public static final String NO_CATEGORIES_AVAILABLE = "No categories available";
    public static final String NO_CATEGORY_ANNOTATIONS =
            "No category annotations were published for this report.";
    public static final String OVERVIEW_DEFAULT_COPY =
            "Overview is the default view; its tab remains last in the navigation contract.";
    public static final String NO_FAILED_ASSERTIONS = "No failed assertions.";
    public static final String OPEN_DUPLICATE_SCREENSHOT = "Open duplicate screenshot";
    public static final String BASE64_IMAGE_MARKER = "📷 base64 img";
    public static final String DUPLICATE_SCREENSHOT_NOTE =
            "same image — identical to an earlier captured state in this run;"
                    + " not re-embedded to avoid duplicating the same bytes.";
    public static final String REQUEST_DETAILS = "Request details";
    public static final String RESPONSE_DETAILS = "Response details";
    public static final String AUDIT_DETAILS = "Audit details";
    public static final String EXECUTION_DIAGNOSTICS = "Execution diagnostics";
    public static final String TECHNICAL_TRACE = "TECHNICAL TRACE";
    public static final String REQUEST_JSON = "REQUEST JSON";
    public static final String RESPONSE_JSON = "RESPONSE JSON";
    public static final String ACTION = "ACTION";
    public static final String TARGET = "TARGET";
    public static final String OBSERVED = "OBSERVED";
    public static final String TIME = "TIME";
    public static final String ASSERTION = "ASSERTION";
    public static final String SETUP_EVIDENCE = "Setup evidence";
    public static final String FIELD = "FIELD";
    public static final String VALUE = "VALUE";
    public static final String EXPECTED = "EXPECTED";
    public static final String ACTUAL = "ACTUAL";
    public static final String RESULT = "RESULT";
    public static final String ATTEMPT = "ATTEMPT";
    public static final String STATUS = "STATUS";
    public static final String DURATION = "DURATION";
    public static final String OUTCOME = "OUTCOME";
    public static final String REASON = "REASON";
    public static final String JSON_BODY = "JSON BODY";
    public static final String CHECK = "CHECK";
    public static final String UI_SCREENSHOT = "UI screenshot";
    public static final String AUDIT_OBSERVATIONS = "Audit observations";
    public static final String PLAYWRIGHT_ACTION_SEQUENCE = "Playwright action sequence";
    public static final String CHECKPOINT = "checkpoint";
    public static final String CHECKPOINTS = "checkpoints";
    public static final String FAILED_OF = "failed of";
    public static final String ENTRIES = "entries";
    public static final String EVIDENCE_TYPE = "EVIDENCE TYPE";
    public static final String RESPONSE_STATUS = "response.status";
    public static final String RESPONSE_URL = "response.url";
    public static final String RESPONSE_DURATION_MS = "response.duration.ms";
    public static final String TOTAL_DURATION_MS = "total.duration.ms";
    public static final String ATTEMPTS = "attempts";
    public static final String RETRY_EVENTS = "retry.events";
    public static final String REQUEST_PREFIX = "REQUEST";

    private AtfReportLabels() {
    }

    public static String responseDetails(int fields) {
        return counted(RESPONSE_DETAILS, fields, "field", "fields");
    }

    public static String auditDetails(int fields) {
        return counted(AUDIT_DETAILS, fields, "field", "fields");
    }

    public static String requestDetails(int fields) {
        return counted(REQUEST_DETAILS, fields, "field", "fields");
    }

    public static String uiFlowContext(int fields) {
        return counted("UI flow context", fields, "field", "fields");
    }

    public static String uiFlowProvenance(int checkpoints) {
        return counted("UI flow provenance", checkpoints, CHECKPOINT, CHECKPOINTS);
    }

    public static String assertions(int rows) {
        return String.format("Assertions · %d", rows);
    }

    public static String setupEvidence(int checks) {
        return counted(SETUP_EVIDENCE, checks, "check", "checks");
    }

    public static String executionEvidence(int attempts) {
        return counted("Execution evidence", attempts, "attempt", "attempts");
    }

    public static String actionSequence(int entries) {
        return String.format("%s (%d %s)", PLAYWRIGHT_ACTION_SEQUENCE, entries,
                entries == 1 ? "entry" : ENTRIES);
    }

    public static String failedActions(long failures, int total) {
        return String.format("%s · %d %s %d %s", PLAYWRIGHT_ACTION_SEQUENCE,
                failures, FAILED_OF, total, total == 1 ? "entry" : ENTRIES);
    }

    public static String failedObservations(long failures, int total) {
        return String.format("%s · %d %s %d", AUDIT_OBSERVATIONS,
                failures, FAILED_OF, total);
    }

    private static String counted(String label, int count, String singular, String plural) {
        return String.format("%s · %d %s", label, count, count == 1 ? singular : plural);
    }
}
