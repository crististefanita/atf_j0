package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for Overview chart palette and percentage normalization. */
final class ExtentReportRuntimeOverviewCharts {
    private ExtentReportRuntimeOverviewCharts() { }

    static String script() {
        return ReportAssetComposer.extentOverviewChartsScript();
    }
}
