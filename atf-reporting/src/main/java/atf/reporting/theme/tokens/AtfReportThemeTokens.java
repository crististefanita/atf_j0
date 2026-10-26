package atf.reporting.theme.tokens;

import atf.reporting.contract.AtfReportStatus;

/**
 * Immutable visual vocabulary shared by the Extent and fallback renderers.
 *
 * <p>This class owns only reusable CSS tokens.  It deliberately contains no
 * selectors, layout rules or renderer decisions; those remain in the theme
 * facade and can be split independently without changing the public report
 * contract.</p>
 */
public final class AtfReportThemeTokens {
    public static final String FONT_STACK = AtfReportTypography.SANS_FONT_FAMILY;
    /** Shared Pass/Warning/Fail geometry used by every report tab. */
    public static final String BADGE_HEIGHT = "18.2px";
    public static final String BADGE_PADDING = "0 5px";
    public static final String BADGE_RADIUS = "4px";
    public static final String BADGE_FONT_SIZE = "9.52px";
    public static final String BADGE_LINE_HEIGHT = "18.2px";
    /** Meta/duration badges remain slightly taller, as in the report model. */
    public static final String META_BADGE_HEIGHT = "22.4px";
    public static final String META_BADGE_PADDING = "5px 7px";
    public static final String META_BADGE_RADIUS = "4px";
    public static final String META_BADGE_FONT_SIZE = "9.52px";
    public static final String META_BADGE_LINE_HEIGHT = "10.6624px";
    /** Key/value evidence tables reserve one common column plus a small text delta. */
    public static final String EVIDENCE_KEY_COLUMN_WIDTH = "28%";
    /** Stable STATUS lane shared by UI and API evidence event tables. */
    public static final String EVIDENCE_STATUS_COLUMN_WIDTH = "52px";
    public static final String EVIDENCE_SUMMARY_PADDING = "9px 12px";
    public static final String EVIDENCE_SUMMARY_LINE_HEIGHT = "16px";
    public static final String EVIDENCE_SUMMARY_HEIGHT = "34px";
    /** Two-line technical aggregate summaries get exactly twice the compact baseline. */
    public static final String EVIDENCE_AGGREGATE_SUMMARY_HEIGHT = "68px";
    /** One bounded viewport for every scrollable report table, matching System/Environment. */
    public static final String TABLE_SCROLL_HEIGHT = "500px";
    /** Source-compatible semantic aliases; the value remains owned by TABLE_SCROLL_HEIGHT. */
    public static final String EVIDENCE_TABLE_SCROLL_HEIGHT = TABLE_SCROLL_HEIGHT;
    public static final String EVIDENCE_RADIUS = "10px";
    /** Scenario proposition geometry is shared by Extent and fallback contracts. */
    public static final String BASE_LINE_HEIGHT = "1.55";
    public static final String SCENARIO_LINE_HEIGHT = BASE_LINE_HEIGHT;
    public static final String SCENARIO_NODE_MIN_HEIGHT = "18.76px";
    public static final String SCENARIO_HEADER_MIN_HEIGHT = "38.5px";
    public static final String SCENARIO_GLYPH_OFFSET = "1px";
    /** Fixed Extent navigation rail that overlays narrow static reports. */
    public static final String NARROW_VIEWPORT = "992px";
    public static final String NARROW_NAV_INSET = "70px";
    public static final String AUDIT_SCROLL_HEIGHT = TABLE_SCROLL_HEIGHT;

    private AtfReportThemeTokens() {
    }

    public static String cssVariables() {
        return String.join("", ":root{",
                variable("dark", AtfReportColors.DARK),
                variable("bg", AtfReportColors.DARK),
                variable("surface", AtfReportColors.DARK),
                variable("panel", AtfReportColors.DARK),
                variable("panel-2", AtfReportColors.DARK),
                variable("code", AtfReportColors.JSON_SURFACE),
                variable("json-text", AtfReportColors.JSON_TEXT),
                variable("code-text", AtfReportColors.CODE_TEXT),
                variable("table-text", AtfReportColors.TABLE_TEXT),
                variable("table-label", AtfReportColors.TABLE_LABEL),
                variable("border", AtfReportColors.BORDER),
                variable("card-border", AtfReportColors.REFERENCE_BORDER),
                variable("text", AtfReportColors.TEXT),
                variable("card-text", AtfReportColors.CARD_TEXT),
                variable("muted", AtfReportColors.MUTED),
                variable("overview-muted", AtfReportColors.OVERVIEW_MUTED),
                variable("annotation-muted", AtfReportColors.ANNOTATION_MUTED),
                variable("accent", AtfReportColors.ACCENT),
                variable("accent-contrast", AtfReportColors.ACCENT_CONTRAST),
                variable(AtfReportStatus.PASS.displayValue(), AtfReportColors.EXTENT_PASS),
                variable("pass-text", AtfReportColors.PASS_TEXT),
                variable("good", AtfReportColors.GOOD),
                variable(AtfReportStatus.FAIL.displayValue(), AtfReportColors.EXTENT_FAIL),
                variable("fail-text", AtfReportColors.FAIL_TEXT),
                variable("bad", AtfReportColors.DANGER),
                variable(AtfReportStatus.WARNING.displayValue(), AtfReportColors.WARNING),
                variable("warn", AtfReportColors.WARNING),
                variable("info", AtfReportColors.INFO),
                variable("info-text", AtfReportColors.INFO_TEXT),
                variable("warning-text", AtfReportColors.WARNING_TEXT),
                variable(AtfReportStatus.SKIP.displayValue(), AtfReportColors.EXTENT_SKIP),
                variable("skip-text", AtfReportColors.SKIP_TEXT),
                variable("accent08", AtfReportColors.ACCENT_08),
                variable("accent12", AtfReportColors.ACCENT_12),
                variable("accent14", AtfReportColors.ACCENT_14),
                variable("secondary-heading-bg", AtfReportColors.ACCENT_07),
                variable("secondary-heading-bg-hover", AtfReportColors.ACCENT_12),
                variable("secondary-heading-border", AtfReportColors.ACCENT_42),
                variable("font-sans", AtfReportTypography.SANS_FONT_FAMILY),
                variable("font-base-size", AtfReportTypography.BASE_FONT_SIZE),
                variable("font-overview-size", AtfReportTypography.OVERVIEW_FONT_SIZE),
                variable("font-json-size", AtfReportTypography.JSON_FONT_SIZE),
                variable("font-table-header-size", AtfReportTypography.TABLE_HEADER_FONT_SIZE),
                variable("font-table-cell-size", AtfReportTypography.TABLE_CELL_FONT_SIZE),
                variable("font-badge-size", AtfReportTypography.BADGE_FONT_SIZE),
                variable("badge-height", BADGE_HEIGHT),
                variable("badge-padding", BADGE_PADDING),
                variable("badge-radius", BADGE_RADIUS),
                variable("badge-font-size", BADGE_FONT_SIZE),
                variable("badge-line-height", BADGE_LINE_HEIGHT),
                variable("meta-badge-height", META_BADGE_HEIGHT),
                variable("meta-badge-padding", META_BADGE_PADDING),
                variable("meta-badge-radius", META_BADGE_RADIUS),
                variable("meta-badge-font-size", META_BADGE_FONT_SIZE),
                variable("meta-badge-line-height", META_BADGE_LINE_HEIGHT),
                variable("evidence-key-column-width", EVIDENCE_KEY_COLUMN_WIDTH),
                variable("evidence-status-column-width", EVIDENCE_STATUS_COLUMN_WIDTH),
                variable("evidence-summary-padding", EVIDENCE_SUMMARY_PADDING),
                variable("evidence-summary-line-height", EVIDENCE_SUMMARY_LINE_HEIGHT),
                variable("evidence-summary-height", EVIDENCE_SUMMARY_HEIGHT),
                variable("evidence-aggregate-summary-height", EVIDENCE_AGGREGATE_SUMMARY_HEIGHT),
                variable("table-scroll-height", TABLE_SCROLL_HEIGHT),
                variable("evidence-table-scroll-height", TABLE_SCROLL_HEIGHT),
                variable("evidence-radius", EVIDENCE_RADIUS),
                variable("base-line-height", BASE_LINE_HEIGHT),
                variable("scenario-line-height", SCENARIO_LINE_HEIGHT),
                variable("scenario-node-min-height", SCENARIO_NODE_MIN_HEIGHT),
                variable("scenario-header-min-height", SCENARIO_HEADER_MIN_HEIGHT),
                variable("scenario-glyph-offset", SCENARIO_GLYPH_OFFSET),
                variable("narrow-nav-inset", NARROW_NAV_INSET),
                variable("overview-font-family", FONT_STACK),
                variable("overview-font-size", AtfReportTypography.OVERVIEW_FONT_SIZE),
                variable("audit-scroll-height", TABLE_SCROLL_HEIGHT),
                "}");
    }

    private static String variable(String name, String value) {
        return "--atf-" + name + ":" + value + ";";
    }
}
