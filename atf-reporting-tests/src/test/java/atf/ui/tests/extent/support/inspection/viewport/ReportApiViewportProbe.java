package atf.ui.tests.extent.support.inspection.viewport;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfReportPorts.Phase;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
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

import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Controlled fallback fixture and computed-style assertions for the common API viewport. */
public final class ReportApiViewportProbe {
    private static final int LONG_ROWS = 40;
    private static final ReportDisclosureLocators LOCATORS = new ReportDisclosureLocators();

    private ReportApiViewportProbe() { }

    public static void verifyExtentShortTables() {
        Path report = TestReportPaths.report("extent-short-api-viewport.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ViewportComponent", "short API tables", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            addRequest(phase, "short", 1);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 900);
            page.navigate(report.toUri() + "#test-view");
            LOCATORS.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            Locator scenario = LOCATORS.scenarios(page).first();
            openNode(scenario, "data-atf-scenario-open");
            Locator phase = LOCATORS.firstPhase(scenario);
            openNode(phase, "data-atf-phase-open");
            Locator request = LOCATORS.requests(phase).first();
            openNode(request, "data-atf-node-open");
            LOCATORS.summary(LOCATORS.evidence(request, REQUEST_EVIDENCE)).click();
            LOCATORS.summary(LOCATORS.evidence(request, EXECUTION_EVIDENCE)).click();
            Locator requestTable = LOCATORS.viewport(request, REQUEST_EVIDENCE);
            Locator retryTable = LOCATORS.viewport(request, EXECUTION_EVIDENCE);
            assertNatural(requestTable);
            assertNatural(retryTable);
            page.setViewportSize(760, 900);
            assertNatural(requestTable);
            assertNatural(retryTable);
            assertEquals(Boolean.TRUE, page.evaluate(
                    "document.documentElement.scrollWidth <= document.documentElement.clientWidth"));
        }
    }

    public static void verifyFallbackTables() {
        Path report = fallbackReport();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 900);
            page.navigate(report.toUri().toString());
            LOCATORS.fallbackTestsTab(page).click();
            Locator requests = LOCATORS.fallbackRequests(page);
            assertEquals(2, requests.count());
            for (int index = 0; index < requests.count(); index++) {
                Locator request = requests.nth(index);
                LOCATORS.summary(LOCATORS.evidence(request, REQUEST_EVIDENCE)).click();
                LOCATORS.summary(LOCATORS.evidence(request, EXECUTION_EVIDENCE)).click();
            }
            verifyAtWidth(page, requests);
            page.setViewportSize(760, 900);
            verifyAtWidth(page, requests);
        }
    }

    private static void verifyAtWidth(Page page, Locator requests) {
        Locator shortRequest = requests.nth(0);
        Locator longRequest = requests.nth(1);
        assertNatural(LOCATORS.viewport(shortRequest, REQUEST_EVIDENCE));
        assertNatural(LOCATORS.viewport(shortRequest, EXECUTION_EVIDENCE));
        assertScrollable(LOCATORS.viewport(longRequest, REQUEST_EVIDENCE), LONG_ROWS);
        assertScrollable(LOCATORS.viewport(longRequest, EXECUTION_EVIDENCE), LONG_ROWS);
        assertEquals(Boolean.TRUE, page.evaluate(
                "document.documentElement.scrollWidth <= document.documentElement.clientWidth"),
                "API tables must not create document-level horizontal overflow");
    }

    private static void openNode(Locator node, String attribute) {
        if (!"true".equals(node.getAttribute(attribute))) {
            LOCATORS.header(node).click();
        }
    }

    private static void assertNatural(Locator viewport) {
        assertEquals(Boolean.TRUE, viewport.evaluate("element => {"
                + "const style=getComputedStyle(element);"
                + "const table=element.querySelector('table'), cap=parseFloat(style.maxHeight);"
                + "return style.overflowY==='auto' && Math.abs(element.clientHeight-"
                + "Math.min(table.getBoundingClientRect().height,cap))<=2;}"),
                "short table must keep natural height without empty reserved space");
    }

    private static void assertScrollable(Locator viewport, int expectedRows) {
        assertEquals(expectedRows, LOCATORS.rows(viewport).count());
        assertTrue(Boolean.TRUE.equals(viewport.evaluate("element => {"
                + "const style=getComputedStyle(element), header=element.querySelector('thead th'),"
                + "last=element.querySelector('tbody tr:last-child');"
                + "if(style.overflowY!=='auto'||element.scrollHeight<=element.clientHeight"
                + "||!header||!last)return false;"
                + "element.scrollTop=element.scrollHeight;"
                + "const bounds=element.getBoundingClientRect();"
                + "const table=element.querySelector('table'),stacked="
                + "getComputedStyle(table).getPropertyValue('--atf-table-stacked').trim()==='1';"
                + "const labels=Array.from(table.querySelectorAll('tbody tr')).every(r=>Array.from(r.cells).every(c=>"
                + "c.getAttribute('data-atf-column-label')&&getComputedStyle(c,'::before').content!=='none'));"
                + "return (stacked?labels:getComputedStyle(header).position==='sticky'"
                + "&&header.getBoundingClientRect().top>=bounds.top-1)"
                + "&&last.getBoundingClientRect().bottom<=bounds.bottom+1;}")),
                "long table must scroll locally with sticky header and reachable last row");
    }

    private static Path fallbackReport() {
        Path report = TestReportPaths.report("fallback-api-viewport-parity.html");
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("ViewportComponent", "short and long API tables", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            addRequest(phase, "short", 1);
            addRequest(phase, "long", LONG_ROWS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        return report;
    }

    private static void addRequest(Phase phase,
                                   String title, int rows) {
        var scope = phase.startRequest(title, "GET", "/resource");
        for (int index = 1; index <= rows; index++) {
            scope.detail("request.header.X-Diagnostic-" + index, "value-" + index);
        }
        Duration duration = Duration.ofMillis(rows);
        var request = new ReportRequest("GET", "/resource", Map.of(), Map.of(), "none", "");
        var response = new ReportResponse(200, "{}", Map.of(), duration,
                "https://api.example.test/resource");
        var attempts = new ArrayList<ReportAttemptRecord>();
        for (int index = 1; index <= rows; index++) {
            attempts.add(new ReportAttemptRecord(index, 200, Duration.ofMillis(1),
                    "SUCCESS", "attempt " + index));
        }
        scope.execution(new ReportExecutionEvidence(response, rows, duration,
                List.of(), attempts, request, ReportTransportProvenance.ACCEPTANCE_MOCK));
        scope.complete(AtfReportStatus.PASS);
    }
}
