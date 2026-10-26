package atf.reporting.html.script;

import atf.reporting.asset.ReportAssetComposer;

/**
 * Navigation behaviour for the renderer-neutral fallback report.
 *
 * <p>The executable browser code lives in the native fallback-navigation.js
 * asset. This class remains the typed report-script port used by
 * {@code HtmlReportSession}; it does not rebuild JavaScript from Java
 * fragments.</p>
 */
public final class HtmlReportNavigationScript implements HtmlReportScript {
    private static final HtmlReportNavigationScript STANDARD = new HtmlReportNavigationScript();

    private HtmlReportNavigationScript() {
    }

    public static HtmlReportScript standard() {
        return STANDARD;
    }

    @Override
    public String source() {
        return ReportAssetComposer.fallbackNavigationScript();
    }
}
