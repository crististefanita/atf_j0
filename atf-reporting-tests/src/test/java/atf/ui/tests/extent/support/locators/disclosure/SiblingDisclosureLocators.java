package atf.ui.tests.extent.support.locators.disclosure;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.List;

/** Scenario-local controls and artifact mutations, separated from the behavioral oracle. */
public final class SiblingDisclosureLocators {
    public Locator header(Locator card) { return card.locator(":scope > .card-header"); }
    public Locator phase(Locator scenario) { return scenario.locator("[data-atf-node-role='phase']").first(); }
    public Locator request(Locator scenario) { return scenario.locator("[data-atf-node-role='request']").first(); }
    public Locator response(Locator scenario) { return scenario.locator("details").filter(
            new Locator.FilterOptions().setHas(scenario.page().getByText("Response details · 0 fields", new Page.GetByTextOptions().setExact(true)))).first(); }
    public Locator summary(Locator details) { return details.locator(":scope > summary"); }
    public Locator codes(Locator scenario) { return scenario.locator(".response-json-body pre"); }
    public void expandAll(Page page) { page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Expand all nodes").setExact(true)).click(); }
    public void collapseAll(Page page) { page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Collapse all nodes").setExact(true)).click(); }
    public List<String> states(Locator scenario, boolean fallback) {
        if (fallback) return scenario.locator("details").all().stream()
                .map(node -> node.getAttribute("open") == null ? "closed" : "open").toList();
        return scenario.locator("[data-atf-node-role]").all().stream().map(node ->
                node.getAttribute("data-atf-node-role") + ":" + node.getAttribute("aria-expanded")
                        + ":" + header(node).getAttribute("aria-expanded")).toList();
    }
    public void hideBodyKeepingExpandedMetadata(Locator scenario) {
        phase(scenario).evaluate("p => p.querySelector(':scope > .accordion').style.setProperty('display','none','important')");
    }
}
