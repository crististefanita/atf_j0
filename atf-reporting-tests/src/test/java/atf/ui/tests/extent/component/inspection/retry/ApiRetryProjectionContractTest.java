package atf.ui.tests.extent.component.inspection.retry;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportRetryEvent;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_EVIDENCE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rendered exchange cardinality for standalone, fallback and UI+API; no consumer dependency. */
@AtfComponentTest
@ReportBrowserTest
class ApiRetryProjectionContractTest {
    private static final String PASSWORD = "projection-form-secret";
    private static final String RETRY_SECRET = "projection-retry-secret";
    private static final String TEST_BODY = "TEST BODY";
    private static final ReportPhaseAuditLocators PHASES = new ReportPhaseAuditLocators();
    private static final ReportDisclosureLocators DISCLOSURES = new ReportDisclosureLocators();
    private static Playwright playwright;
    private static Browser browser;

    enum Projection { STANDALONE, FALLBACK, UI_API }

    @TempDir Path directory;

    @BeforeAll
    static void openBrowser() {
        playwright = BrowserResources.create(Playwright::create);
        try {
            browser = BrowserResources.launch(playwright, new BrowserType.LaunchOptions().setHeadless(true));
        } catch (RuntimeException failure) {
            try { BrowserResources.close(playwright); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    @AfterAll
    static void closeBrowser() {
        BrowserResources.closeAll(browser, playwright);
    }

    @ParameterizedTest
    @CsvSource({"STANDALONE,200,true", "FALLBACK,200,true", "UI_API,200,true",
            "STANDALONE,503,true", "FALLBACK,503,true", "UI_API,503,true",
            "STANDALONE,200,false", "FALLBACK,200,false", "UI_API,200,false"})
    void oneExchangePipelineCoversSingleAttemptRetrySuccessAndExhaustion(
            Projection projection, int finalStatus, boolean retried) throws Exception {
        var evidence = evidence(finalStatus, retried);
        var status = finalStatus == 200 ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        Path report = publish(projection, evidence, status);
        String html = Files.readString(report);

        assertFalse(html.contains(PASSWORD));
        assertFalse(html.contains(RETRY_SECRET));
        try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
            try {
                Page page = pageLease.value();
                boolean fallback = projection == Projection.FALLBACK;
                page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
                if (fallback) PHASES.testsTab(page, true).click();
                else {
                    PHASES.firstOwner(page).click();
                    page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                }
                var scenario = PHASES.scenarios(page, fallback).first();
                var phase = PHASES.phases(scenario).first();
                assertEquals(1, PHASES.requests(phase, fallback).count());
                var request = PHASES.requests(phase, fallback).first();
                if (projection == Projection.UI_API) {
                    assertEquals(1, DISCLOSURES.validations(request).count());
                    var validation = DISCLOSURES.validations(request).first();
                    assertEquals(1, PHASES.requests(validation, false).count());
                    request = PHASES.requests(validation, false).first();
                }
                var requestDetails = PHASES.evidence(request, REQUEST_EVIDENCE);
                var responseDetails = PHASES.evidence(request, RESPONSE_EVIDENCE);
                var executionDetails = PHASES.evidence(request, EXECUTION_EVIDENCE);
                String requestHtml = request.innerHTML();
                assertAll(
                        () -> assertEquals(1, requestDetails.count()),
                        () -> assertEquals(1, responseDetails.count()),
                        () -> assertEquals(1, executionDetails.count()),
                        () -> assertTrue(DISCLOSURES.summary(requestDetails).textContent().trim()
                                .matches("Request details · \\d+ fields?")),
                        () -> assertTrue(DISCLOSURES.summary(responseDetails).textContent().trim()
                                .matches("Response details · \\d+ fields?")),
                        () -> assertEquals(retried ? "Execution evidence · 2 attempts" : "Execution evidence · 1 attempt",
                                DISCLOSURES.summary(executionDetails).textContent().trim()),
                        () -> assertTrue(requestHtml.indexOf("Request details ·")
                                < requestHtml.indexOf("Response details ·")),
                        () -> assertTrue(requestHtml.indexOf("Response details ·")
                                < requestHtml.indexOf("Execution evidence ·")),
                        () -> assertEquals(retried, executionDetails.textContent().contains("temporary upstream")),
                        () -> assertTrue(executionDetails.textContent().contains("final observed response")));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private Path publish(Projection projection, ReportExecutionEvidence evidence, AtfReportStatus status) {
        if (projection == Projection.UI_API) {
            try (var session = UiExtentReportSession.openConfigured(directory, "ui-api.html", AtfConsoleSink.noOp())) {
                var scenario = session.startScenario("UiContract", "neutral.contract", "session ready",
                        List.of("component", "ui", "api"));
                var phase = scenario.startPhase(TEST_BODY);
                phase.apiTrace(THEN, "session is ready", "/account", observedExchanges(List.of(evidence)), status);
                phase.complete(status);
                scenario.complete(status);
                return session.reportPath();
            }
        }
        Path report = directory.resolve("standalone.html");
        try (var session = open(projection, report)) {
            var scenario = session.startScenario("ApiContract", "session ready", List.of("component", "api"));
            var phase = scenario.startPhase(TEST_BODY);
            var request = phase.startRequest("POST /sessions — establish a session", "POST", "/sessions");
            ApiExchangeEvidence.publish(request, evidence);
            var validation = request.startValidation("Contract validation");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "200",
                    Integer.toString(evidence.response().status()), status == AtfReportStatus.PASS)));
            validation.complete(status);
            request.complete(status);
            BrowserResources.close(request);
            phase.complete(status);
            scenario.complete(status);
            var snapshot = snapshot(session);
            assertEquals(1, snapshot.requestCount());
            assertEquals(1, snapshot.validationCount());
            assertEquals(status, snapshot.classes().getFirst().scenarios().getFirst().status());
        }
        return report;
    }

    private static AtfReportPorts.Session open(Projection projection, Path report) {
        return switch (projection) {
            case STANDALONE -> new ExtentReportSession(report);
            case FALLBACK -> new HtmlReportSession(report);
            case UI_API -> throw new IllegalArgumentException("UI uses its own public session");
        };
    }

    private static AtfReportSemanticSnapshot snapshot(AtfReportPorts.Session session) {
        return session instanceof ExtentReportSession extent
                ? extent.semanticSnapshot() : ((HtmlReportSession) session).semanticSnapshot();
    }

    private static ReportExecutionEvidence evidence(int finalStatus, boolean retried) {
        var request = new ReportRequest("POST", "/sessions", Map.of(),
                Map.of("Content-Type", "application/x-www-form-urlencoded"), "none",
                "email=report%40example.test&password=" + PASSWORD);
        var response = new ReportResponse(finalStatus, "{\"observed\":true}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(11),
                "https://api.example.test/sessions");
        var last = new ReportAttemptRecord(retried ? 2 : 1, finalStatus, Duration.ofMillis(11),
                finalStatus == 200 ? "SUCCESS" : "FAILURE", "final observed response");
        return new ReportExecutionEvidence(response, retried ? 2 : 1,
                Duration.ofMillis(retried ? 18 : 11),
                retried ? List.of(new ReportRetryEvent(1, 503, "Retry-After", Duration.ofMillis(5),
                        "token=" + RETRY_SECRET)) : List.of(),
                retried ? List.of(new ReportAttemptRecord(1, 503, Duration.ofMillis(7), "RETRYABLE_RESPONSE",
                        "temporary upstream"), last) : List.of(last),
                request, ReportTransportProvenance.ACCEPTANCE_MOCK);
    }
}
