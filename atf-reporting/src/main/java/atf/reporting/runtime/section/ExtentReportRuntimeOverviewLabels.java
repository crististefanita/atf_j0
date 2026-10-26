package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for Overview and Categories label normalization. */
final class ExtentReportRuntimeOverviewLabels {
    private ExtentReportRuntimeOverviewLabels() { }

    static String script() {
        return ReportAssetComposer.extentOverviewLabelsScript();
    }
}
