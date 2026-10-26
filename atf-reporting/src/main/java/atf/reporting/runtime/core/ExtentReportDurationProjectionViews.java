package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration projection views browser seam. */
final class ExtentReportDurationProjectionViews {
    private ExtentReportDurationProjectionViews() {
    }

    static String script() {
        return ReportAssetComposer.durationProjectionViewsScript();
    }
}

