package atf.reporting.runtime.failure;

import atf.reporting.asset.ReportAssetComposer;

/** Java facade for native failure context and cause-chain assets. */
public final class ExtentReportFailureContext {
    private ExtentReportFailureContext() {
    }

    public static String script() {
        return ReportAssetComposer.extentFailureContextScript();
    }

    public static String causeChainScript() {
        return ReportAssetComposer.extentFailureCauseChainScript();
    }
}
