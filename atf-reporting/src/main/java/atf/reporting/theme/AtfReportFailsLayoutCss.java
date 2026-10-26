package atf.reporting.theme;

import atf.reporting.asset.ReportAssetComposer;

/** Stable facade for the native Fails list/detail layout stylesheet. */
public final class AtfReportFailsLayoutCss {
    private AtfReportFailsLayoutCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeFailsLayoutCss();
    }
}
