package atf.reporting.theme;
import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.theme.css.AtfReportThemeBaseCss;
import atf.reporting.theme.css.AtfReportThemeDetailCss;
import atf.reporting.theme.css.AtfReportThemeNavigationCss;
import atf.reporting.theme.css.AtfReportThemeResponsiveCss;
import atf.reporting.theme.css.AtfReportThemeScenarioCss;
import atf.reporting.theme.evidence.AtfReportEvidenceCss;
import atf.reporting.theme.tokens.AtfFallbackTheme;
import atf.reporting.theme.tokens.AtfReportThemeTokens;

/**
 * Composition root for the visual contract shared by the Java report and the
 * reference .NET ATF report. Semantic CSS layers own their own surface; this
 * class only defines their order and public rendering entry points.
 */
public final class AtfReportTheme {
    public static final String FONT_STACK = AtfReportThemeTokens.FONT_STACK;

    private AtfReportTheme() {
    }

    public static String sparkCss() {
        return "body{font-family:" + FONT_STACK + ";line-height:"
                + AtfReportThemeTokens.BASE_LINE_HEIGHT + ";}";
    }

    public static String fallbackCss() {
        return AtfFallbackTheme.css();
    }

    public static String css() {
        return AtfReportThemeTokens.cssVariables()
                + AtfReportThemeBaseCss.css()
                + AtfReportEvidenceCss.css()
                + AtfReportThemeNavigationCss.css()
                + AtfReportThemeResponsiveCss.css()
                + AtfReportFailsLayoutCss.css()
                + AtfReportThemeDetailCss.css()
                + AtfReportThemeScenarioCss.css()
                + ReportAssetComposer.tableViewportCss();
    }
}

