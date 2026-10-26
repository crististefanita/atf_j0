package atf.reporting.theme;

import atf.reporting.asset.ReportAssetComposer;

/** Stable facade for Categories empty-state rules. */
public final class AtfReportCategoryCss {
    private AtfReportCategoryCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeCategoryCss();
    }
}
