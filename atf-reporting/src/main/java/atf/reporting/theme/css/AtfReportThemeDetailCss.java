package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/** Stable facade for native detail and failure-projection rules. */
public final class AtfReportThemeDetailCss {
    private AtfReportThemeDetailCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeDetailCss();
    }
}
