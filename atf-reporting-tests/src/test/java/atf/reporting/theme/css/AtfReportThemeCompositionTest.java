package atf.reporting.theme.css;

import atf.testkit.AtfUnitTest;
import atf.reporting.theme.AtfReportTheme;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the semantic seam between the theme composition root and base CSS. */
@AtfUnitTest
class AtfReportThemeCompositionTest {
    @Test
    void baseCssOwnsSharedSurfaceAndTestStructureWithoutRendererMarkup() {
        String base = AtfReportThemeBaseCss.css();

        assertAll(
                () -> assertTrue(base.contains(".test-view .test-content-detail")),
                () -> assertTrue(base.contains("data-atf-scenario-id")),
                () -> assertTrue(base.contains("--atf-badge-height")),
                () -> assertTrue(base.contains(".atf-scenario-annotations")),
                () -> assertFalse(base.contains("<style")),
                () -> assertFalse(base.contains("<script")));
    }

    @Test
    void compositionKeepsBaseEvidenceAndProjectionLayersTogether() {
        String theme = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(theme.indexOf("html,body,body.dark") < theme.indexOf("pre.code-block")),
                () -> assertTrue(theme.contains(".atf-ui-action-sequence")),
                () -> assertTrue(theme.contains(".category-view")),
                () -> assertTrue(theme.contains(".atf-secondary-overview")),
                () -> assertTrue(theme.contains(".atf-fails-view")));
    }

    @Test
    void semanticExtractionPreservesTheCssCompositionContract() {
        String css = AtfReportTheme.css();

        assertAll(
                () -> assertTrue(css.length() > 80_000),
                () -> assertTrue(css.contains(".atf-ui-action-table")),
                () -> assertTrue(css.contains(".atf-ui-action-operation")),
                () -> assertTrue(css.contains(".atf-ui-action-screenshot-link")),
                () -> assertFalse(css.contains("rgb(")),
                () -> assertFalse(css.contains("rgba(")));
    }

}
