package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for removing unsupported vendor Overview cards. */
final class ExtentReportRuntimeOverviewCleanup {
    private ExtentReportRuntimeOverviewCleanup() { }

    static String script() {
        return ReportAssetComposer.extentOverviewCleanupScript();
    }
}
