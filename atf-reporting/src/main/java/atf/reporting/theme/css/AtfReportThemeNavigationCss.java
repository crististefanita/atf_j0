package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/** Stable facade for native navigation, tabs and phase rules. */
public final class AtfReportThemeNavigationCss {
    private AtfReportThemeNavigationCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeNavigationCss();
    }
}
