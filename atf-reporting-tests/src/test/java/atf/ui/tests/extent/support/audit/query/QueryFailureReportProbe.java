package atf.ui.tests.extent.support.audit.query;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.api.ReportQueryParameter;
import atf.reporting.api.ReportQueryParameters;
import atf.reporting.api.contract.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.ui.tests.extent.support.locators.verdict.VerdictReportLocators;
import atf.ui.tests.extent.support.locators.query.QueryReportLocators;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import java.nio.file.*;
import java.util.*;
import static atf.reporting.contract.AtfReportStatus.PASS;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Controlled contract evidence, with no synthetic transport or business journey. */
public final class QueryFailureReportProbe {
    private static final VerdictReportLocators UI = new VerdictReportLocators();
    private static final QueryReportLocators DISCLOSURE = new QueryReportLocators();
    private static final String SECRET = "QUERY_FIXTURE_SECRET_73";
    private QueryFailureReportProbe() { }

    public static void verify(boolean fallback) throws Exception {
        Path file = TestReportPaths.report("query-fidelity/" + UUID.randomUUID() + "/" + (fallback ? "fallback" : "extent") + ".html");
        Files.createDirectories(file.getParent());
        var query = new ReportQueryParameters(List.of(new ReportQueryParameter("tag", "red"), new ReportQueryParameter("tag", "blue")));
        var contract = new ApiReportRouteContract("GET", "/query", 200, "{}", Map.of(), "*", query, Map.of(), "json", null);
        var auditor = new ApiReportContractAuditor(() -> List.of(contract));
        var negative = auditor.inspect(ApiReportContractInput.of("GET /query", "https://query.test/query?tag=blue", 200, "", "{}"));
        var positive = auditor.inspect(ApiReportContractInput.of("GET /query", "https://query.test/query?tag=red&tag=blue", 200, "", "{}"));
        assertFalse(negative.compliant()); assertTrue(positive.compliant());
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file);
        try (session) {
            publish(session, "negative query", List.of(new AtfAssertion("Independent comparison", "complete required query",
                    String.join("; ", negative.violations()), negative.compliant()),
                    new AtfAssertion("Bounded diagnostic", "password=" + SECRET + "&suffix=" + "x".repeat(280) + "expected",
                            "password=" + SECRET + "&suffix=" + "x".repeat(280) + "observed", false)));
            publish(session, "positive query sibling", List.of(new AtfAssertion("Complete query contract", "conforms", "conforms", positive.compliant())));
        }
        String html = Files.readString(file); assertFalse(html.contains(SECRET));
        assertFalse(html.contains("x".repeat(280) + "observed"));
        verifyBrowser(file, fallback);
    }

    private static void publish(AtfReportPorts.Session session, String name, List<AtfAssertion> rows) {
        var scenario = session.startScenario("QueryContractFixture", "controlled.query", name, List.of("component", "query-contract"));
        try (var phase = scenario.startPhase("TEST BODY"); var step = phase.startAssertionStep("Query contract assertions")) {
            step.assertions(rows); step.complete(PASS); phase.complete(PASS);
        }
        scenario.complete(PASS);
    }

    private static void verifyBrowser(Path file, boolean fallback) {
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                page.navigate(file.toUri().toString()); UI.overview(page, fallback).click();
                assertEquals("1", UI.metric(page, fallback, fallback ? "Passed" : "Tests Passed").innerText());
                assertEquals("1", UI.metric(page, fallback, fallback ? "Failed" : "Tests Failed").innerText());
                UI.tests(page, fallback).click(); if (!fallback) UI.expandAll(page).click();
                if (fallback) {
                    var disclosure = DISCLOSURE.fallbackAssertions(page);
                    assertThat(disclosure).not().hasAttribute("open", "");
                    DISCLOSURE.summary(disclosure).press("Enter"); assertThat(disclosure).hasAttribute("open", "");
                }
                var table = UI.comparisonTable(page, fallback);
                assertThat(table).isVisible(); assertEquals(2, UI.rows(table).count());
                assertTrue(UI.rows(table).first().innerText().contains("expected query tag"));
                for (String cell : UI.valueCells(table).allInnerTexts()) assertTrue(cell.length() <= 240);
                UI.fails(page, fallback).click(); assertEquals(2, UI.failures(page, fallback).count());
                if (!fallback) UI.failures(page, false).first().click(); UI.openFailure(page, fallback).click();
                var scenario = UI.scenario(page, fallback, "negative query"); assertThat(scenario).isVisible();
                if (!fallback) {
                    var header = UI.scenarioHeader(scenario); assertThat(header).hasAttribute("aria-expanded", "true");
                    header.press("Enter"); assertThat(header).hasAttribute("aria-expanded", "false");
                    header.press("Space"); assertThat(header).hasAttribute("aria-expanded", "true"); UI.expandAll(page).click();
                }
                assertThat(UI.comparisonTable(page, fallback)).isVisible();
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}
