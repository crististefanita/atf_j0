package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration scheduling browser seam. */
final class ExtentReportDurationScheduling {
    private ExtentReportDurationScheduling() {
    }

    static String script() {
        return ReportAssetComposer.durationSchedulingScript();
    }
}

