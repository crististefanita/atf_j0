package atf.ui.tests.extent.component.inspection.profile;

import atf.ui.tests.extent.support.lifecycle.SinglePageBrowserProbe;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.nio.file.Path;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** The original browser oracle, isolated without changing its action/navigation budgets. */
public final class ArtifactProfileBrowserProbe extends SinglePageBrowserProbe {
    private static final ReportPhaseAuditLocators LOCATORS = new ReportPhaseAuditLocators();
    @Override public void verify(String[] arguments) {
        verifyBrowser(Path.of(arguments[0]), Boolean.parseBoolean(arguments[1]),
                Integer.parseInt(arguments[2]), Boolean.parseBoolean(arguments[3]));
    }

    private void verifyBrowser(Path file, boolean fallback, int original, boolean http) {
        Page page = page();
        page.navigate(file.toUri() + (fallback ? "" : "#test-view"));
        if (fallback) LOCATORS.testsTab(page, true).click();
        else {
            LOCATORS.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
        }
        var scenario = LOCATORS.scenarios(page, fallback).nth(original);
        if (!fallback) LOCATORS.header(scenario).click();
        var phase = LOCATORS.phases(scenario).first();
        if (!fallback) LOCATORS.header(phase).click();
        Locator group;
        if (http) {
            var request = LOCATORS.requests(phase, fallback).first();
            if (!fallback) LOCATORS.header(request).click();
            var validation = request.locator("[data-atf-node-role=validation]").first();
            if (fallback) validation.locator("details.assertion-evidence > summary").click(); else LOCATORS.header(validation).click();
            group = validation;
        } else {
            group = LOCATORS.assertionStep(phase).first();
            if (fallback) group.locator("summary").first().press("Space"); else LOCATORS.header(group).press("Enter");
        }
        var table = group.locator((fallback ? "table.assertions" : "table.helm-validation-table") + ":visible");
        assertThat(table).isVisible();
        var rows = table.locator("tbody > tr"); assertEquals(3, rows.count());
        assertEquals("13", rows.first().locator("td").nth(2).innerText());
        assertEquals(fallback ? "Fail" : "❌", rows.first().locator("td").nth(3).innerText());
        assertEquals(rows.nth(1).locator("td").nth(1).innerText(), rows.nth(1).locator("td").nth(2).innerText(), "bounded previews may match while the original verdict stays FAIL");
        assertEquals(fallback ? "Fail" : "❌", rows.nth(1).locator("td").nth(3).innerText());
        assertThat(rows.last()).isVisible(); assertTrue(rows.last().innerText().contains("final original comparison"));
    }

}
