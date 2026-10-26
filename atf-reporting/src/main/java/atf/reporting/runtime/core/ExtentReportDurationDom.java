package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration DOM browser seam. */
final class ExtentReportDurationDom {
    private ExtentReportDurationDom() {
    }

    static String script() {
        return ReportAssetComposer.durationDomScript();
    }
}

