package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration projection lookup browser seam. */
final class ExtentReportDurationProjectionLookup {
    private ExtentReportDurationProjectionLookup() {
    }

    static String script() {
        return ReportAssetComposer.durationProjectionLookupScript();
    }
}

