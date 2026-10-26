package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/** Stable facade for Category status badge rules. */
public final class AtfReportCategoryStatusCss {
    private AtfReportCategoryStatusCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeCategoryStatusCss();
    }
}
