package atf.reporting.evidence.profile;

import atf.reporting.model.ReportScreenshot;

/** Consumer-owned source of a screenshot; reporting never controls a browser. */
public interface ReportEvidenceCaptureProvider {
    ReportEvidenceCaptureSource source();

    /** Called only when the selected policy permits a capture at this point. */
    ReportScreenshot capture(String title, String target);
}
