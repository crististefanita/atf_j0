package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration parsing browser seam. */
final class ExtentReportDurationParsing {
    private ExtentReportDurationParsing() {
    }

    static String script() {
        return ReportAssetComposer.durationParsingScript();
    }
}

