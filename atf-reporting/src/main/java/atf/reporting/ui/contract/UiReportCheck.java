package atf.reporting.ui.contract;

/** Pure renderer-contract checks that can run without a browser. */
public enum UiReportCheck {
    STRUCTURE,
    STYLE,
    COLLAPSED_BY_DEFAULT,
    SETUP_EVIDENCE,
    CROSS_TAB,
    SCREEN_VALIDATION,
    INLINE_SCREENSHOT,
    FAILS_PROJECTION
}
