package atf.reporting.runtime.tests;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for scenario semantic markers and annotation projection. */
public final class ExtentReportRuntimeScenarios {
    private ExtentReportRuntimeScenarios() { }

    public static String script() {
        return ReportAssetComposer.extentScenariosScript();
    }
}
