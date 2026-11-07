package atf.ui.tests.extent.e2e.configurable;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.theme.tokens.AtfReportThemeTokens;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;
import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.ui.tests.extent.support.audit.screen.CategoryFilterAuditor;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.ui.tests.extent.support.configurable.CategoryCardinalityFixture;
import atf.ui.tests.extent.support.configurable.CategoryCardinalityReportFixtures;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.inspection.layout.ScrollableApiAggregateReportFixture;
import atf.ui.tests.extent.support.inspection.layout.ScrollableTableReportFixture;
import atf.ui.tests.extent.support.inspection.layout.ScrollableTableViewportBrowserProbe;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browser proof for Categories with zero, one and many generated filters,
 * plus UI/API scroll viewport contracts.
 *
 * <p>The fixture publisher is isolated under {@code configurable.fixtures};
 * this class only composes the dynamic test and assertions. Each case publishes a real standalone Extent artifact for each
 * cardinality and audits it through the same locator boundary used by the
 * production report explorations. This keeps the cardinality contract
 * independent from the API publisher tag vocabulary. Scroll measurements use
 * separate authentic UI and API reports so each selector is measured in the
 * renderer surface that owns it.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
@ReportBrowserTest
class CategoryCardinalityBrowserFixtureTest {
    private static final UiConfiguration CONFIGURATION =
            ReportingTestResources.uiConfiguration(CategoryCardinalityBrowserFixtureTest.class);

    @TestFactory
    Stream<DynamicTest> generatedCategoryCardinalities() {
        Stream<DynamicTest> categories = Stream.of(
                CategoryCardinalityFixture.ZERO,
                CategoryCardinalityFixture.ONE,
                CategoryCardinalityFixture.MANY)
                .map(fixture -> DynamicTest.dynamicTest(
                        fixture.gherkinProposition(),
                        () -> verifyFixture(fixture)));
        DynamicTest emptyStatusFilter = DynamicTest.dynamicTest(
                "GIVEN one Pass category WHEN Fail has no matches THEN stale detail clears and Clear restores it",
                this::verifyEmptyStatusFilter);
        Stream<DynamicTest> scrollViewports = Stream.of(
                        ScrollableTableReportFixture.Density.SHORT,
                        ScrollableTableReportFixture.Density.LONG)
                .map(density -> DynamicTest.dynamicTest(
                        scrollViewportProposition(density), () -> verifyScrollViewport(density)));
        return Stream.concat(Stream.concat(categories, Stream.of(emptyStatusFilter)), scrollViewports);
    }

    private void verifyEmptyStatusFilter() throws Exception {
        Path root = Files.createTempDirectory("atf-category-empty-filter-");
        Path report = CategoryCardinalityReportFixtures.publish(root, CategoryCardinalityFixture.ONE);
        try (ReportAuditContext context = ReportAuditContext.open(CONFIGURATION, report,
                "category-empty-status-filter")) {
            try {
                UiAuditFlow audit = new UiAuditFlow(context, CONFIGURATION,
                        getClass().getSimpleName(), getClass().getPackageName(),
                        List.of("ui", "categories", "empty-filter"),
                        "GIVEN a category report with Pass results is available",
                        "Empty category status filter retained stale detail", ReportUiContract.Tab.CATEGORIES.hash());
                Page page = audit.openTab(ReportUiContract.Tab.CATEGORIES.hash());
                ReportUiLocators locators = new ReportUiLocators();
                Locator detail = locators.visibleCategoryDetail(page);
                assertEquals(1, locators.categoryTitle(detail).count(), "The initial category detail must be selected");

                audit.actions().click(locators.categoryStatusFilterTriggerAction(page),
                        "open Categories status filters");
                var fail = locators.categoryStatusOptionAction(page, "fail");
                audit.actions().waitForVisible(fail);
                audit.actions().click(fail, "filter categories to Fail");
                assertEquals(0, locators.categoryVisibleItems(locators.categoryView(page)).count(),
                        "The Pass-only fixture must have no visible category under the Fail filter");
                assertEquals(0, locators.categoryTitle(detail).count(),
                        "The detail from the previously selected category must be cleared");
                assertEquals(0, page.locator("#category-view .test-item.active").count(),
                        "No hidden category may remain selected when the filter has no matches");

                var clear = locators.categoryStatusOptionAction(page, "clear");
                if (!clear.locator().isVisible()) {
                    audit.actions().click(locators.categoryStatusFilterTriggerAction(page),
                            "reopen Categories status filters");
                    audit.actions().waitForVisible(clear);
                }
                audit.actions().click(clear, "clear the status filter");
                assertEquals(1, locators.categoryVisibleItems(locators.categoryView(page)).count(),
                        "Clear must restore the category list");
                assertEquals(1, locators.categoryTitle(detail).count(),
                        "Clear must restore readable detail for the category");
                assertEquals(1, page.locator("#category-view .test-item.active").count(),
                        "Clear must select the restored category detail owner");
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        } finally {
            if (!Boolean.getBoolean("atf.keep.category.fixtures")) {
                CategoryCardinalityReportFixtures.deleteTree(root);
            }
        }
    }

    private static String scrollViewportProposition(ScrollableTableReportFixture.Density density) {
        String content = density.expectsLongContent() ? "long" : "short";
        return "GIVEN fresh UI and API Extent reports have %s table content WHEN standard and narrow viewport geometry is measured "
                .formatted(content)
                + "THEN the shared scroll cap, natural short height, sticky header and reachable last row are valid";
    }

    private void verifyScrollViewport(ScrollableTableReportFixture.Density density) throws Exception {
        Path root = Files.createTempDirectory("atf-scroll-viewport-");
        Path uiReport = ScrollableTableReportFixture.publish(root, density);
        Path apiReport = ScrollableApiAggregateReportFixture.publish(root, density);
        String suffix = density.name().toLowerCase();
        UiConfiguration apiAuditConfiguration = CONFIGURATION.withReportDirectory(root.resolve("api-audit"));
        try (ReportAuditContext uiContext = ReportAuditContext.open(CONFIGURATION, uiReport,
                     "scroll-viewport-ui-" + suffix);
             ReportAuditContext apiContext = ReportAuditContext.open(apiAuditConfiguration, apiReport,
                     "scroll-viewport-api-" + suffix)) {
            try {
                UiAuditFlow audit = new UiAuditFlow(uiContext, CONFIGURATION,
                        getClass().getSimpleName(), getClass().getPackageName(),
                        List.of("ui", "reporting", "scroll-viewport"),
                        "GIVEN the fresh Extent viewport fixture is available",
                        "Scrollable table viewport contract failed", "Overview / Tests");
                UiAuditFlow apiAudit = new UiAuditFlow(apiContext, apiAuditConfiguration,
                        getClass().getSimpleName(), getClass().getPackageName(),
                        List.of("api", "reporting", "scroll-viewport"),
                        "GIVEN the fresh API aggregate fixture is available",
                        "API aggregate viewport contract failed", ReportUiContract.Tab.TESTS.hash());
                ScrollableTableViewportBrowserProbe probe = new ScrollableTableViewportBrowserProbe();
                ScrollableTableViewportBrowserProbe.Result result = audit.auditResult(
                        "Shared table viewport measurements — " + suffix, session -> {
                            var measured = probe.inspect(audit, apiAudit, CONFIGURATION.browser(), density);
                            var apiActions = apiAudit.actions().drain();
                            var uiBrowserDiagnostics = uiContext.browserDiagnostics();
                            var apiBrowserDiagnostics = apiContext.browserDiagnostics();
                            audit.actions().observation("API aggregate artifact inspected",
                                    apiReport.getFileName().toString(),
                                    "API Tests request aggregate summaries and tables are measurable",
                                    "actions=" + apiActions.size() + "; browser diagnostics="
                                            + apiBrowserDiagnostics.violations());
                            List<AtfAssertion> assertions = new ArrayList<>(measurementAssertions(measured));
                            assertions.add(new AtfAssertion("UI report browser runtime",
                                    "no console, page, or request failures",
                                    uiBrowserDiagnostics.violations().toString(), uiBrowserDiagnostics.valid()));
                            assertions.add(new AtfAssertion("API report browser runtime",
                                    "no console, page, or request failures",
                                    apiBrowserDiagnostics.violations().toString(), apiBrowserDiagnostics.valid()));
                            var page = audit.openTab(ReportUiContract.Tab.OVERVIEW.hash());
                            boolean browserValid = measured.valid() && uiBrowserDiagnostics.valid()
                                    && apiBrowserDiagnostics.valid();
                            session.screen("Scrollable table viewport measurements",
                                    "Overview + Tests / standard " + CONFIGURATION.browser().viewportWidth()
                                            + "px and narrow 591px",
                                    audit.capture(page, "scroll viewport geometry " + suffix),
                                    assertions, browserValid ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                            assertTrue(measured.valid(), measured::summary);
                            assertTrue(uiBrowserDiagnostics.valid(),
                                    () -> "UI fixture browser errors: " + uiBrowserDiagnostics.violations());
                            assertTrue(apiBrowserDiagnostics.valid(),
                                    () -> "API fixture browser errors: " + apiBrowserDiagnostics.violations());
                            return measured;
                        });
                assertTrue(result.valid(), result::summary);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        } finally {
            if (!Boolean.getBoolean("atf.keep.scroll.fixtures")) {
                CategoryCardinalityReportFixtures.deleteTree(root);
            }
        }
    }

    private static List<AtfAssertion> measurementAssertions(
            ScrollableTableViewportBrowserProbe.Result result) {
        List<AtfAssertion> assertions = new ArrayList<>();
        result.measurements().forEach(measurement -> assertions.add(new AtfAssertion(
                measurement.viewport() + " / " + measurement.surface(),
                "no horizontal clipping; max-height " + AtfReportThemeTokens.TABLE_SCROLL_HEIGHT
                        + "; short content natural; long content reachable",
                measurement.summary(), measurement.valid())));
        result.aggregateSummaries().forEach(summary -> assertions.add(new AtfAssertion(
                summary.viewport() + " aggregate " + summary.kind() + " summary",
                "34px; collapsed by default", "height=" + summary.heightPx()
                        + "px; open=" + summary.open() + "; defaultState=" + summary.defaultState(),
                summary.valid())));
        return List.copyOf(assertions);
    }

    private void verifyFixture(CategoryCardinalityFixture fixture) throws Exception {
        Path root = java.nio.file.Files.createTempDirectory("atf-category-cardinality-");
        Path report = CategoryCardinalityReportFixtures.publish(root, fixture);
        try (ReportAuditContext context = ReportAuditContext.open(CONFIGURATION, report,
                "category-cardinality-" + fixture.name().toLowerCase())) {
            try {
                UiAuditFlow audit = new UiAuditFlow(context, CONFIGURATION,
                        getClass().getSimpleName(), getClass().getPackageName(),
                        List.of("ui", "category-fixture"),
                        UiAuditGherkin.categoryFixtureReady(fixture.name()),
                        "Category cardinality fixture failed", ReportUiContract.Tab.CATEGORIES.hash());
                Page page = audit.openTab(ReportUiContract.Tab.TESTS.hash());
                ReportUiLocators locators = new ReportUiLocators();
                if (fixture == CategoryCardinalityFixture.ZERO) {
                    assertEquals(0, locators.navTab(page, ReportUiContract.Tab.CATEGORIES).count(),
                            "The Categories navigation must not be rendered when no category exists");
                    assertEquals(1, locators.categoryView(page).count(),
                            "The Categories view shell must remain available for the empty state");
                    assertTrue(!locators.categoryView(page).isVisible(),
                            "The Categories view must remain hidden when no category exists");
                    return;
                }

                page = audit.openTab(ReportUiContract.Tab.CATEGORIES.hash());
                CategoryFilterAuditor auditor = new CategoryFilterAuditor(audit);
                List<CategoryFilterAuditor.CategoryOption> options = auditor.discover(page);
                assertEquals(fixture.expectedCategoryCount(), options.size(),
                        "Generated category count must match the fixture");

                for (CategoryFilterAuditor.CategoryOption option : options) {
                    CategoryFilterAuditor.FilterSnapshot selected = auditor.select(page, option);
                    assertTrue(selected.valid(), () -> "Category filter did not reconcile: " + selected);
                    assertTrue(auditor.navigateToFirstLinkedScenario(page, option),
                            () -> "Category link did not navigate for " + option.name());
                    page = audit.openTab(ReportUiContract.Tab.CATEGORIES.hash());
                }
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        } finally {
            if (!Boolean.getBoolean("atf.keep.category.fixtures")) {
                CategoryCardinalityReportFixtures.deleteTree(root);
            }
        }
    }
}
