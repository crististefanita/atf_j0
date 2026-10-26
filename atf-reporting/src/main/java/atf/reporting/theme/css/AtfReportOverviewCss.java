package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/** Composes the resource-backed CSS owned by the Overview and Category surfaces. */
public final class AtfReportOverviewCss {
    private AtfReportOverviewCss() {
    }

    public static String layout() {
        return ReportAssetComposer.extentOverviewLayoutCss();
    }

    public static String tagAudit() {
        return ReportAssetComposer.extentOverviewTagAuditCss();
    }

    public static String categoryTables() {
        return ReportAssetComposer.extentOverviewCategoryTablesCss();
    }

    /** Complete Overview metrics surface, without the separately owned status badges. */
    public static String metrics() {
        return String.join("", layout(), tagAudit(), categoryTables());
    }

    public static String categoryStatus() {
        return ReportAssetComposer.extentOverviewCategoryStatusCss();
    }
}
