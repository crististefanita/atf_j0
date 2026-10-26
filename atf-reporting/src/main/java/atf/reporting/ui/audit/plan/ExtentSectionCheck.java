package atf.reporting.ui.audit.plan;

/** Typed checks a section module may expose without leaking DOM details. */
public enum ExtentSectionCheck {
    STRUCTURE,
    COLLAPSED_BY_DEFAULT,
    JSON_CONTRACT,
    MOCK_RECONCILIATION,
    STYLE,
    SCREENSHOT_LINK,
    INLINE_ALIGNMENT,
    CROSS_SECTION
}

