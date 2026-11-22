package atf.shop.tests.support.reporting;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import java.nio.file.Path;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Selectors and browser checks for the consumer launch-metadata disclosure. */
public final class ShopLaunchReportProbe {
    private ShopLaunchReportProbe() { }

    public static void verify(Path report, int expected, String selector) throws Exception {
        try (var viewer = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = viewer.page();
            page.navigate(report.toUri().toString());
            page.locator("#nav-test").click();
            page.locator("p.name").filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("First")).click();
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Expand all nodes")).click();
            var detail = page.locator("#test-view .test-content-detail .detail-body");
            var group = detail.locator("details.atf-observation-evidence").last();
            assertFalse((Boolean) group.evaluate("element => element.open"), "Expand-all keeps launch metadata collapsed");
            var phase = group.locator("xpath=ancestor::div[@class='card']")
                    .filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("Launch context")).last();
            var result = phase.locator("xpath=ancestor::div[@class='card'][1]");
            var phaseNode = phase.locator(":scope > .card-header a.node");
            if (!phaseNode.isVisible()) result.locator(":scope > .card-header a.node").click();
            if (!group.isVisible()) phaseNode.click();
            group.locator("summary").click();
            var rows = group.locator("table.helm-evidence-table tbody tr");
            assertEquals(9 + expected, rows.count(), "All discovery fields and selected sources expand");
            assertEquals(Integer.toString(expected), rows.filter(new com.microsoft.playwright.Locator.FilterOptions()
                    .setHasText("EFFECTIVE SELECTION COUNT")).locator("td").nth(1).innerText());
            assertEquals(selector, rows.filter(new com.microsoft.playwright.Locator.FilterOptions()
                    .setHasText("SELECTOR #1")).locator("td").nth(1).innerText());
            assertEquals("0", rows.filter(new com.microsoft.playwright.Locator.FilterOptions()
                    .setHasText("DYNAMIC REGISTRATIONS AT CAPTURE")).locator("td").nth(1).innerText());
            for (int index = 0; index < rows.count(); index++) {
                rows.nth(index).scrollIntoViewIfNeeded();
                assertTrue(rows.nth(index).isVisible(), "Every metadata row is readable");
            }
            page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
                    .setPath(report.resolveSibling("expanded-launch-context.png")));
        }
    }

}
