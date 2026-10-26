package atf.reporting.runtime.evidence;

import atf.reporting.asset.ReportAssetComposer;

public final class ExtentReportRuntimeEvidenceRoles {
    private ExtentReportRuntimeEvidenceRoles() {
    }

    public static String script() {
        return ReportAssetComposer.extentEvidenceRolesScript();
    }
}

