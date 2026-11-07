package atf.ui.tests.extent.component.presentation;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.presentation.PresentationReportFixture;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Reads the actual visible System rows, never a script/template marker or another table. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ReportBrowserTest
class ReportSystemMetadataComponentTest {
    @TempDir Path directory;
    private Playwright playwright;
    private Browser browser;
    @BeforeAll void start() { playwright = BrowserResources.create(Playwright::create); browser = BrowserResources.launch(playwright); }
    @AfterAll void stop() {
        BrowserResources.closeAll(browser, playwright);
    }
    static Stream<Arguments> matrix() {
        return Stream.of(false, true).flatMap(fallback -> Stream.of(false, true).map(aggregate -> Arguments.of(fallback, aggregate)));
    }
    @ParameterizedTest @MethodSource("matrix")
    void overviewPublishesTheSameCommonFactsWithTheActualRenderer(boolean fallback, boolean aggregate) {
        Path file = PresentationReportFixture.publish(directory.resolve("metadata-" + fallback + "-" + aggregate + ".html"), fallback, aggregate);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                open(page, file, fallback);
                assertEquals(expected(fallback), read(table(page, fallback)));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oracleRejectsOneMissingFactWhileTheOtherTablesAndValuesRemain(boolean fallback) {
        Path file = PresentationReportFixture.publish(directory.resolve("mutant-" + fallback + ".html"), fallback, true);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                var page = pageLease.value();
                open(page, file, fallback);
                var table = table(page, fallback);
                assertEquals(expected(fallback), read(table));
                table.locator("tr").filter(new Locator.FilterOptions().setHasText("Runtime")).evaluate("row => row.remove()");
                assertThrows(AssertionError.class, () -> assertEquals(expected(fallback), read(table)));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static void open(Page page, Path file, boolean fallback) {
        page.setDefaultTimeout(5_000); page.setDefaultNavigationTimeout(30_000);
        page.navigate(file.toUri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        if (fallback) page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Overview").setExact(true)).click();
        else page.locator("#nav-overview").click();
    }
    private static Locator table(Page page, boolean fallback) {
        var table = page.locator(fallback ? "#overview table" : ".dashboard-view .sysenv-container table");
        assertEquals(1, table.count()); assertThat(table).isVisible(); return table;
    }
    private static Map<String, String> read(Locator table) {
        var actual = new LinkedHashMap<String, String>();
        for (var row : table.locator("tr").all()) {
            var cells = row.locator("td"); if (cells.count() != 2) continue;
            String key = cells.first().innerText().trim();
            assertNull(actual.put(key, cells.last().innerText().trim()), "duplicate fact: " + key);
        }
        return actual;
    }
    private static Map<String, String> expected(boolean fallback) {
        return Map.of("Environment", "LOCAL", "Runtime", "Java " + System.getProperty("java.version", "unknown"),
                "OS", System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "unknown"),
                "Client", "Configured per execution", "Reporting", (fallback ? "Fallback" : "Extent") + " · embedded offline HTML",
                "Assertion preview", "240 characters");
    }
}
