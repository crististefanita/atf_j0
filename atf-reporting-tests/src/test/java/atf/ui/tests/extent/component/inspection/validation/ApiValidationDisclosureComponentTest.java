package atf.ui.tests.extent.component.inspection.validation;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.disclosure.ApiValidationDisclosureFixture;
import atf.ui.tests.extent.support.locators.disclosure.ApiValidationDisclosureLocators;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Public API mock -> real publisher -> browser, including independent oracle degradation controls. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ReportBrowserTest
class ApiValidationDisclosureComponentTest {
    private static final ApiValidationDisclosureLocators DOM = new ApiValidationDisclosureLocators();
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void startBrowser() {
        playwright = BrowserResources.create(Playwright::create);
        browser = BrowserResources.launch(playwright, new BrowserType.LaunchOptions().setHeadless(true));
    }
    @AfterAll void closeBrowser() {
        BrowserResources.closeAll(browser, playwright);
    }

    static Stream<Arguments> matrix() {
        return Stream.of(false, true).flatMap(fallback -> Stream.of(false, true).flatMap(aggregate ->
                Stream.of(AtfReportStatus.PASS, AtfReportStatus.FAIL, AtfReportStatus.WARNING)
                        .map(status -> Arguments.of(fallback, aggregate, status))));
    }

    @ParameterizedTest
    @MethodSource("matrix")
    void apiValidationIsCollapsedAuditableAndKeyboardOperable(boolean fallback, boolean aggregate, AtfReportStatus status)
            throws Exception {
        var report = ApiValidationDisclosureFixture.publish(fallback, aggregate, status);
        assertFalse(Files.readString(report).contains(ApiValidationDisclosureFixture.CANARY));
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5_000);
                var errors = new ArrayList<String>();
                page.onPageError(errors::add);
                page.navigate(report.toUri() + "#test-view");
                var table = arrive(page, fallback);
                verifyClosed(table, fallback, status);
                var disclosure = DOM.disclosure(table, fallback);
                var summary = DOM.summary(disclosure, fallback);
                summary.click();
                assertThat(table).isVisible();
                assertThat(summary).hasAttribute("aria-expanded", "true");
                DOM.rows(table).last().scrollIntoViewIfNeeded();
                assertThat(DOM.rows(table).last()).isVisible();
                summary.press("Enter");
                assertThat(table).isHidden();
                summary.press("Space");
                assertThat(table).isVisible();
                if (status == AtfReportStatus.FAIL) {
                    summary.click();
                    DOM.followFailure(page, fallback);
                    table = DOM.table(DOM.validation(DOM.scenario(page, fallback, ApiValidationDisclosureFixture.SUBJECT)));
                    assertThat(table).isVisible();
                    assertEquals("JSON null · final comparison", DOM.cells(DOM.rows(table).last()).first().innerText().trim());
                    String deepLink = page.url();
                    page.reload();
                    assertEquals(deepLink, page.url());
                    assertThat(DOM.table(DOM.validation(DOM.scenario(page, fallback,
                            ApiValidationDisclosureFixture.SUBJECT)))).isVisible();
                }
                page.setViewportSize(392, 860);
                assertTrue((Boolean) table.evaluate("t => t.getBoundingClientRect().width <= innerWidth"), "narrow table width");
                assertTrue(errors.isEmpty(), errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true,missing-disclosure", "true,initially-open", "true,stolen-final-row",
            "false,missing-disclosure", "false,initially-open", "false,stolen-final-row"})
    void oracleRejectsMissingDisclosureInitialExpansionAndSiblingEvidence(boolean fallback, String mutation) throws Exception {
        var report = ApiValidationDisclosureFixture.publish(fallback, true, AtfReportStatus.FAIL);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5_000);
                page.navigate(report.toUri() + "#test-view");
                var table = arrive(page, fallback);
                verifyClosed(table, fallback, AtfReportStatus.FAIL);
                DOM.mutate(table, fallback, mutation);
                assertThrows(AssertionError.class, () -> verifyClosed(table, fallback, AtfReportStatus.FAIL), mutation);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true,FAIL", "true,WARNING", "false,FAIL", "false,WARNING"})
    void preservesEachTypedMediaVerdictAndRejectsItsReassignment(boolean fallback, AtfReportStatus status) throws Exception {
        var report = ApiValidationDisclosureFixture.publishMedia(fallback, status);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5_000);
                page.navigate(report.toUri() + "#test-view");
                var table = arrive(page, fallback);
                verifyMedia(table, fallback, status);
                DOM.swapFirstTwoResults(table);
                assertThrows(AssertionError.class, () -> verifyMedia(table, fallback, status),
                        "The same negative count on a different row must not pass");
                DOM.swapFirstTwoResults(table);
                verifyMedia(table, fallback, status);
                DOM.summary(DOM.disclosure(table, fallback), fallback).click();
                assertThat(table).isVisible();
                DOM.rows(table).last().scrollIntoViewIfNeeded();
                assertThat(DOM.rows(table).last()).isVisible();
                DOM.summary(DOM.disclosure(table, fallback), fallback).press("Enter");
                assertThat(table).isHidden();
                DOM.summary(DOM.disclosure(table, fallback), fallback).press("Space");
                assertThat(table).isVisible();
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void verifyMedia(Locator table, boolean fallback, AtfReportStatus status) {
        verifyShellClosed(table, fallback);
        var rows = DOM.rows(table);
        assertEquals(3, rows.count());
        assertEquals(List.of("HTTP status", "Content-Type JSON", "JSON null · final comparison"),
                rows.all().stream().map(r -> DOM.cells(r).first().textContent().trim()).toList());
        assertEquals("200", DOM.cells(rows.first()).nth(1).textContent().trim());
        assertEquals("200", DOM.cells(rows.first()).nth(2).textContent().trim());
        assertEquals("application/json", DOM.cells(rows.nth(1)).nth(1).textContent().trim());
        assertEquals("text/plain; profile=\"application/json\"", DOM.cells(rows.nth(1)).nth(2).textContent().trim());
        assertEquals("null", DOM.cells(rows.last()).nth(2).textContent().trim());
        for (int index = 0; index < 3; index++)
            assertEquals(marker(index == 1 ? status : AtfReportStatus.PASS, fallback),
                    DOM.cells(rows.nth(index)).last().textContent().trim().toUpperCase(), "Typed verdict at row " + index);
    }

    private static Locator arrive(Page page, boolean fallback) {
        DOM.selectTests(page, fallback);
        var scenario = DOM.scenario(page, fallback, ApiValidationDisclosureFixture.SUBJECT);
        DOM.openAncestors(scenario, fallback);
        return DOM.table(DOM.validation(scenario));
    }

    private static void verifyClosed(Locator table, boolean fallback, AtfReportStatus status) {
        verifyShellClosed(table, fallback);
        verifyRows(table, fallback, status);
    }

    private static void verifyShellClosed(Locator table, boolean fallback) {
        assertEquals(1, table.count(), "one table at exact subject");
        assertEquals(1, DOM.disclosure(table, fallback).count(), "one disclosure at exact subject");
        assertEquals(1, DOM.summary(DOM.disclosure(table, fallback), fallback).count(), "one operable control");
        assertFalse(DOM.isOpen(DOM.disclosure(table, fallback), fallback), "initially collapsed");
        assertThat(table).isHidden();
        assertThat(DOM.summary(DOM.disclosure(table, fallback), fallback)).isVisible();
        assertThat(DOM.summary(DOM.disclosure(table, fallback), fallback)).hasAttribute("aria-expanded", "false");
        assertEquals(List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"),
                DOM.headings(table).allTextContents().stream().map(String::trim).map(String::toUpperCase).toList());
    }

    private static void verifyRows(Locator table, boolean fallback, AtfReportStatus status) {
        var rows = DOM.rows(table);
        assertEquals(3, rows.count(), "exact subject inventory; a sibling must not compensate");
        assertEquals(List.of("JSON object", "JSON array", "JSON null · final comparison"),
                rows.all().stream().map(r -> DOM.cells(r).first().textContent().trim()).toList());
        for (var row : rows.all()) {
            var cells = DOM.cells(row);
            assertEquals(4, cells.count());
            assertEquals(marker(status, fallback), cells.last().textContent().trim().toUpperCase());
            assertTrue(cells.nth(1).textContent().length() <= 240);
            assertTrue(cells.nth(2).textContent().length() <= 240);
        }
        assertEquals("null", DOM.cells(rows.last()).nth(1).textContent().trim());
        assertEquals(status == AtfReportStatus.PASS ? "null" : "{}", DOM.cells(rows.last()).nth(2).textContent().trim());
        assertEquals(DOM.cells(rows.first()).nth(1).textContent(), DOM.cells(rows.first()).nth(2).textContent(),
                "a mismatch after 240 must retain its supplied verdict despite identical previews");
    }

    private static String marker(AtfReportStatus status, boolean fallback) {
        return fallback ? status.name() : switch (status) {
            case PASS -> "✅";
            case FAIL -> "❌";
            case WARNING -> "⚠️";
            default -> throw new IllegalArgumentException(status.name());
        };
    }
}
