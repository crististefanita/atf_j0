package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/**
 * Semantic base layer for the Extent theme: shared surfaces, test-card
 * structure, scenario annotations and status-badge geometry.
 *
 * <p>This is intentionally separate from navigation, evidence and Overview
 * rules. The composition root can therefore change one visual responsibility
 * without turning the entire renderer stylesheet into one hotspot.</p>
 */
public final class AtfReportThemeBaseCss {
    private AtfReportThemeBaseCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeBaseCss();
    }
}
