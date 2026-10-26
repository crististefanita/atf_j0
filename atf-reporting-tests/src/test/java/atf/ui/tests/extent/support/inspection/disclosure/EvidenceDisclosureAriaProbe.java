package atf.ui.tests.extent.support.inspection.disclosure;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.TestReportPaths;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static atf.reporting.contract.AtfReportStatus.PASS;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_EVIDENCE;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real renderer and runtime; selectors remain in the shared locator boundary. */
public final class EvidenceDisclosureAriaProbe {
    private static final ReportDisclosureLocators LOCATORS = new ReportDisclosureLocators();

    private EvidenceDisclosureAriaProbe() { }

    public static void verify() {
        Path report = fixture();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            page.setDefaultTimeout(5_000);
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.navigate(report.toUri() + "#test-view");
            for (int selection = 0; selection < 2; selection++) {
                LOCATORS.firstOwner(page).click();
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                Locator scenario = LOCATORS.scenarios(page).first();
                open(scenario, "data-atf-scenario-open");
                Locator phase = LOCATORS.firstPhase(scenario);
                open(phase, "data-atf-phase-open");
                Locator request = LOCATORS.requests(phase).first();
                open(request, "data-atf-node-open");
                for (var kind : List.of(REQUEST_EVIDENCE, RESPONSE_EVIDENCE, EXECUTION_EVIDENCE)) {
                    Locator details = LOCATORS.evidence(request, kind);
                    Locator summary = LOCATORS.summary(details);
                    assertState(details, false);
                    summary.click();
                    assertState(details, true);
                    summary.click();
                    assertState(details, false);
                    summary.press("Enter");
                    assertState(details, true);
                    summary.press(" ");
                    assertState(details, false);
                }
            }
            assertTrue(errors.isEmpty(), errors.toString());
        }
    }

    private static void assertState(Locator details, boolean open) {
        assertThat(LOCATORS.summary(details)).hasAttribute("aria-expanded", Boolean.toString(open));
        assertEquals(open, details.evaluate("element => element.open"));
    }

    private static void open(Locator card, String state) {
        if (!"true".equals(card.getAttribute(state))) LOCATORS.header(card).click();
    }

    private static Path fixture() {
        Path report = TestReportPaths.report("native-evidence-aria.html");
        var headers = Map.of("content-type", "application/json");
        var duration = Duration.ofMillis(29);
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("DisclosureAria", "native renderer disclosures", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("QUERY /resource — preserve evidence", "QUERY", "/resource");
            request.detail("request.method", "QUERY");
            request.json("REQUEST JSON", "{\"query\":\"value\"}");
            request.detail("response.status", "200");
            request.json("RESPONSE JSON", "{\"accepted\":true}");
            request.execution(new ReportExecutionEvidence(
                    new ReportResponse(200, "{\"accepted\":true}", headers, duration, "https://api.example.test/resource"),
                    1, duration, List.of(), List.of(),
                    new ReportRequest("QUERY", "/resource", Map.of(), headers, "none", "{\"query\":\"value\"}"),
                    ReportTransportProvenance.ACCEPTANCE_MOCK));
            request.complete(PASS);
            phase.complete(PASS);
            scenario.complete(PASS);
        }
        return report;
    }
}
