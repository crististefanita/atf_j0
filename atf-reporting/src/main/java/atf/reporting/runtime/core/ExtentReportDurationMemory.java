package atf.reporting.runtime.core;

import atf.reporting.asset.ReportAssetComposer;

/** Native-asset facade for the duration memory browser seam. */
final class ExtentReportDurationMemory {
    private ExtentReportDurationMemory() {
    }

    static String script() {
        return ReportAssetComposer.durationMemoryScript();
    }
}

