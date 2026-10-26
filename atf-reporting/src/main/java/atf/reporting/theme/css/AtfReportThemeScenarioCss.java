package atf.reporting.theme.css;

import atf.reporting.asset.ReportAssetComposer;

/**
 * Scenario-title rules kept separate from the general detail layer.
 *
 * <p>Long, declarative Gherkin propositions are report content, not a reason
 * to hide text behind an ellipsis. The title keeps the inline plus/minus
 * affordance supplied by the parent card while wrapping its own text inside
 * the available column.</p>
 */
public final class AtfReportThemeScenarioCss {
    private AtfReportThemeScenarioCss() {
    }

    public static String css() {
        return ReportAssetComposer.themeScenarioCss();
    }
}
