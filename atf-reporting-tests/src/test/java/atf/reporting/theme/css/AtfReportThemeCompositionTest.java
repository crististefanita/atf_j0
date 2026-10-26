package atf.reporting.theme.css;

import atf.core.files.AtfPathLocator;
import atf.testkit.AtfUnitTest;
import atf.reporting.theme.AtfReportTheme;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void baseCssFacadeContainsNoInlineRuleAssembly() throws Exception {
        String source = Files.readString(sourceFile(
                "atf/reporting/theme/css/AtfReportThemeBaseCss.java"));

        assertAll(
                () -> assertTrue(source.contains("ReportAssetComposer.themeBaseCss()")),
                () -> assertFalse(source.contains("return \"")),
                () -> assertFalse(source.contains("AtfReportColors.")),
                () -> assertFalse(source.contains("AtfReportThemeTokens.")));
    }

    @Test
    void nativeThemeFacadesContainNoInlineCssAssembly() throws Exception {
        assertAll(
                () -> assertNativeFacade("atf/reporting/theme/css/AtfReportThemeResponsiveCss.java",
                        "ReportAssetComposer.themeResponsiveCss()"),
                () -> assertNativeFacade("atf/reporting/theme/css/AtfReportCategoryStatusCss.java",
                        "ReportAssetComposer.themeCategoryStatusCss()"),
                () -> assertNativeFacade("atf/reporting/theme/AtfReportFailsLayoutCss.java",
                        "ReportAssetComposer.themeFailsLayoutCss()"),
                () -> assertNativeFacade("atf/reporting/theme/AtfReportFailsStatusCss.java",
                        "ReportAssetComposer.themeFailsStatusCss()"),
                () -> assertNativeFacade("atf/reporting/theme/AtfReportCategoryCss.java",
                        "ReportAssetComposer.themeCategoryCss()"),
                () -> assertNativeFacade("atf/reporting/theme/css/AtfReportThemeNavigationCss.java",
                        "ReportAssetComposer.themeNavigationCss()"),
                () -> assertNativeFacade("atf/reporting/theme/css/AtfReportThemeDetailCss.java",
                        "ReportAssetComposer.themeDetailCss()"),
                () -> assertNativeFacade("atf/reporting/theme/evidence/AtfUiEvidenceCss.java",
                        "ReportAssetComposer.uiEvidenceCss(sectionId, screenshotLinkClass)"));
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
    void compositionRootContainsOrderOnlyAndSemanticLayersOwnCssBlocks() throws Exception {
        String source = Files.readString(sourceFile("atf/reporting/theme/AtfReportTheme.java"));

        assertAll(
                () -> assertTrue(source.contains("AtfReportThemeNavigationCss.css()")),
                () -> assertTrue(source.contains("AtfReportThemeDetailCss.css()")),
                () -> assertFalse(source.contains(".category-view")),
                () -> assertFalse(source.contains(".atf-secondary-metadata")),
                () -> assertFalse(source.contains(".exception-view.atf-fails-view")));
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

    private static Path sourceFile(String relativePath) {
        return AtfPathLocator.locateSourceFile(Path.of(System.getProperty("user.dir", ".")),
                Path.of(relativePath), List.of(Path.of("src/main/java"),
                        Path.of("atf-reporting/src/main/java")));
    }

    private static void assertNativeFacade(String relativePath, String composerCall) throws Exception {
        String source = Files.readString(sourceFile(relativePath));
        assertAll(relativePath,
                () -> assertTrue(source.contains(composerCall)),
                () -> assertFalse(source.contains("return \"")),
                () -> assertFalse(source.contains("+ \"")));
    }
}
