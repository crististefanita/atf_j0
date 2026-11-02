package atf.ui.tests.extent.component.values;

import atf.reporting.support.values.UiRowEvidenceFixture;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Native browser parsing must retain initial LF/CRLF and trailing whitespace at the original captured row. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UiRowProjectionBrowserComponentTest {
    private Playwright playwright;
    private Browser browser;
    @BeforeAll void start() { playwright = Playwright.create(); browser = playwright.chromium().launch(); }
    @AfterAll void stop() { if (browser != null) browser.close(); if (playwright != null) playwright.close(); }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void fullCellMatchesTheOriginalCaptureAcrossKeyboardCollapseAndReopen(boolean extent) {
        var file = UiRowEvidenceFixture.publish(extent);
        try (var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1100, 850))) {
            page.setDefaultTimeout(5_000); page.navigate(file.toUri() + "#test-view");
            if (extent) {
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes").setExact(true)).click();
            } else page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Tests").setExact(true)).click();
            var owner = page.locator(extent ? ".test-content-detail" : "#tests article.scenario").first();
            for (var source : AtfUiEvidenceSource.values()) {
                var group = owner.locator(source == AtfUiEvidenceSource.ACTION ? "details.atf-ui-action-sequence" : "details.atf-ui-observations").first();
                group.locator(":scope > summary").press("Enter");
                var cell = group.locator("details.atf-ui-value").first(); var summary = cell.locator(":scope > summary");
                assertNull(cell.getAttribute("open")); summary.press("Space");
                assertEquals(UiRowEvidenceFixture.value(source), cell.locator("pre").textContent());
                cell.locator("pre").press("End");
                page.waitForFunction("e => e.scrollTop + e.clientHeight >= e.scrollHeight - 1", cell.locator("pre").elementHandle());
                summary.press("Enter"); assertNull(cell.getAttribute("open")); summary.press("Enter");
                assertEquals(UiRowEvidenceFixture.value(source), cell.locator("pre").textContent());
                group.locator(":scope > summary").press("Space");
            }
            assertFalse(page.content().contains(UiRowEvidenceFixture.CANARY));
        }
    }
}
