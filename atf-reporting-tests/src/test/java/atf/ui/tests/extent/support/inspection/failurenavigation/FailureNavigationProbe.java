package atf.ui.tests.extent.support.inspection.failurenavigation;

import atf.reporting.support.failurecorrelation.FailureNavigationFixture;
import atf.reporting.support.failurecorrelation.FailureNavigationFixture.Owner;
import atf.ui.tests.extent.support.locators.failurenavigation.FailureNavigationLocators;
import com.microsoft.playwright.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/** Independent visibility and geometry checks run before any manual expansion. */
public final class FailureNavigationProbe {
    private static final FailureNavigationLocators LOCATORS = new FailureNavigationLocators();
    private FailureNavigationProbe() { }

    public static void verify(Owner owner, int width) {
        var output = FailureNavigationFixture.publish(owner);
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900))) {
            var errors = new ArrayList<String>(); page.onPageError(errors::add);
            page.navigate(output.toUri() + "#test-view");
            for (int phaseIndex = 0; phaseIndex < FailureNavigationFixture.PHASES.size(); phaseIndex++) {
                String phase = FailureNavigationFixture.PHASES.get(phaseIndex);
                int ordinal = phaseIndex * 2 + 2;
                open(page, ordinal, width == 392);
                page.waitForCondition(() -> LOCATORS.target(page, phase).isVisible());
                assertVisible(page, phase);
                assertTrue(page.url().contains("failure-ordinal=" + ordinal), "Reloadable address retains exact assertion identity");
                assertNotEquals("true", LOCATORS.sibling(page).getAttribute("data-atf-scenario-open"),
                        "The adjacent passing scenario is not expanded by failure navigation");
                LOCATORS.phase(page, phase).evaluate("p=>{p.querySelector(':scope>.accordion').style.setProperty('display','none','important')}");
                assertThrows(AssertionError.class, () -> assertVisible(page, phase),
                        "A visible scenario with a hidden phase must fail the independent oracle");
                open(page, ordinal, false);
                page.waitForCondition(() -> LOCATORS.target(page, phase).isVisible());
                assertVisible(page, phase);
                page.reload();
                page.waitForCondition(() -> LOCATORS.target(page, phase).isVisible());
                assertVisible(page, phase);
            }
            assertTrue(errors.isEmpty(), errors::toString);
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Path.of(output + "-" + width + ".png")));
        }
    }

    private static void open(Page page, int ordinal, boolean keyboard) {
        LOCATORS.fails(page).click();
        assertEquals(6, LOCATORS.failures(page).count(), "Each phase contributes two original failed comparisons");
        LOCATORS.failures(page).nth(ordinal - 1).click();
        assertEquals(Integer.toString(ordinal), LOCATORS.backlink(page).getAttribute("data-atf-failure-ordinal"));
        if (keyboard) LOCATORS.backlink(page).press("Enter");
        else LOCATORS.backlink(page).click();
    }

    private static void assertVisible(Page page, String phase) {
        var target = LOCATORS.target(page, phase);
        assertTrue(target.isVisible(), "The second checkpoint's original failed comparison is visible without Expand all");
        assertEquals(4, target.locator("td").count());
        assertTrue(target.locator("td").nth(3).innerText().contains("❌"), "The original FAIL remains a FAIL");
        assertTrue((Boolean) target.evaluate("""
                row => [...row.cells].every(cell => cell.getBoundingClientRect().width > 0)
                  && row.closest('table').getBoundingClientRect().width > 100
                """), "All assertion columns retain real rendered geometry after navigation");
    }
}
