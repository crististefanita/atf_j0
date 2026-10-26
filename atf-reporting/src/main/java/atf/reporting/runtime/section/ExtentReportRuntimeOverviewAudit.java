package atf.reporting.runtime.section;

import atf.reporting.asset.ReportAssetComposer;

/** Native-resource facade for Overview status counters and audit-table projection. */
final class ExtentReportRuntimeOverviewAudit {
    private ExtentReportRuntimeOverviewAudit() { }

    static String script() {
        return ReportAssetComposer.extentOverviewAuditScript();
    }
}
