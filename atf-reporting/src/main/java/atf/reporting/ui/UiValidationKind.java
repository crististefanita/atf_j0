package atf.reporting.ui;

/** Internal ownership of a checkpoint's validation evidence. */
enum UiValidationKind {
    STANDARD("UI validation", false),
    VISUAL("UI validation", true),
    SETUP("Setup validation", false);

    private final String reportTitle;
    private final boolean visualEvidence;

    UiValidationKind(String reportTitle, boolean visualEvidence) {
        this.reportTitle = reportTitle;
        this.visualEvidence = visualEvidence;
    }

    String reportTitle() {
        return reportTitle;
    }

    boolean hasVisualEvidence() {
        return visualEvidence;
    }
}
