package atf.core.ui;

/** Identifies whether a UI evidence row came from Playwright or from an audit observation. */
public enum AtfUiEvidenceSource {
    /** A browser operation executed through the Playwright adapter. */
    ACTION,
    /** A reporter-side observation such as a screenshot or tab visibility marker. */
    OBSERVE
}
