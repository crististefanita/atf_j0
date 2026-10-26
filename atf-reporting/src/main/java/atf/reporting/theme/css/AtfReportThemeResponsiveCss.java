package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/**
 * Responsive layout facade for the Extent test split view.
 *
 * <p>Extent's desktop layout keeps the test list and the selected test side by
 * side.  Its mobile rule intentionally moves the wrapper by the desktop
 * sidebar width, which leaves a 200px horizontal offset in a static report.
 * The report remains usable on a narrow viewport when the wrapper starts at
 * the viewport edge; the existing list/content transition still controls
 * which pane is visible.</p>
 */
public final class AtfReportThemeResponsiveCss {
    private AtfReportThemeResponsiveCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeResponsiveCss();
    }
}
