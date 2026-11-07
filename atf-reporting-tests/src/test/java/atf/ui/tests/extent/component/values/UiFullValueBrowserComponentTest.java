package atf.ui.tests.extent.component.values;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.support.values.UiFullValueFixture;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Both renderers expose the exact full scalar through an independently operable, initially closed cell. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ReportBrowserTest
class UiFullValueBrowserComponentTest {
    private Playwright playwright;
    private Browser browser;
    @BeforeAll void start() { playwright = BrowserResources.create(Playwright::create); browser = BrowserResources.launch(playwright); }
    @AfterAll void stop() {
        BrowserResources.closeAll(browser, playwright);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void exactCellsSurviveKeyboardNestedTogglingAndNarrowPanel(boolean extent) {
        var file = UiFullValueFixture.publish(extent);
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser, new Browser.NewContextOptions().setViewportSize(1280, 900)))) {
            try {
                var page = pageLease.value();
                var errors = new ArrayList<String>(); page.onPageError(errors::add); page.setDefaultTimeout(5_000);
                page.navigate(file.toUri() + "#test-view");
                if (extent) page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                else page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Tests").setExact(true)).click();
                var root = page.locator(extent ? ".test-content-detail" : "#tests article.scenario").first();
                var action = root.locator("details.atf-ui-action-sequence").first();
                if (extent) {
                    var card = root.locator(".test-contents > .mt-4 > .accordion > .card").first();
                    for (int depth = 0; depth < 5 && !action.isVisible(); depth++) {
                        assertTrue(card.count() > 0, "Captured checkpoint ancestor " + depth);
                        var header = card.locator(":scope > .card-header[role='button']");
                        assertEquals("false", header.getAttribute("aria-expanded"));
                        header.press("Enter");
                        assertEquals("true", header.getAttribute("aria-expanded"));
                        card = card.locator(":scope > .accordion > .card").first();
                    }
                }
                assertTrue(action.isVisible());
                for (var source : List.of(action, root.locator("details.atf-ui-observations").first())) {
                    assertNull(source.getAttribute("open")); source.locator(":scope > summary").click();
                    var values = source.locator("details.atf-ui-value"); assertEquals(2, values.count());
                    for (int column = 0; column < 2; column++) {
                        var value = values.nth(column); var summary = value.locator(":scope > summary");
                        assertNull(value.getAttribute("open")); assertEquals(240, summary.locator(".se-title").textContent().length());
                        summary.press("Enter"); assertNotNull(value.getAttribute("open"));
                        assertEquals(UiFullValueFixture.value(column == 0, 1), value.locator("pre").textContent());
                        assertTrue(value.locator("pre").isVisible()); assertNull(values.nth(1 - column).getAttribute("open"));
                        assertEquals(true, summary.evaluate("e => e.querySelector('.se-title').getBoundingClientRect().bottom <= e.getBoundingClientRect().bottom + 1"));
                        assertEquals(true, value.evaluate("e => e.querySelector('pre').getBoundingClientRect().top >= e.querySelector('summary').getBoundingClientRect().bottom - 1"));
                        value.locator("pre").press("End");
                        page.waitForFunction("e => e.scrollTop + e.clientHeight >= e.scrollHeight - 1", value.locator("pre").elementHandle());
                        assertEquals(true, value.locator("pre").evaluate("e => e.scrollTop + e.clientHeight >= e.scrollHeight - 1"));
                        page.setViewportSize(980, 800);
                        assertTrue(value.locator("pre").isVisible());
                        summary.press("Space"); assertNull(value.getAttribute("open"));
                    }
                    source.locator(":scope > summary").press("Space"); assertNull(source.getAttribute("open"));
                }
                assertTrue(errors.isEmpty(), errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}
