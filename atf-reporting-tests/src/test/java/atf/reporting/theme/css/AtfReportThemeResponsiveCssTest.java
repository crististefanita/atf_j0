package atf.reporting.theme.css;
import atf.testkit.AtfUnitTest;

import atf.reporting.asset.ReportAsset;
import atf.reporting.asset.ReportAssetLoader;
import atf.reporting.theme.AtfReportTheme;
import atf.reporting.theme.tokens.AtfReportThemeTokens;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the narrow static-report layout to the viewport edge. */
@AtfUnitTest
class AtfReportThemeResponsiveCssTest {

    @Test
    void keepsTheTestWrapperInsideTheViewportOnNarrowScreens() {
        String css = AtfReportThemeResponsiveCss.css();
        String theme = AtfReportTheme.css();
        String tokens = AtfReportThemeTokens.cssVariables();
        String source = new ReportAssetLoader().load(ReportAsset.EXTENT_THEME_RESPONSIVE_CSS);

        assertAll(
                () -> assertTrue(css.contains("@media only screen and (max-width:992px)")),
                () -> assertTrue(css.contains(".test-wrapper,.test-view,#category-view:not(.hide){")),
                () -> assertTrue(css.contains("left:var(--atf-narrow-nav-inset)!important")),
                () -> assertTrue(css.contains("width:calc(100% - var(--atf-narrow-nav-inset))!important")),
                () -> assertTrue(css.contains(
                        ".test-wrapper .test-list,.test-view .test-list,#category-view .test-list{")),
                () -> assertTrue(css.contains("float:none!important")),
                () -> assertTrue(css.contains("flex:0 0 auto!important")),
                () -> assertTrue(css.contains(
                        ".test-wrapper .test-content,.test-view .test-content,#category-view .test-content{")),
                () -> assertTrue(css.contains("left:auto!important")),
                () -> assertTrue(source.contains("${NARROW_VIEWPORT}")),
                () -> assertTrue(css.contains("@media only screen and (max-width:" + AtfReportThemeTokens.NARROW_VIEWPORT + ")")),
                () -> assertFalse(css.contains("${")),
                () -> assertTrue(tokens.contains("--atf-narrow-nav-inset:" + AtfReportThemeTokens.NARROW_NAV_INSET)),
                () -> assertTrue(theme.contains(".test-wrapper,.test-view,#category-view:not(.hide){")
                        && theme.contains("left:var(--atf-narrow-nav-inset)!important")));
    }
}
