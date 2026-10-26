package atf.reporting.runtime.failure;

import atf.reporting.asset.ReportAssetComposer;

/** Java facade for the native failure-detection asset. */
public final class ExtentReportFailureDetection {
    private ExtentReportFailureDetection() {
    }

    public static String script() {
        return ReportAssetComposer.extentFailureDetectionScript();
    }
}
