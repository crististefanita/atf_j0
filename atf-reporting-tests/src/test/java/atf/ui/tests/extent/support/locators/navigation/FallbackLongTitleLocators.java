package atf.ui.tests.extent.support.locators.navigation;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public final class FallbackLongTitleLocators {
    public Locator tests(Page page) { return page.locator("button[data-tab='tests']"); }
    public Locator layout(Page page) { return page.locator("section[id='tests'] > div[class~='layout']"); }
    public Locator sidebar(Page page) { return layout(page).locator(":scope > aside"); }
    public Locator content(Page page) { return layout(page).locator(":scope > div[class~='test-content']"); }
    public Locator firstScenario(Page page) { return content(page).locator("article[class~='scenario']").first(); }
    public Locator links(Page page) { return sidebar(page).locator("a[class~='test-link']"); }
    public Locator scenario(Page page, String id) { return page.locator("article[class~='scenario'][id='" + id + "']"); }
    public Locator heading(Locator scenario) { return scenario.locator(":scope > div[class~='scenario-title'] > h2"); }
    public Locator assertionSummary(Locator scenario) { return scenario.locator("details[data-atf-node-role='assertion-step'] > summary"); }
    public Locator assertionRows(Locator scenario) { return scenario.locator("table[class~='assertions'] > tbody > tr"); }
}
