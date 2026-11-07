package atf.ui.tests.extent.support.inspection.disclosure;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.TestReportPaths;
import atf.ui.tests.extent.support.locators.disclosure.ReportBulkDisclosureLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static atf.reporting.contract.AtfReportStatus.PASS;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Real artifact and repeated owner selection, with no direct selectors in the test. */
public final class ReportBulkDisclosureProbe {
    private static final ReportBulkDisclosureLocators LOCATORS = new ReportBulkDisclosureLocators();
    private ReportBulkDisclosureProbe() { }

    public static void verify() {
        Path report = fixture();
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)))) {
            try {
                Browser browser = browserLease.value();
                Page page = BrowserResources.newPage(browser);
                var errors = new ArrayList<String>();
                page.onPageError(errors::add);
                page.navigate(report.toUri() + "#test-view");
                LOCATORS.owners(page).first().click();
                ready(page);
                assertTree(page, false);
                LOCATORS.expand(page).click();
                assertTree(page, true);
                LOCATORS.expand(page).click();
                assertTree(page, true);
                assertEquals(0, LOCATORS.disclosures(page).evaluateAll(
                        "nodes => nodes.filter(node => node.open).length"),
                        "bulk structural expansion must not override local evidence disclosure state");
                LOCATORS.owners(page).nth(1).click();
                ready(page);
                assertTree(page, false);
                LOCATORS.expand(page).press("Enter");
                assertTree(page, true);
                LOCATORS.collapse(page).press(" ");
                assertTree(page, false);
                LOCATORS.expand(page).click();
                assertTree(page, true);
                LOCATORS.collapse(page).click();
                assertTree(page, false);
                assertTrue(errors.isEmpty(), errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void ready(Page page) {
        page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
    }

    private static void assertTree(Page page, boolean open) {
        var cards = LOCATORS.cards(page);
        assertEquals(10, cards.count(), "two scenarios with phase, request, validation and nested exchange");
        for (int index = 0; index < cards.count(); index++) {
            var card = cards.nth(index);
            String attribute = card.getAttribute("data-atf-scenario-open") != null ? "data-atf-scenario-open"
                    : "phase".equals(card.getAttribute("data-atf-node-role")) ? "data-atf-phase-open" : "data-atf-node-open";
            assertThat(card).hasAttribute(attribute, Boolean.toString(open));
            assertThat(LOCATORS.header(card)).hasAttribute("aria-expanded", Boolean.toString(open));
            if (open) assertThat(LOCATORS.header(card)).isVisible();
        }
    }

    private static Path fixture() {
        Path report = TestReportPaths.report("bulk-semantic-disclosures.html");
        try (var session = new ExtentReportSession(report)) {
            for (String owner : List.of("BulkOwnerA", "BulkOwnerB")) {
                for (int index = 1; index <= 2; index++) {
                    var scenario = session.startScenario(owner, "Component tree " + index, List.of("component"));
                    var phase = scenario.startPhase("TEST BODY");
                    var request = phase.startRequest("THEN checkpoint " + index, "", "");
                    var validation = request.startValidation("API validation");
                    var exchange = validation.startRequest("Captured call", "GET", "/resource");
                    exchange.json("REQUEST JSON", "null");
                    exchange.json("RESPONSE JSON", "{\"accepted\":true}");
                    exchange.complete(PASS);
                    validation.complete(PASS);
                    request.complete(PASS);
                    phase.complete(PASS);
                    scenario.complete(PASS);
                }
            }
        }
        return report;
    }
}
