package atf.ui.tests.extent.component.provenance;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.provenance.UiActionAttributionFixture;
import atf.ui.tests.extent.support.inspection.provenance.UiActionAttributionFixture.Mode;
import atf.ui.tests.extent.support.inspection.provenance.UiFailureCatalogueFixture;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Real published pages: neutral attribution stays attached to the second failed action in Tests and Fails. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UiActionAttributionComponentTest {
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void start() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll void stop() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @ParameterizedTest
    @CsvSource({"false,web", "false,android", "true,web", "true,android"})
    void failureKeepsNeutralTypeCauseTargetAndOriginalRow(boolean fallback, String context) throws Exception {
        Path report = UiActionAttributionFixture.publish(fallback, context, Mode.DISPATCH_FAIL);
        try (var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 1000))) {
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.setDefaultTimeout(10_000);
            page.setDefaultNavigationTimeout(45_000);
            page.route("http://**/*", Route::abort);
            page.route("https://**/*", Route::abort);
            page.navigate(report.toUri() + "#test-view");
            if (fallback) page.locator("button[data-tab='tests']").click();
            else {
                page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");
                page.locator("#test-view .test-list-wrapper .test-list-item>li.test-item[test-id]").first().click();
            }
            var scope = page.locator(fallback ? "#tests article.scenario" : "#test-view .test-content-detail:visible").first();
            var action = scope.locator("details.atf-ui-action-sequence").first();
            assertFalse(Boolean.TRUE.equals(action.evaluate("e=>e.open")), "Actions start collapsed");
            var table = action.locator("table.atf-ui-action-table");
            openPath(table);
            assertThat(action.locator(":scope>summary")).containsText("UI action sequence");
            verifyRows(table);
            if (!fallback) {
                var diagnostic = scope.locator("details.atf-ui-failure-evidence").first();
                openPath(diagnostic.locator(":scope>summary"));
                assertThat(diagnostic.locator(":scope>summary")).containsText("UI action failed");
                if (!Boolean.TRUE.equals(diagnostic.evaluate("e=>e.open"))) diagnostic.locator(":scope>summary").press("Enter");
                assertThat(diagnostic).containsText(UiActionAttributionFixture.TARGET);
                assertThat(diagnostic).containsText(UiActionAttributionFixture.CAUSE);
            }
            Path captures = Path.of(System.getProperty("atf.test.reportDirectory"), "browser-provenance");
            Files.createDirectories(captures);
            page.screenshot(new Page.ScreenshotOptions().setPath(captures.resolve(context + "-" + fallback + "-tests.png")));
            if (fallback) {
                page.locator("button[data-tab='fails']").click();
                assertThat(page.locator("#fails")).containsText(UiActionAttributionFixture.CAUSE);
                page.locator("#fails a.fail-row").first().click();
            } else {
                page.locator("#nav-fails").click();
                page.locator("#atf-failure-list > .test-item").first().click();
                var cause = page.locator(".atf-failure-index-cause .atf-failure-value");
                assertThat(cause).containsText(UiActionAttributionFixture.CAUSE);
                var detail = page.locator("#atf-failure-detail");
                assertThat(detail).containsText("UI action");
                assertThat(detail).containsText("#2");
                assertThat(detail).containsText(UiActionAttributionFixture.TARGET);
                page.screenshot(new Page.ScreenshotOptions().setPath(captures.resolve(context + "-fails.png")));
                detail.locator(".atf-failure-open").click();
            }
            scope = page.locator(fallback ? "#tests article.scenario" : "#test-view .test-content-detail:visible").first();
            table = scope.locator("table.atf-ui-action-table").first();
            assertThat(table).isVisible();
            verifyRows(table);
            assertTrue(errors.isEmpty(), errors.toString());
        }
    }

    private static void verifyRows(Locator table) {
        var rows = table.locator("tbody>tr");
        assertEquals(2, rows.count());
        assertThat(rows.nth(1).locator("td").first()).containsText("#2");
        assertEquals(0, rows.first().locator(".atf-ui-action-result-fail").count());
        assertEquals(1, rows.nth(1).locator(".atf-ui-action-result-fail").count());
        assertThat(rows.nth(1).locator("td").nth(1)).hasText(UiActionAttributionFixture.TARGET);
        assertThat(rows.nth(1).locator("td").nth(3)).containsText(UiActionAttributionFixture.CAUSE);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void catalogueOpensEachOriginalKindInItsOwnScenario(boolean fallback, boolean requestOwner) throws Exception {
        Path report = UiFailureCatalogueFixture.publish(fallback, requestOwner);
        try (var page = browser.newPage()) {
            page.setDefaultTimeout(10_000);
            page.setDefaultNavigationTimeout(45_000);
            page.navigate(report.toUri() + (fallback ? "#fails" : "#test-view"));
            if (!fallback) page.waitForFunction("()=>document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");
            String[] selectors = {".atf-ui-action-table:not(.atf-ui-observation-table) tbody>tr:nth-child(2)",
                    ".atf-ui-observation-table tbody>tr", "table.assertions tbody>tr:first-child"};
            for (int index = 0; index < selectors.length; index++) {
                if (fallback) {
                    page.locator("button[data-tab='fails']").click();
                    assertEquals(3, page.locator("#fails a.fail-row").count());
                    page.locator("#fails a.fail-row").nth(index).click();
                } else {
                    page.locator("#nav-fails").click();
                    assertEquals(3, page.locator("#atf-failure-list>.test-item").count());
                    page.locator("#atf-failure-list>.test-item").nth(index).click();
                    assertThat(page.locator("#atf-failure-detail")).containsText(UiFailureCatalogueFixture.CAUSES.get(index));
                    var labels = page.locator("#atf-failure-detail .atf-failure-label").allTextContents();
                    assertTrue(labels.contains(index < 2 ? "DIAGNOSTIC" : "ASSERTION"));
                    assertFalse(labels.contains(index < 2 ? "ASSERTION" : "DIAGNOSTIC"));
                    page.locator("#atf-failure-detail .atf-failure-open").click();
                }
                String selector = !fallback && index == 2 ? ".helm-validation-table tbody>tr:first-child" : selectors[index];
                var source = page.locator(fallback ? "#tests article.scenario" : "#test-view .test-content-detail:visible").first().locator(selector).first();
                assertThat(source).isVisible();
                assertThat(source).containsText(UiFailureCatalogueFixture.CAUSES.get(index));
                assertTrue(page.url().contains("failure-ordinal=" + (fallback || index < 2 ? index + 1 : 1)));
                if (!fallback) assertTrue(page.url().endsWith("failure-kind=" + (index < 2 ? "ui" : "assertion")));
            }
        }
    }

    private static void openPath(Locator target) {
        var ancestors = target.locator("xpath=ancestor::*[self::details or contains(concat(' ',normalize-space(@class),' '),' card ')]");
        for (int pass = 0; pass < ancestors.count() && !target.isVisible(); pass++) {
            boolean progressed = false;
            for (var ancestor : ancestors.all()) {
                if (target.isVisible()) break;
                if ("details".equals(ancestor.evaluate("e=>e.localName"))) {
                    var summary = ancestor.locator(":scope>summary");
                    if (summary.isVisible() && !Boolean.TRUE.equals(ancestor.evaluate("e=>e.open"))) {
                        summary.press("Enter");
                        progressed = true;
                    }
                } else {
                    var header = ancestor.locator(":scope>.card-header");
                    if (header.count() > 0 && header.isVisible() && "false".equals(header.getAttribute("aria-expanded"))) {
                        header.press("Enter");
                        assertThat(header).hasAttribute("aria-expanded", "true");
                        progressed = true;
                    }
                }
            }
            if (!progressed) break;
        }
        assertThat(target).isVisible();
        target.scrollIntoViewIfNeeded();
    }
}
