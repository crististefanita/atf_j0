package atf.reporting.standalone.source;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

/** Producer-only source contracts travel with the parentless library build. */
class ReportingSourceArchitectureTest {
    @Test
    void coreFacadeDoesNotReassembleBrowserFunctionsInJava() throws Exception {
        String source = source("atf/reporting/runtime/core/ExtentReportRuntimeCore.java");

        assertAll(
                () -> assertTrue(source.contains("ReportAssetComposer.runtimeCoreDomScript()")),
                () -> assertTrue(source.contains("ReportAssetComposer.runtimeCoreScenariosScript()")),
                () -> assertTrue(source.contains("ReportAssetComposer.runtimeCoreMetadataScript()")),
                () -> assertFalse(source.contains("function ")),
                () -> assertFalse(source.contains("StringBuilder")),
                () -> assertFalse(source.contains("+ \"")));
    }

    @Test
    void durationSeamsAreNativeAssetFacades() throws Exception {
        assertAll(
                () -> assertDurationFacade("ExtentReportDurationParsing.java", "durationParsingScript()"),
                () -> assertDurationFacade("ExtentReportDurationDom.java", "durationDomScript()"),
                () -> assertDurationFacade("ExtentReportDurationMemory.java", "durationMemoryScript()"),
                () -> assertDurationFacade("ExtentReportDurationAggregation.java", "durationAggregationScript()"),
                () -> assertDurationFacade("ExtentReportDurationProjectionLookup.java", "durationProjectionLookupScript()"),
                () -> assertDurationFacade("ExtentReportDurationProjectionViews.java", "durationProjectionViewsScript()"),
                () -> assertDurationFacade("ExtentReportDurationProjectionPublication.java",
                        "durationProjectionPublicationScript()"),
                () -> assertDurationFacade("ExtentReportDurationScheduling.java", "durationSchedulingScript()"),
                () -> assertDurationFacade("ExtentReportDurationLifecycle.java", "durationLifecycleScript()"));
    }

    @Test
    void baseCssFacadeContainsNoInlineRuleAssembly() throws Exception {
        String source = source(
                "atf/reporting/theme/css/AtfReportThemeBaseCss.java");

        assertAll(
                () -> assertTrue(source.contains("ReportAssetComposer.themeBaseCss()")),
                () -> assertFalse(source.contains("return \"")),
                () -> assertFalse(source.contains("AtfReportColors.")),
                () -> assertFalse(source.contains("AtfReportThemeTokens.")));
    }

    @Test
    void nativeThemeFacadesContainNoInlineCssAssembly() throws Exception {
        assertAll(
                () -> assertThemeFacade("atf/reporting/theme/css/AtfReportThemeResponsiveCss.java",
                        "ReportAssetComposer.themeResponsiveCss()"),
                () -> assertThemeFacade("atf/reporting/theme/css/AtfReportCategoryStatusCss.java",
                        "ReportAssetComposer.themeCategoryStatusCss()"),
                () -> assertThemeFacade("atf/reporting/theme/AtfReportFailsLayoutCss.java",
                        "ReportAssetComposer.themeFailsLayoutCss()"),
                () -> assertThemeFacade("atf/reporting/theme/AtfReportFailsStatusCss.java",
                        "ReportAssetComposer.themeFailsStatusCss()"),
                () -> assertThemeFacade("atf/reporting/theme/AtfReportCategoryCss.java",
                        "ReportAssetComposer.themeCategoryCss()"),
                () -> assertThemeFacade("atf/reporting/theme/css/AtfReportThemeNavigationCss.java",
                        "ReportAssetComposer.themeNavigationCss()"),
                () -> assertThemeFacade("atf/reporting/theme/css/AtfReportThemeDetailCss.java",
                        "ReportAssetComposer.themeDetailCss()"),
                () -> assertThemeFacade("atf/reporting/theme/evidence/AtfUiEvidenceCss.java",
                        "ReportAssetComposer.uiEvidenceCss(sectionId, screenshotLinkClass)"));
    }

    @Test
    void compositionRootContainsOrderOnlyAndSemanticLayersOwnCssBlocks() throws Exception {
        String source = source("atf/reporting/theme/AtfReportTheme.java");

        assertAll(
                () -> assertTrue(source.contains("AtfReportThemeNavigationCss.css()")),
                () -> assertTrue(source.contains("AtfReportThemeDetailCss.css()")),
                () -> assertFalse(source.contains(".category-view")),
                () -> assertFalse(source.contains(".atf-secondary-metadata")),
                () -> assertFalse(source.contains(".exception-view.atf-fails-view")));
    }

    @Test
    void publicFacadeDelegatesLifecycleStateToFocusedCollaborators() throws Exception {
        String source = source("atf/reporting/ui/UiExtentReportSession.java");

        assertAll(
                () -> assertTrue(source.contains("UiScenarioLifecycle")),
                () -> assertTrue(source.contains("UiPhaseLifecycle")),
                () -> assertFalse(source.contains("new UiEvidencePublisher")),
                () -> assertFalse(source.contains("delegateScenario.startPhase")),
                () -> assertTrue(source.contains("lifecycle.uiValidationAsMedia")),
                () -> assertTrue(source.contains("openConfigured(UiExtentReportOptions options)")),
                () -> assertTrue(source.contains("return open(configuration, options)")));
    }

    @Test
    void lifecycleCollaboratorsOwnCompletionGuardsAndEvidencePublication() throws Exception {
        String scenario = source("atf/reporting/ui/UiScenarioLifecycle.java");
        String phase = source("atf/reporting/ui/UiPhaseLifecycle.java");

        assertAll(
                () -> assertTrue(scenario.contains("UI scenario is already complete")),
                () -> assertTrue(scenario.contains("delegate.complete")),
                () -> assertTrue(phase.contains("UI phase is already complete")),
                () -> assertTrue(phase.contains("publishAction")),
                () -> assertTrue(phase.contains("publishFailure")),
                () -> assertFalse(phase.contains("Page")));
    }

    private static String source(String path) throws Exception {
        return Files.readString(ReportingModuleSource.root().resolve("src/main/java").resolve(path));
    }
    private static void assertDurationFacade(String file, String composerCall) throws Exception {
        String text=source("atf/reporting/runtime/core/"+file);
        assertAll(file,
                ()->assertTrue(text.contains("ReportAssetComposer."+composerCall)),
                ()->assertFalse(text.contains("function ")),
                ()->assertFalse(text.contains("String.join")),
                ()->assertFalse(text.contains("+ \"")));
    }
    private static void assertThemeFacade(String file, String composerCall) throws Exception {
        String text=source(file);
        assertAll(file,
                ()->assertTrue(text.contains(composerCall)),
                ()->assertFalse(text.contains("return \"")),
                ()->assertFalse(text.contains("+ \"")));
    }
}
