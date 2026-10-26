package atf.reporting.asset;

import atf.reporting.theme.tokens.AtfReportThemeTokens;
import atf.reporting.runtime.tests.ExtentReportRuntimeTestFilters;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportAssetContractTest {
    private final ReportAssetLoader loader = new ReportAssetLoader();

    @Test
    void everyDeclaredAssetIsPackagedWithItsSemanticFormat() {
        for (ReportAsset asset : ReportAssetManifest.production().assets()) {
            String source = loader.load(asset);
            assertAll(asset.name(),
                    () -> assertFalse(source.isBlank()),
                    () -> assertTrue(asset.resourcePath().startsWith("/atf/reporting/")),
                    () -> assertFalse(asset.owner().isBlank()),
                    () -> assertTrue(source.length() > 100));
        }

        assertEquals(ReportAsset.Format.CSS, ReportAsset.FALLBACK_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EVIDENCE_INTERACTION_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EVIDENCE_TABLE_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EVIDENCE_DISCLOSURE_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EVIDENCE_STACK_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.UI_EVIDENCE_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_OVERVIEW_LAYOUT_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_OVERVIEW_TAG_AUDIT_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_OVERVIEW_CATEGORY_TABLES_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_OVERVIEW_CATEGORY_STATUS_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_RESPONSIVE_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_BASE_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_CATEGORY_STATUS_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_FAILS_LAYOUT_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_FAILS_STATUS_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_CATEGORY_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_NAVIGATION_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_SCENARIO_CSS.format());
        assertEquals(ReportAsset.Format.CSS, ReportAsset.EXTENT_THEME_DETAIL_CSS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_TEST_FILTERS_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_NAVIGATION_DOM_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_NAVIGATION_DISCLOSURE_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_NAVIGATION_PERFORMANCE_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_NAVIGATION_CONTROLLER_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_OVERVIEW_AUDIT_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_OVERVIEW_LABELS_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_OVERVIEW_CHARTS_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_OVERVIEW_CLEANUP_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_SCENARIOS_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_CATEGORIES_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.FALLBACK_NAVIGATION_JS.format());
        assertEquals("extent-evidence-roles", ReportAsset.EXTENT_EVIDENCE_ROLES_JS.owner());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_EVIDENCE_DISCLOSURE_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_EVIDENCE_FAILURES_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_EVIDENCE_TABLE_LAYOUT_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_FAILURE_DETECTION_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_FAILURE_CONTEXT_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_FAILURE_CAUSE_CHAIN_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.EXTENT_FAILURE_PROJECTION_JS.format());
        assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.RUNTIME_BOOT_JS.format());
    }

    @Test
    void keepsNativeSourcesReadableAndPublicationCopiesCompact() {
        for (ReportAsset asset : ReportAssetManifest.production().assets()) {
            String source = loader.load(asset);
            String publication = loader.loadForPublication(asset);
            assertAll(asset.name(),
                    () -> assertTrue(source.contains("\n"), "native source should stay formatted"),
                    () -> assertFalse(publication.contains("\n"),
                            "publication copy must not carry source newlines"),
                    () -> assertFalse(publication.contains("\r"),
                            "publication copy must not carry source carriage returns"));
        }
    }

    @Test
    void productionManifestIsTypedCompleteAndDeterministic() {
        ReportAssetManifest manifest = ReportAssetComposer.manifest();

        assertAll(
                () -> assertEquals(Arrays.asList(ReportAsset.values()), manifest.assets()),
                () -> assertEquals(manifest.assets().size(),
                        manifest.cssAssets().size() + manifest.javascriptAssets().size()),
                () -> assertEquals(List.of(ReportAsset.Format.CSS),
                        manifest.cssAssets().stream().map(ReportAsset::format).distinct().toList()),
                () -> assertEquals(List.of(ReportAsset.Format.JAVASCRIPT),
                        manifest.javascriptAssets().stream().map(ReportAsset::format).distinct().toList()),
                () -> assertTrue(manifest.assets().stream()
                        .allMatch(asset -> asset.resourcePath().endsWith(asset.format().extension()))));
    }

    @Test
    void manifestRejectsDuplicatesAndMissingAssets() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ReportAssetManifest.of(List.of(
                                ReportAsset.FALLBACK_CSS, ReportAsset.FALLBACK_CSS))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ReportAssetManifest.of(List.of(ReportAsset.FALLBACK_CSS))));
    }

    @Test
    void fallbackAssetsAreExpandedAndRemainTheRendererContract() {
        String css = ReportAssetComposer.fallbackCss();
        String evidenceCss = ReportAssetComposer.evidenceInteractionCss();
        String evidenceTableCss = ReportAssetComposer.evidenceTableCss();
        String disclosureCss = ReportAssetComposer.evidenceDisclosureCss();
        String stackCss = ReportAssetComposer.evidenceStackCss();
        String uiEvidenceCss = ReportAssetComposer.uiEvidenceCss(
                AtfReportHtmlVocabulary.ElementId.UI_EVIDENCE,
                AtfReportHtmlVocabulary.CssClass.ATF_UI_EVIDENCE_SCREENSHOT_LINK);
        String filters = ExtentReportRuntimeTestFilters.script();
        String navigationDom = ReportAssetComposer.extentNavigationDomScript();
        String navigationDisclosure = ReportAssetComposer.extentNavigationDisclosureScript();
        String navigationPerformance = ReportAssetComposer.extentNavigationPerformanceScript();
        String navigationController = ReportAssetComposer.extentNavigationControllerScript();
        String overviewAudit = ReportAssetComposer.extentOverviewAuditScript();
        String overviewLabels = ReportAssetComposer.extentOverviewLabelsScript();
        String overviewCharts = ReportAssetComposer.extentOverviewChartsScript();
        String overviewCleanup = ReportAssetComposer.extentOverviewCleanupScript();
        String scenarios = ReportAssetComposer.extentScenariosScript();
        String categories = ReportAssetComposer.extentCategoriesScript();
        String evidenceDisclosure = ReportAssetComposer.extentEvidenceDisclosureScript();
        String evidenceFailures = ReportAssetComposer.extentEvidenceFailuresScript();
        String evidenceTableLayout = ReportAssetComposer.extentEvidenceTableLayoutScript();
        String overviewLayout = ReportAssetComposer.extentOverviewLayoutCss();
        String overviewTagAudit = ReportAssetComposer.extentOverviewTagAuditCss();
        String overviewCategoryTables = ReportAssetComposer.extentOverviewCategoryTablesCss();
        String overviewCategoryStatus = ReportAssetComposer.extentOverviewCategoryStatusCss();
        String themeResponsive = ReportAssetComposer.themeResponsiveCss();
        String themeBase = ReportAssetComposer.themeBaseCss();
        String themeCategoryStatus = ReportAssetComposer.themeCategoryStatusCss();
        String themeFailsLayout = ReportAssetComposer.themeFailsLayoutCss();
        String themeFailsStatus = ReportAssetComposer.themeFailsStatusCss();
        String themeCategory = ReportAssetComposer.themeCategoryCss();
        String themeNavigation = ReportAssetComposer.themeNavigationCss();
        String themeScenario = ReportAssetComposer.themeScenarioCss();
        String themeDetail = ReportAssetComposer.themeDetailCss();
        String failureDetection = ReportAssetComposer.extentFailureDetectionScript();
        String failureContext = ReportAssetComposer.extentFailureContextScript();
        String failureCauseChain = ReportAssetComposer.extentFailureCauseChainScript();
        String failureProjection = ReportAssetComposer.extentFailureProjectionScript();
        String script = ReportAssetComposer.fallbackNavigationScript();
        String runtimeBoot = ReportAssetComposer.runtimeBootScript("function q(){};");

        assertAll(
                () -> assertFalse(css.contains("${")),
                () -> assertTrue(css.contains("font-family:" + AtfReportThemeTokens.FONT_STACK)),
                () -> assertTrue(css.contains(".tab.active")),
                () -> assertTrue(css.contains(".fail-row")),
                () -> assertFalse(evidenceCss.contains("${")),
                () -> assertTrue(evidenceCss.contains(".atf-ui-action-sequence>summary")),
                () -> assertTrue(evidenceCss.contains(".atf-ui-action-screenshot-link")),
                () -> assertFalse(evidenceTableCss.contains("${")),
                () -> assertTrue(evidenceTableCss.contains(".helm-evidence-table")),
                () -> assertTrue(disclosureCss.contains(".step-detail>summary")),
                () -> assertTrue(disclosureCss.contains(".atf-disclosure-glyph:before")),
                () -> assertTrue(disclosureCss.contains("details.failure-evidence")),
                () -> assertTrue(stackCss.contains("--atf-evidence-status-column-width")),
                () -> assertTrue(stackCss.contains("data-atf-evidence-last='true'")
                        || stackCss.contains("data-atf-evidence-last=\"true\""),
                        "the evidence-last marker must remain present regardless of quote style"),
                () -> assertTrue(stackCss.contains("@media (max-width:700px)")),
                () -> assertFalse(uiEvidenceCss.contains("${")),
                () -> assertTrue(uiEvidenceCss.contains("#atf-ui-exploratory-evidence")),
                () -> assertTrue(uiEvidenceCss.contains(".atf-ui-evidence-screenshot-link")),
                () -> assertFalse(filters.contains("${")),
                () -> assertTrue(filters.contains("var atfFilterState={status:'',tag:''}")),
                () -> assertTrue(filters.contains("function applyTestFilters(")),
                () -> assertTrue(filters.contains("data-atf-filter-hidden")),
                () -> assertTrue(navigationDom.contains("function captureDomContract(")),
                () -> assertTrue(navigationDisclosure.contains("function bindScenarioCollapse(")),
                () -> assertTrue(navigationDisclosure.contains("function bindNodeCollapse(")),
                () -> assertTrue(navigationPerformance.contains("function atfPerformanceSnapshot(")),
                () -> assertTrue(navigationPerformance.contains("firstDisclosureToggleMs")),
                () -> assertTrue(navigationPerformance.contains("data-atf-performance-ready")),
                () -> assertTrue(navigationPerformance.contains("longtask")),
                () -> assertTrue(navigationController.contains("window.atfActivate=function")),
                () -> assertFalse(navigationDom.contains("${")),
                () -> assertFalse(navigationDisclosure.contains("${")),
                () -> assertFalse(navigationPerformance.contains("${")),
                () -> assertFalse(navigationController.contains("${")),
                () -> assertTrue(overviewAudit.contains("function populateAudit(")),
                () -> assertTrue(overviewLabels.contains("function normalizeTagTableHeaders(")),
                () -> assertTrue(overviewCharts.contains("function normalizeChartPalettes(")),
                () -> assertTrue(overviewCleanup.contains("function removeUnsupportedOverviewCards(")),
                () -> assertTrue(scenarios.contains("function normalizeScenarioAnnotations(")),
                () -> assertTrue(categories.contains("function ensureCategoryFilters(")),
                () -> assertTrue(categories.contains("function ensureCategoryDetail(")),
                () -> assertTrue(evidenceDisclosure.contains("function normalizeEvidenceDisclosures(")),
                () -> assertTrue(evidenceDisclosure.contains("function normalizeSemanticEvidenceDisclosures(")),
                () -> assertTrue(evidenceFailures.contains("function appendUiFailureEvidence(")),
                () -> assertTrue(evidenceFailures.contains("function markLastSemanticEvidence(")),
                () -> assertTrue(evidenceTableLayout.contains("function normalizeValidationTableColumns(")),
                () -> assertTrue(evidenceTableLayout.contains("data-atf-validation-column-widths")),
                () -> assertFalse(overviewLayout.contains("${")),
                () -> assertTrue(overviewLayout.contains(".atf-secondary-overview>summary")),
                () -> assertFalse(overviewTagAudit.contains("${")),
                () -> assertTrue(overviewTagAudit.contains(".atf-tag-table")),
                () -> assertFalse(overviewCategoryTables.contains("${")),
                () -> assertTrue(overviewCategoryTables.contains(".category-container")),
                () -> assertTrue(overviewCategoryStatus.contains(".category-view .test-list")),
                () -> assertFalse(themeResponsive.contains("${")),
                () -> assertTrue(themeResponsive.contains("@media only screen and (max-width:992px)")),
                () -> assertFalse(themeBase.contains("${")),
                () -> assertTrue(themeBase.contains(".test-view .test-content-detail")),
                () -> assertTrue(themeBase.contains("font-family:" + AtfReportThemeTokens.FONT_STACK)),
                () -> assertFalse(themeCategoryStatus.contains("${")),
                () -> assertTrue(themeCategoryStatus.contains(".category-view .test-list")),
                () -> assertTrue(themeFailsLayout.contains("#atf-fails-view")),
                () -> assertTrue(themeFailsStatus.contains(".test-status .badge")),
                () -> assertTrue(themeCategory.contains(".atf-empty-state")),
                () -> assertFalse(themeNavigation.contains("${")),
                () -> assertTrue(themeNavigation.contains(".category-view")),
                () -> assertTrue(themeNavigation.contains(".detail-head .info>.uri-anchor{display:none!important}")),
                () -> assertFalse(themeNavigation.matches("(?s).*\\n\\s*//.*"),
                        "rendered CSS must not contain JavaScript-style line comments"),
                () -> assertFalse(themeScenario.contains("${")),
                () -> assertTrue(themeScenario.contains("overflow-wrap:anywhere!important;word-break:break-word!important")),
                () -> assertTrue(themeScenario.contains("flex-wrap:nowrap!important")),
                () -> assertFalse(themeDetail.contains("${")),
                () -> assertTrue(themeDetail.contains(".atf-failure-detail")),
                () -> assertTrue(failureDetection.contains("function failedSetupEvidence(")),
                () -> assertTrue(failureDetection.contains("function failedUiEvidence(")),
                () -> assertTrue(failureContext.contains("function collectFailureState(")),
                () -> assertTrue(failureContext.contains("function failureContext(")),
                () -> assertTrue(failureCauseChain.contains("function failureCauseChain(")),
                () -> assertTrue(failureProjection.contains("function renderFails(")),
                () -> assertTrue(failureProjection.contains("atf-failure fail-row")),
                () -> assertTrue(failureProjection.contains("status status-fail")),
                () -> assertTrue(failureProjection.contains("atf-empty-state")),
                () -> assertTrue(script.contains("document.querySelectorAll('.tab')")),
                () -> assertTrue(script.contains("history.replaceState(null,'','#'+id)")),
                () -> assertTrue(script.contains("activate('overview')")),
                () -> assertTrue(script.contains("activate('tests')")),
                () -> assertFalse(runtimeBoot.contains("${")),
                () -> assertTrue(runtimeBoot.contains("__atfRuntimeBoot='starting'")),
                () -> assertTrue(runtimeBoot.contains("function q(){};")),
                () -> assertTrue(ReportAssetComposer.extentEvidenceRolesScript()
                        .contains("function assignNodeRoles()")));
    }

    @Test
    void durationAssetsAreNativeAndKeepThePublicCompositionBoundary() {
        assertAll(
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_PARSING_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_DOM_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_MEMORY_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_AGGREGATION_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_PROJECTION_LOOKUP_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_PROJECTION_VIEWS_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_PROJECTION_PUBLICATION_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_SCHEDULING_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.DURATION_LIFECYCLE_JS.format()),
                () -> assertTrue(ReportAssetComposer.durationParsingScript().contains("function atfDurationValue(")),
                () -> assertTrue(ReportAssetComposer.durationAggregationScript()
                        .contains("function aggregateNodeDuration(")),
                () -> assertTrue(ReportAssetComposer.durationProjectionPublicationScript()
                        .contains("function aggregateClassDurations(")),
                () -> assertTrue(ReportAssetComposer.durationLifecycleScript()
                        .contains("function watchDurationNodes(")));
    }

    @Test
    void coreRuntimeAssetsKeepTheSharedDomVocabularyInStableOrder() {
        String dom = ReportAssetComposer.runtimeCoreDomScript();
        String scenarios = ReportAssetComposer.runtimeCoreScenariosScript();
        String metadata = ReportAssetComposer.runtimeCoreMetadataScript();

        assertAll(
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.RUNTIME_CORE_DOM_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.RUNTIME_CORE_SCENARIOS_JS.format()),
                () -> assertEquals(ReportAsset.Format.JAVASCRIPT, ReportAsset.RUNTIME_CORE_METADATA_JS.format()),
                () -> assertTrue(dom.contains("function q(")),
                () -> assertTrue(dom.contains("function esc(")),
                () -> assertTrue(scenarios.contains("function testItems(")),
                () -> assertTrue(metadata.contains("function normalizeTestPackageMetadata(")),
                () -> assertFalse(dom.contains("${")),
                () -> assertFalse(scenarios.contains("${")),
                () -> assertFalse(metadata.contains("${")));
    }

    @Test
    void liveExtentEvidenceRowsKeepTheStatusLaneFreeOfHorizontalSeparators() {
        String css = ReportAssetComposer.evidenceStackCss();

        assertAll(
                () -> assertTrue(css.contains(".test-view .test-contents"),
                        "the active Extent detail container must be covered"),
                () -> assertTrue(css.contains("details.step-detail"),
                        "API/setup/execution evidence rows must be covered"),
                () -> assertTrue(css.contains("details.atf-ui-action-sequence"),
                        "Playwright action rows must be covered"),
                () -> assertTrue(css.contains("details.atf-ui-observations"),
                        "audit observation rows must be covered"),
                () -> assertTrue(css.contains("details.setup-evidence"),
                        "setup evidence must retain its own boundary"),
                () -> assertTrue(css.contains("details.assertion-evidence"),
                        "assertion evidence must retain its own boundary"),
                () -> assertTrue(css.contains(">.collapse>.card-body"),
                        "active Extent card boundaries must use explicit DOM combinators"),
                () -> assertTrue(css.contains("table.helm-validation-table"),
                        "assertion rows must be covered"),
                () -> assertTrue(css.contains("border:1px solid var(--atf-border)!important"),
                        "setup and assertion disclosures must keep their visible border"),
                () -> assertTrue(css.contains("border-top:0!important"),
                        "the row boundary must not double-render"),
                () -> assertTrue(css.contains("border-bottom:0!important"),
                        "the horizontal separator must be removed"),
                () -> assertTrue(css.contains("details.request-evidence[data-atf-evidence-last=\"true\"]"),
                        "a standalone request disclosure must close its own frame"),
                () -> assertTrue(css.contains("details.step-detail[data-atf-evidence-last=\"true\"]"),
                        "the final semantic evidence disclosure must close its frame regardless of evidence type"),
                () -> assertTrue(css.contains(":not(:has(~ tr.event-row details.step-detail))")
                                || css.contains(":not(:has(~tr.event-row details.step-detail))"),
                        "a missing evidence marker must still resolve the final disclosure structurally"),
                () -> assertTrue(css.contains("border-radius:0 0 var(--atf-evidence-radius) var(--atf-evidence-radius)!important"),
                        "the final request disclosure must have a visible bottom edge"),
                () -> assertFalse(css.matches("(?s).*\\.test-view \\.test-contents.*border:0!important.*"),
                        "the fix must preserve vertical table separators"));
    }
}







