package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration aggregation browser seam. */
final class ExtentReportDurationAggregation {
    private ExtentReportDurationAggregation() {
    }

    static String script() {
        return ReportAssetComposer.durationAggregationScript();
    }
}

