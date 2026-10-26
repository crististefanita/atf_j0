package atf.reporting.theme.tokens;

/** Immutable typography vocabulary shared by all report renderers. */
public final class AtfReportTypography {
    public static final String SANS_FONT_FAMILY = "'Segoe UI','Inter','Helvetica Neue',Arial,sans-serif";
    public static final String CODE_FONT_FAMILY = "ui-monospace,Consolas,monospace";
    public static final String BASE_FONT_SIZE = "14px";
    public static final String OVERVIEW_FONT_SIZE = "12.6px";
    public static final String JSON_FONT_SIZE = "12.5px";
    // The reference report sets the document root to 14px.  Keep the
    // resolved values explicit in the self-contained Java artifact so rem
    // defaults from the embedded Bootstrap bundle cannot make Tests larger.
    public static final String TABLE_HEADER_FONT_SIZE = "11.2px";
    public static final String TABLE_CELL_FONT_SIZE = "12.6px";
    public static final String BADGE_FONT_SIZE = "9.52px";

    private AtfReportTypography() {
    }
}
