package atf.shop.locators.reporting;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;

/** Test-only locator facade for the Shop order checkpoint artifact; no renderer implementation lives here. */
public final class ShopOrderReportLocators {
    public List<Locator> summaries(Page page) { return page.locator("summary:visible").all(); }
    public Locator responseComparison(Page page) {
        return page.locator("tr:visible:has(> td[data-atf-column-label='ASSERTION'])")
                .filter(new Locator.FilterOptions().setHasText("API response · order creation"));
    }
    public Locator cells(Locator row) { return row.locator("td"); }
    public Locator parent(Locator element) { return element.locator(".."); }
    public Locator responseDetails(Page page) {
        return page.locator("summary:visible").filter(new Locator.FilterOptions().setHasText("Response details"));
    }
    public Locator expandAll(Page page) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Expand all nodes"));
    }
    public Locator failureLink(Page page, boolean fallback) {
        return fallback ? page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions()
                .setHasText("API response · order creation")).first()
                : page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Open in Tests"));
    }
    public Locator failedMetric(Page page, boolean fallback) {
        return page.getByText(fallback ? "Failed" : "Tests Failed", new Page.GetByTextOptions().setExact(true))
                .locator("..").locator(fallback ? "strong" : "h3");
    }
    public Locator tab(Page page, boolean fallback, String name) {
        return fallback ? page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true))
                : page.locator(switch (name) { case "Tests" -> "#nav-test"; case "Fails" -> "#nav-fails"; default -> "#nav-overview"; });
    }
}
