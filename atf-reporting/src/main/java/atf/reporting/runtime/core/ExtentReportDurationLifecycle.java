package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration lifecycle browser seam. */
final class ExtentReportDurationLifecycle {
    private ExtentReportDurationLifecycle() {
    }

    static String script() {
        return ReportAssetComposer.durationLifecycleScript();
    }
}

