package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for scenario, phase and node disclosure behavior. */
final class ExtentReportRuntimeNavigationDisclosure {
    private ExtentReportRuntimeNavigationDisclosure() { }

    static String script() {
        return ReportAssetComposer.extentNavigationDisclosureScript();
    }
}
