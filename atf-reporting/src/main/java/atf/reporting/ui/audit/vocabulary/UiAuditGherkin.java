package atf.reporting.ui.audit.vocabulary;

import atf.reporting.ui.audit.plan.UiExtentReportSource;

import java.util.Objects;

/**
 * Single semantic owner for setup propositions published by UI report audits.
 *
 * <p>The constants deliberately contain the human-readable proposition body.
 * The lifecycle owns the typed {@code GIVEN} keyword and adds it exactly once.
 * Setup evidence below the proposition carries the artifact identity; it must
 * not replace the concise text with an enum or an internal source label.</p>
 */
public final class UiAuditGherkin {
    public static final String GENERATED_API_REPORT_READY =
            "the generated API Extent report is available for read-only inspection";
    public static final String STATIC_UI_REPORT_READY =
            "the previous UI Extent report is available for read-only inspection";
    public static final String REPORT_INPUT_READY =
            "the selected Extent report is available for read-only inspection";

    private UiAuditGherkin() {
    }

    public static String reportReady(UiExtentReportSource source) {
        return switch (Objects.requireNonNull(source, "source")) {
            case GENERATED_API -> GENERATED_API_REPORT_READY;
            case STATIC_PATH, STATIC_RESOURCE -> STATIC_UI_REPORT_READY;
        };
    }

    public static String categoryFixtureReady(String fixtureName) {
        String name = Objects.requireNonNull(fixtureName, "fixtureName").trim();
        if (name.isEmpty()) throw new IllegalArgumentException("fixtureName must not be blank");
        return "the category cardinality fixture " + name
                + " is available for read-only inspection";
    }
}
