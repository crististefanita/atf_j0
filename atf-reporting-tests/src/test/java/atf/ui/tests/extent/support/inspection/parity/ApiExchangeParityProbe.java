package atf.ui.tests.extent.support.inspection.parity;

import atf.api.reporting.api.ReportAttemptRecord;
import atf.api.reporting.api.ReportRequest;
import atf.api.reporting.api.ReportResponse;
import atf.api.reporting.api.ReportTransportProvenance;
import atf.api.reporting.api.evidence.ApiExchangeEvidence;
import atf.api.reporting.api.evidence.ReportBodyAvailability;
import atf.api.reporting.console.AtfConsoleSink;
import atf.api.reporting.configuration.ReportingSettings;
import atf.api.reporting.security.SensitiveDataRedactor;
import atf.api.reporting.contract.AtfReportPorts;
import atf.api.core.reporting.contract.AtfReportStatus;
import atf.api.reporting.extent.session.ExtentReportSession;
import atf.api.reporting.html.HtmlReportSession;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.reporting.support.TestReportPaths;
import atf.api.reporting.support.CoreReportPortsAdapter;
import atf.api.reporting.ui.UiExtentReportSession;
import atf.api.reporting.ui.UiExtentReportOptions;
import atf.ui.tests.extent.support.locators.parity.ApiExchangeParityLocators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static atf.api.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.api.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.api.reporting.contract.AtfReportStatus.INFO;
import static atf.api.reporting.contract.AtfReportStatus.PASS;
import static atf.api.reporting.contract.AtfReportStatus.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Same immutable input, independently inspected in real Extent/UI/fallback artifacts. */
public final class ApiExchangeParityProbe {
    private static final ApiExchangeParityLocators LOCATORS = new ApiExchangeParityLocators();
    private static final List<String> CHANNELS = List.of("request-evidence", "response-evidence", "execution-evidence");
    private static final List<String> CASES = List.of("query measured", "get absent", "put json", "json null", "capture unavailable", "mock replay");
    private static final String RESPONSE = "{\"accepted\":true,\"id\":7}";
    private static final Map<String, String> ORIGIN = Map.of("Evidence origin",
            "Component fixture: deterministic captured snapshots; this test executes no network");
    private ApiExchangeParityProbe() { }

    public static void verify() {
        String stamp = Instant.now().toString().replace(':', '-');
        Path api = TestReportPaths.report("api-exchange-parity_" + stamp + ".html");
        Path fallback = TestReportPaths.report("fallback-exchange-parity_" + stamp + ".html");
        Path ui = fixtures(api, fallback);
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            var errors = new ArrayList<String>();
            page.onPageError(errors::add);
            page.setViewportSize(1280, 900);
            page.navigate(api.toUri() + "#test-view");
            select(page, 0);
            var direct = inspect(page, false);
            select(page, 1);
            assertEquals(direct, inspect(page, false), "legacy field seam must preserve the typed projection");
            page.navigate(ui.toUri() + "#test-view");
            select(page, 0);
            assertEquals(direct, inspect(page, false), "UI nesting must not change API evidence ownership or payloads");
            page.setViewportSize(960, 900);
            assertEquals(Boolean.TRUE, page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth"));
            page.navigate(fallback.toUri().toString());
            LOCATORS.fallbackTests(page).click();
            assertEquals(direct, inspect(page, true), "fallback and Extent must use the same evidence schema");
            assertTrue(errors.isEmpty(), errors.toString());
        }
    }

    private static void select(Page page, int owner) {
        LOCATORS.owners(page).nth(owner).click();
        page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
        LOCATORS.expand(page).click();
    }

    private static List<Map<String, Object>> inspect(Page page, boolean fallback) {
        var requests = LOCATORS.requests(page, fallback);
        assertEquals(CASES.size(), requests.count(), "every configured fixture case must be rendered");
        var snapshots = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < requests.count(); index++) {
            var snapshot = new LinkedHashMap<String, Object>();
            for (String kind : CHANNELS) {
                var evidence = LOCATORS.evidence(requests.nth(index), kind);
                assertEquals(1, evidence.count(), CASES.get(index) + ": " + kind);
                assertTrue(evidence.isVisible(), "owner expansion exposes each structural evidence header");
                if (!Boolean.TRUE.equals(evidence.evaluate("element => element.open"))) LOCATORS.summary(evidence).click();
                snapshot.put(kind, fields(evidence));
                var documents = LOCATORS.documents(evidence).allTextContents();
                assertEquals(kind.equals("execution-evidence") || index == 1 || index == 4 ? 0 : 1,
                        documents.size(), CASES.get(index)
                                + ": only captured JSON has a payload; absence and missing capture stay explicit");
                snapshot.put(kind + " JSON", documents.stream().map(ApiExchangeParityProbe::json).toList());
            }
            String text = requests.nth(index).innerText();
            assertFalse(text.contains("raw-secret"), "all public projections must redact headers and replay");
            if (index == 0) {
                assertTrue(text.contains("17 ms"));
                assertTrue(text.contains("43 ms"));
                assertTrue(text.contains("curl --request 'QUERY'"));
                assertTrue(text.contains("--header 'Authorization: ***REDACTED***'"),
                        "renderer redaction must preserve both shell header quotes regardless of header order");
                assertTrue(text.contains("--data-raw '{\"query\":\"value\"}'"),
                        "the quoted body must remain a separate replay argument");
            }
            if (index == 1) assertTrue(text.contains("0 ms"), "measured zero remains zero");
            if (index == 4) assertTrue(text.contains("Not captured"), "missing capture must be explained");
            if (index == 5) assertTrue(text.contains("NOT EXECUTABLE"), "mock URLs are not live replay evidence");
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> fields(Locator evidence) {
        var rows = (List<List<String>>) LOCATORS.rows(evidence).evaluateAll(
                "rows => rows.map(row => [...row.querySelectorAll(':scope > td,:scope > th')].map(cell => cell.textContent.trim()))");
        var fields = new LinkedHashMap<String, String>();
        for (var row : rows) {
            if (row.size() != 2 || row.get(0).equalsIgnoreCase("FIELD") || row.get(0).equals("TRACE EXCHANGE")) continue;
            assertFalse(fields.containsKey(row.get(0)), "duplicate field: " + row.get(0));
            fields.put(row.get(0), row.get(1));
        }
        return fields;
    }

    private static Object json(String text) {
        try { return new ObjectMapper().readTree(text); }
        catch (com.fasterxml.jackson.core.JsonProcessingException error) { throw new AssertionError("Rendered JSON is malformed", error); }
    }

    private static Path fixtures(Path api, Path fallback) {
        var options = UiExtentReportOptions.builder(api.getParent()).fileName("ui-exchange-parity.html")
                .systemInfo(ORIGIN).console(AtfConsoleSink.noOp()).build();
        try (var extent = new ExtentReportSession(api, ReportingSettings.defaults(),
                new SensitiveDataRedactor(), AtfConsoleSink.noOp(), ORIGIN);
             var html = new HtmlReportSession(fallback);
             var ui = UiExtentReportSession.openConfigured(options)) {
            for (int index = 0; index < CASES.size(); index++) {
                var evidence = evidence(index);
                direct(extent, "TypedEvidence", index, evidence, false);
                direct(extent, "LegacyAdapter", index, evidence, true);
                direct(html, "TypedEvidence", index, evidence, false);
                var scenario = ui.startScenario("NestedEvidence", "atf.component", CASES.get(index), List.of("component", "captured-snapshot"));
                var phase = scenario.startPhase("TEST BODY");
                phase.step(THEN, CASES.get(index), "https://ui.example.test/checkpoint", "", List.of(), List.of(), observedExchanges(List.of(evidence)), atf.api.reporting.evidence.ReportScreenshotPolicy.CapturePoint.CHECKPOINT, PASS);
                phase.complete(PASS);
                scenario.complete(PASS);
            }
            return ui.reportPath();
        }
    }

    private static void direct(AtfReportPorts.Session session, String owner, int index,
                               ReportExecutionEvidence evidence, boolean legacyFields) {
        var status = evidence.response().status() < 0 ? WARNING : INFO;
        if (legacyFields) {
            var scenario = CoreReportPortsAdapter.wrap(session).startScenario(owner, CASES.get(index), List.of("component", "captured-snapshot"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest(CASES.get(index), evidence.request().method(), evidence.request().path());
            CoreReportPortsAdapter.publishEvidence(request, evidence, null);
            var coreStatus = AtfReportStatus.valueOf(status.name());
            request.complete(coreStatus);
            phase.complete(coreStatus);
            scenario.complete(coreStatus);
            return;
        }
        var scenario = session.startScenario(owner, CASES.get(index), List.of("component", "captured-snapshot"));
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest(CASES.get(index), evidence.request().method(), evidence.request().path());
        ApiExchangeEvidence.publish(request, evidence);
        request.complete(status);
        phase.complete(status);
        scenario.complete(status);
    }

    private static ReportExecutionEvidence evidence(int index) {
        String method = index == 0 ? "QUERY" : index == 2 ? "PUT" : "GET";
        String requestBody = index == 4 ? null : index == 3 ? "null" : index == 1 ? "" : "{\"query\":\"value\"}";
        String responseBody = index == 4 ? null : index == 3 ? "null" : index == 1 ? "" : RESPONSE;
        Duration duration = index == 4 ? null : index == 1 ? Duration.ZERO : Duration.ofMillis(17);
        Duration total = index == 4 ? null : index == 1 ? Duration.ZERO : Duration.ofMillis(43);
        var request = new ReportRequest(method, "/resource", Map.of("page", "1"),
                Map.of("Content-Type", "application/json", "Authorization", "Bearer raw-secret"), "bearer", requestBody);
        var response = new ReportResponse(index == 4 ? -1 : index == 1 ? 204 : 200, responseBody,
                Map.of("Content-Type", "application/json", "X-Correlation", "fixture"), duration, "https://api.example.test/resource?page=1");
        return new ReportExecutionEvidence(response, 1, total, List.of(),
                List.of(new ReportAttemptRecord(1, response.status(), duration, index == 4 ? "FAILURE" : "SUCCESS", "fixture")),
                request, index == 5 ? ReportTransportProvenance.ACCEPTANCE_MOCK : ReportTransportProvenance.LIVE_HTTP);
    }
}
