package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for optional browser runtime performance diagnostics. */
final class ExtentReportRuntimeNavigationPerformance {
    private ExtentReportRuntimeNavigationPerformance() { }

    static String script() {
        return ReportAssetComposer.extentNavigationPerformanceScript();
    }
}
