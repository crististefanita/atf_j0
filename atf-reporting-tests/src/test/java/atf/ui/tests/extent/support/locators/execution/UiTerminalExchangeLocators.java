package atf.ui.tests.extent.support.locators.execution;

import com.microsoft.playwright.Locator;

/** Selects the real nested exchange, independently of its generated title or diagnostic preview. */
public final class UiTerminalExchangeLocators {
    public Locator exchanges(Locator scenario, boolean fallback) {
        return scenario.locator(fallback ? "[class~='request']:not(:has([class~='request']))"
                : "div[data-atf-node-role='request']:not(:has(div[data-atf-node-role='request']))");
    }
    public Locator execution(Locator request) { return request.locator("details[class~='execution-evidence']"); }
    public Locator requestDetails(Locator request) { return request.locator("details[class~='request-evidence']"); }
    public Locator responses(Locator request) { return request.locator("details[class~='response-evidence']"); }
    public Locator summaryRows(Locator execution) { return execution.locator("table[class~='execution-table'] tbody tr"); }
    public Locator cell(Locator row) { return row.locator(":scope > td").last(); }
}
