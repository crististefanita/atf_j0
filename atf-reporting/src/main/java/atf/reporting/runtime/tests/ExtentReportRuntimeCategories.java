package atf.reporting.runtime.tests;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for Categories filtering and linked detail projection. */
public final class ExtentReportRuntimeCategories {
    private ExtentReportRuntimeCategories() { }

    public static String script() {
        return ReportAssetComposer.extentCategoriesScript();
    }
}
