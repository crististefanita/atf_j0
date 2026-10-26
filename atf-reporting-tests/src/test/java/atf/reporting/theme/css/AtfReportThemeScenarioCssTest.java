package atf.reporting.theme.css;
import atf.testkit.AtfUnitTest;

import atf.reporting.theme.AtfReportTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportThemeScenarioCssTest {
    @Test
    void longScenarioTitlesWrapWithoutRemovingTheInlineDisclosureAffordance() {
        String css = AtfReportTheme.css();

        assertTrue(css.contains("white-space:normal!important;overflow:visible!important"));
        assertTrue(css.contains("overflow-wrap:anywhere!important;word-break:break-word!important"));
        assertTrue(css.contains("flex-wrap:nowrap!important"),
                "the disclosure glyph and proposition must remain on one flex row");
        assertTrue(css.contains("card[data-atf-scenario-id]>.card-header:before"));
    }
}
