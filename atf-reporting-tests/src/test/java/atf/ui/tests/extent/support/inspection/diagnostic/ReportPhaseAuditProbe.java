package atf.ui.tests.extent.support.inspection.diagnostic;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.support.TestReportPaths;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_EVIDENCE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Real publication matrix, without an application endpoint or an invented HTTP contract. */
public final class ReportPhaseAuditProbe {
    private static final String INTERNAL_AUDIT = "API audit completeness";
    private static final String INTERNAL_VALUE = "internal capture bookkeeping";
    private static final String FAILURE_MESSAGE = "AssertionError: observed status mismatch; token=private-token";
    private static final String ASSOCIATION_MESSAGE = "response contract was not associated";
    private static final ReportPhaseAuditLocators LOCATORS = new ReportPhaseAuditLocators();
    private static final List<Case> CASES = List.of(
            new Case("empty capture", 0, true, false, false),
            new Case("single exchange", 1, false, false, false),
            new Case("several exchanges", 2, true, false, false),
            new Case("JUnit failure without exchange", 0, true, true, true),
            new Case("JUnit failure with exchanges", 2, true, true, true),
            new Case("association gap without JUnit failure", 0, true, false, true));

    private ReportPhaseAuditProbe() { }

    public static void verifyPhaseDiagnosticIsolation() {
        String setupNote = "informational setup association";
        String actualCause = "actual test body failure";
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            for (boolean fallback : List.of(false, true)) {
                Path report = TestReportPaths.report(fallback
                        ? "fallback-phase-diagnostic-isolation.html" : "extent-phase-diagnostic-isolation.html");
                try (AtfReportPorts.Session session = fallback
                        ? new HtmlReportSession(report) : new ExtentReportSession(report)) {
                    var scenario = session.startScenario("PhaseDiagnosticIsolation", "phase ownership", List.of("api"));
                    var setup = scenario.startPhase("SETUP");
                    setup.detail("API contract association", setupNote);
                    setup.complete(AtfReportStatus.PASS);
                    var body = scenario.startPhase("TEST BODY");
                    body.detail("JUnit failure", actualCause);
                    body.complete(AtfReportStatus.FAIL);
                    scenario.complete(AtfReportStatus.FAIL);
                }
                Page page = browser.newPage();
                page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
                if (!fallback) {
                    LOCATORS.firstOwner(page).click();
                    page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                }
                assertTrue(LOCATORS.body(page).textContent().contains(setupNote),
                        "the informational diagnostic remains available in Tests");
                LOCATORS.failsTab(page, fallback).click();
                String failures = LOCATORS.fails(page, fallback).innerText();
                assertTrue(failures.contains(actualCause));
                assertFalse(failures.contains(setupNote), "a passing phase must not contaminate another failure cause");
                assertFalse(failures.contains("UI action"), "execution diagnostics are not UI action failures");
                page.close();
            }
        }
    }

    public static void rejectsHiddenRenamedAndEmptyPhaseLogBands() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            for (String body : List.of("<details class='phase-details'>Audit details</details>",
                    "<details>Renamed bookkeeping</details>", "")) {
                for (String wrapper : List.of("collapse", "card-body", "legacy-trace")) {
                    page.setContent("""
                            <article class='scenario'><section data-atf-node-role='phase'>
                              <div class='%s' style='display:none'>
                                <table class='table'><tbody><tr><td>Info</td><td>%s</td></tr></tbody></table>
                              </div>
                            </section></article>
                            """.formatted(wrapper, body));
                    Locator phase = LOCATORS.phases(LOCATORS.scenarios(page, true)).first();
                    assertThrows(AssertionError.class, () -> assertNoPhaseLogBand(phase),
                            "CSS hiding, renaming, and an empty wrapper must not satisfy the structural gate");
                }
            }
        }
    }

    public static void verify(boolean fallback) throws Exception {
        Path report = TestReportPaths.report(fallback ? "fallback-phase-audit.html" : "extent-phase-audit.html");
        publish(report, fallback);
        String html = Files.readString(report);
        assertFalse(html.contains("private-token"), "failure diagnostics must cross the redaction boundary");
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            List<String> errors = new ArrayList<>();
            page.onPageError(errors::add);
            page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
            if (fallback) LOCATORS.testsTab(page, true).click();
            else {
                LOCATORS.firstOwner(page).click();
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            }
            Locator scenarios = LOCATORS.scenarios(page, fallback);
            assertEquals(CASES.size(), scenarios.count());
            if (!fallback) {
                assertFalse(LOCATORS.diagnostics(scenarios.nth(3)).isVisible(),
                        "a collapsed scenario must not leak its execution diagnostic");
            }
            for (int index = 0; index < CASES.size(); index++) {
                verifyScenario(scenarios.nth(index), CASES.get(index), fallback);
            }
            LOCATORS.failsTab(page, fallback).click();
            var causes = new ArrayList<String>();
            if (fallback) causes.add(LOCATORS.fails(page, true).innerText());
            else for (int index = 0; index < LOCATORS.failureEntries(page).count(); index++) {
                LOCATORS.failureEntries(page).nth(index).click();
                causes.add(LOCATORS.failureIndex(page).innerText());
            }
            assertTrue(causes.stream().anyMatch(cause -> cause.contains("observed status mismatch")),
                    "Fails must retain the actual JUnit cause even without a contract assertion");
            assertTrue(causes.stream().anyMatch(cause -> cause.contains(ASSOCIATION_MESSAGE)),
                    "Each explicit diagnostic remains reachable through its own Fails record");
            assertAll(
                    () -> assertFalse(LOCATORS.body(page).textContent().contains(INTERNAL_VALUE)),
                    () -> assertTrue(errors.isEmpty(), () -> "Report runtime errors: " + errors));
        }
    }

    private static void verifyScenario(Locator scenario, Case sample, boolean fallback) {
        if (!fallback) open(scenario, "data-atf-scenario-open");
        Locator phase = LOCATORS.phases(scenario).first();
        if (!fallback) open(phase, "data-atf-phase-open");
        assertAll(
                () -> assertEquals(0, LOCATORS.auditBand(scenario).count()),
                () -> assertNoPhaseLogBand(phase),
                () -> assertEquals(0, LOCATORS.diagnostics(phase).count(), "diagnostic owner is the scenario"),
                () -> assertEquals(sample.exchanges(), LOCATORS.requests(phase, fallback).count()),
                () -> assertEquals(sample.hasDiagnostic() ? 1 : 0, LOCATORS.diagnostics(scenario).count()));
        for (int index = 0; index < sample.exchanges(); index++) {
            Locator request = LOCATORS.requests(phase, fallback).nth(index);
            if (!fallback) open(request, "data-atf-node-open");
            assertAll(
                    () -> assertEquals(1, LOCATORS.evidence(request, REQUEST_EVIDENCE).count()),
                    () -> assertEquals(1, LOCATORS.evidence(request, RESPONSE_EVIDENCE).count()),
                    () -> assertEquals(1, LOCATORS.evidence(request, EXECUTION_EVIDENCE).count()),
                    () -> assertTrue(request.textContent().contains("REQUEST JSON")),
                    () -> assertTrue(request.textContent().contains("RESPONSE JSON")));
        }
        if (sample.hasDiagnostic()) {
            Locator diagnostic = LOCATORS.diagnostics(scenario);
            LOCATORS.diagnosticSummary(diagnostic).click();
            assertTrue(diagnostic.innerText().contains(ASSOCIATION_MESSAGE));
            if (sample.failed()) {
                assertTrue(diagnostic.innerText().contains("observed status mismatch"));
                assertTrue(diagnostic.innerText().contains("***REDACTED***"));
            }
            if (!fallback) {
                assertEquals(0, LOCATORS.scenarioLogBodies(scenario).count(),
                        "the scenario diagnostic must not retain a vendor Status/Details/Info wrapper");
                LOCATORS.header(scenario).click();
                assertFalse(diagnostic.isVisible(), "closing the scenario hides its diagnostic subtree");
                LOCATORS.header(scenario).click();
                assertTrue(diagnostic.isVisible(), "reopening restores the local disclosure state");
            }
        }
    }

    private static void publish(Path report, boolean fallback) {
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(report) : new ExtentReportSession(report)) {
            for (Case sample : CASES) {
                var scenario = session.startScenario("PhaseAuditComponent", "atf.component",
                        sample.name(), List.of("api", "component"));
                var phase = scenario.startPhase("TEST BODY");
                if (sample.metadata()) phase.detail(INTERNAL_AUDIT, INTERNAL_VALUE);
                for (int index = 1; index <= sample.exchanges(); index++) publishExchange(phase, index);
                if (sample.failed()) {
                    phase.detail("JUnit failure", FAILURE_MESSAGE);
                }
                if (sample.associationGap()) {
                    phase.detail("API contract association", ASSOCIATION_MESSAGE);
                }
                AtfReportStatus status = sample.failed() ? AtfReportStatus.FAIL : AtfReportStatus.PASS;
                phase.complete(status);
                scenario.complete(status);
            }
            var snapshot = session instanceof ExtentReportSession extent
                    ? extent.semanticSnapshot() : ((HtmlReportSession) session).semanticSnapshot();
            assertEquals(CASES.stream().mapToInt(Case::exchanges).sum(), snapshot.requestCount());
            assertEquals(snapshot.requestCount(), snapshot.validationCount());
            var phases = snapshot.classes().getFirst().scenarios().stream()
                    .map(scenario -> scenario.phases().getFirst()).toList();
            assertEquals(List.of(INTERNAL_AUDIT), phases.getFirst().detailNames());
            assertEquals(List.of(INTERNAL_AUDIT, "JUnit failure", "API contract association"),
                    phases.get(3).detailNames(), "removing presentation must retain internal audit and actual diagnostics");
            assertEquals(AtfReportStatus.PASS,
                    snapshot.classes().getFirst().scenarios().getLast().status(),
                    "an association diagnostic alone must not rewrite the JUnit verdict");
        }
    }

    private static void publishExchange(AtfReportPorts.Phase phase, int index) {
        String path = "/resource/" + index;
        var request = phase.startRequest("create resource " + index, "POST", path);
        request.detail("request.method", "POST");
        request.detail("request.path", path);
        request.detail("HTTP STATUS", "201");
        request.json("REQUEST JSON", "{\"name\":\"fixture\"}");
        request.json("RESPONSE JSON", "{\"id\":" + index + "}");
        Duration duration = Duration.ofMillis(8);
        var response = new ReportResponse(201, "{\"id\":" + index + "}", Map.of(), duration,
                ApiReportTestUrls.atf(path));
        var transport = new ReportRequest("POST", path, Map.of(), Map.of(), "none", "{\"name\":\"fixture\"}");
        request.execution(new ReportExecutionEvidence(response, 1, duration, List.of(),
                List.of(new ReportAttemptRecord(1, 201, duration, "SUCCESS", "controlled fixture")),
                transport, ReportTransportProvenance.ACCEPTANCE_MOCK));
        var validation = request.startValidation("Resource contract");
        validation.assertions(List.of(new AtfAssertion("HTTP status", "201", "201", true)));
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
    }

    private static void open(Locator node, String attribute) {
        if (!"true".equals(node.getAttribute(attribute))) LOCATORS.header(node).click();
    }

    private static void assertNoPhaseLogBand(Locator phase) {
        assertEquals(0, LOCATORS.phaseLogTables(phase).count(), "no phase log table or Info row");
        assertEquals(0, LOCATORS.phaseLogBodies(phase).count(), "no empty phase log container");
    }

    private record Case(String name, int exchanges, boolean metadata, boolean failed, boolean associationGap) {
        private boolean hasDiagnostic() { return failed || associationGap; }
    }
}
