package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for hash routing, tab activation and runtime bootstrap. */
final class ExtentReportRuntimeNavigationController {
    private ExtentReportRuntimeNavigationController() { }

    static String script() {
        return ReportAssetComposer.extentNavigationControllerScript();
    }
}
