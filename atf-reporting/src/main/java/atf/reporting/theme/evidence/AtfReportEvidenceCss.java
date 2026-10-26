package atf.reporting.theme.evidence;

/** Composition root for the evidence CSS providers. */
public final class AtfReportEvidenceCss {
    private AtfReportEvidenceCss() {
    }

    public static String css() {
        return AtfReportEvidenceTableCss.css()
                + AtfReportEvidenceInteractionCss.css()
                + AtfReportEvidenceDisclosureCss.css()
                + AtfEvidenceStackCss.css();
    }
}
