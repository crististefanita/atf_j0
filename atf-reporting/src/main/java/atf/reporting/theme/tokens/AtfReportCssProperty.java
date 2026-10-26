package atf.reporting.theme.tokens;

/** Finite CSS property vocabulary used by the typed rule builder. */
public enum AtfReportCssProperty {
    DISPLAY("display"),
    ALIGN_ITEMS("align-items"),
    JUSTIFY_CONTENT("justify-content"),
    HEIGHT("height"),
    WIDTH("width"),
    MIN_HEIGHT("min-height"),
    MARGIN("margin"),
    PADDING("padding"),
    LINE_HEIGHT("line-height"),
    WHITE_SPACE("white-space"),
    OVERFLOW("overflow"),
    TEXT_OVERFLOW("text-overflow"),
    OVERFLOW_WRAP("overflow-wrap"),
    WORD_BREAK("word-break"),
    MAX_WIDTH("max-width"),
    MIN_WIDTH("min-width"),
    TEXT_ALIGN("text-align"),
    FLEX_WRAP("flex-wrap"),
    MARGIN_TOP("margin-top"),
    ALIGN_SELF("align-self"),
    GAP("gap");

    private final String value;

    AtfReportCssProperty(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
