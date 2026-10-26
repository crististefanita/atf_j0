package atf.reporting.runtime.evidence;

import atf.reporting.asset.ReportAssetComposer;

/** Java facade for the native evidence-failures asset. */
public final class ExtentReportRuntimeEvidenceFailures {
    private ExtentReportRuntimeEvidenceFailures() {
    }

    public static String script() {
        return ReportAssetComposer.extentEvidenceFailuresScript();
    }
}
