package atf.ui.tests.extent.component.inspection.json;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.json.JsonEvidenceFixture;
import atf.ui.tests.extent.support.locators.disclosure.ApiValidationDisclosureLocators;
import atf.ui.tests.extent.support.locators.json.JsonEvidenceLocators;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Real publishers and browser DOM, authored mock snapshots, with negative controls at the active owner. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ReportBrowserTest
class JsonEvidenceFidelityComponentTest {
    private static final ApiValidationDisclosureLocators DOM = new ApiValidationDisclosureLocators();
    private static final JsonEvidenceLocators JSON = new JsonEvidenceLocators();
    private Playwright playwright;
    private Browser browser;

    @BeforeAll void startBrowser() {
        playwright = BrowserResources.create(Playwright::create);
        browser = BrowserResources.launch(playwright, new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll void closeBrowser() {
        BrowserResources.closeAll(browser, playwright);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void keepsJsonAndTypedVerdictsAcrossIndividualAndAggregateReports(boolean fallback, boolean aggregate) throws Exception {
        var report = JsonEvidenceFixture.publish(fallback, aggregate);
        assertFalse(Files.readString(report).contains("synthetic-json-secret-"));
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5_000);
                var errors = new ArrayList<String>();
                page.onPageError(errors::add);
                page.navigate(report.toUri() + "#test-view");
                var scenario = arrive(page, fallback);
                verifyJson(JSON.body(scenario, fallback), fallback);
                var table = DOM.table(DOM.validation(scenario));
                assertEquals(1, table.count());
                var disclosure = DOM.disclosure(table, fallback);
                var summary = DOM.summary(disclosure, fallback);
                assertFalse(DOM.isOpen(disclosure, fallback));
                assertThat(table).isHidden();
                verifyComparisons(table, fallback);
                summary.click();
                assertThat(table).isVisible();
                DOM.rows(table).last().scrollIntoViewIfNeeded();
                assertThat(DOM.rows(table).last()).isVisible();
                summary.press("Enter");
                assertThat(table).isHidden();
                summary.press("Space");
                assertThat(table).isVisible();
                assertTrue(errors.isEmpty(), errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false,rounded-number", "false,lost-duplicate", "true,rounded-number", "true,lost-duplicate"})
    void rejectsRoundedNumbersAndLostDuplicatesEvenWhenStatusIsUnchanged(boolean fallback, String mutation) throws Exception {
        var report = JsonEvidenceFixture.publish(fallback, true);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5_000);
                page.navigate(report.toUri() + "#test-view");
                var body = JSON.body(arrive(page, fallback), fallback);
                verifyJson(body, fallback);
                JSON.mutate(body, mutation);
                assertThrows(AssertionError.class, () -> verifyJson(body, fallback), mutation);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static Locator arrive(Page page, boolean fallback) {
        DOM.selectTests(page, fallback);
        var scenario = DOM.scenario(page, fallback, JsonEvidenceFixture.SUBJECT);
        DOM.openAncestors(scenario, fallback);
        return scenario;
    }

    private static void verifyJson(Locator body, boolean fallback) throws Exception {
        assertEquals(2, body.count(), "two complete JSON bodies at the active subject, each within preview 240");
        var numbers = new ArrayList<String>();
        var fields = new ArrayList<String>();
        var strings = new ArrayList<String>();
        for (var code : body.all()) {
            String text = code.textContent();
            assertTrue(text.length() <= 240, "no truncation budget change");
            try (var parser = new JsonFactory().createParser(text)) {
                for (JsonToken token; (token = parser.nextToken()) != null;) {
                    if (token.isNumeric()) numbers.add(parser.getText());
                    if (token == JsonToken.FIELD_NAME) fields.add(parser.currentName());
                    if (token == JsonToken.VALUE_STRING) strings.add(parser.getText());
                }
            }
        }
        assertEquals(JsonEvidenceFixture.NUMBERS, numbers, "literal tokens, never binary floating-point expected");
        assertEquals(List.of("values", "seen", "password", "seen", "password"), fields);
        assertEquals(List.of("***REDACTED***", "***REDACTED***"), strings);
    }

    private static void verifyComparisons(Locator table, boolean fallback) {
        var rows = DOM.rows(table);
        assertEquals(3, rows.count(), "a neighbor cannot compensate for missing subject evidence");
        var first = DOM.cells(rows.first());
        assertEquals(JsonEvidenceFixture.EXPECTED, first.nth(1).textContent().trim());
        assertEquals(JsonEvidenceFixture.ACTUAL, first.nth(2).textContent().trim());
        var longRow = DOM.cells(rows.nth(1));
        assertEquals(longRow.nth(1).textContent(), longRow.nth(2).textContent(), "same preview, distinct source values");
        for (int index = 0; index < 3; index++) {
            var cells = DOM.cells(rows.nth(index));
            assertEquals(4, cells.count());
            assertTrue(cells.nth(1).textContent().length() <= 240);
            assertTrue(cells.nth(2).textContent().length() <= 240);
            assertEquals(fallback ? index == 2 ? "PASS" : "FAIL" : index == 2 ? "✅" : "❌",
                    cells.last().textContent().trim().toUpperCase());
        }
        assertEquals("JSON null · final comparison", DOM.cells(rows.last()).first().textContent().trim());
    }
}
