package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration projection publication browser seam. */
final class ExtentReportDurationProjectionPublication {
    private ExtentReportDurationProjectionPublication() {
    }

    static String script() {
        return ReportAssetComposer.durationProjectionPublicationScript();
    }
}

