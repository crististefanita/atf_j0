package atf.ui.tests.extent.component.execution;

import atf.reporting.api.*;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.execution.TerminalExecutionLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Renderer-owned no-response fixture; execution policy and consumer business oracles stay at their owners. */
@AtfComponentTest
class TerminalApiExecutionReportTest {
    private static final TerminalExecutionLocators UI = new TerminalExecutionLocators();
    private static final String SECRET = "terminal-evidence-private-token";

    @ParameterizedTest(name = "Terminal evidence parity · fallback={0}")
    @ValueSource(booleans = {false, true})
    void executionDisclosurePreservesOrderedAttemptsAndNoInventedHttpResponse(boolean fallback) throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "terminal-execution-report", UUID.randomUUID().toString()));
        Path file = directory.resolve(fallback ? "fallback.html" : "extent.html");
        var evidence = evidence(2);
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file);
        try (session) {
            var scenario = session.startScenario("TerminalExecutionFixture", getClass().getPackageName(),
                    "503 then terminal transport failure", List.of("component", "controlled-terminal-failure"));
            var phase = scenario.startPhase("TEST BODY");
            try (var request = phase.startRequest("Failed API execution", "GET", "/read")) {
                String diagnostic = ApiExchangeEvidence.publishFailure(request, evidence, "https://fixture.test/read", "FAILED",
                        "controlled terminal failure; " + "bounded-safe-context ".repeat(25) + "; authorization: Bearer " + SECRET);
                assertEquals(240, diagnostic.length()); assertTrue(diagnostic.endsWith("…"));
                request.complete(AtfReportStatus.FAIL);
                phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, diagnostic);
            }
            phase.complete(AtfReportStatus.FAIL); scenario.complete(AtfReportStatus.FAIL);
        }
        String html = Files.readString(file);
        assertFalse(html.contains(SECRET)); assertFalse(html.contains("HTTP 0"));
        assertTrue(html.contains("No HTTP response received for a terminal exchange"));
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage()) {
            page.navigate(file.toAbsolutePath().toUri() + (fallback ? "" : "#test-view"));
            UI.testsTab(page, fallback).click();
            if (!fallback) UI.expandAll(page).click();
            var disclosure = UI.execution(page);
            assertEquals(1, disclosure.count()); assertFalse((Boolean) disclosure.evaluate("node => node.open"));
            assertEquals(0, UI.responseDisclosures(page).count(), "execution diagnostics are not an HTTP response");
            UI.summary(disclosure).click();
            var attempts = UI.attempts(disclosure);
            assertEquals(2, attempts.count());
            assertEquals("503", UI.cells(attempts.first()).nth(1).innerText());
            assertEquals("Unavailable", UI.cells(attempts.last()).nth(1).innerText());
            assertEquals("FAILED", UI.cells(attempts.last()).nth(3).innerText());
            assertFalse(UI.executionSummary(disclosure).innerText().contains("Response duration"));
            attempts.last().scrollIntoViewIfNeeded(); assertTrue(attempts.last().isVisible());
            UI.summary(disclosure).focus(); page.keyboard().press("Enter");
            assertFalse((Boolean) disclosure.evaluate("node => node.open"));
            page.keyboard().press("Space"); assertTrue((Boolean) disclosure.evaluate("node => node.open"));
        }
    }

    @ParameterizedTest(name = "Pre-dispatch cancellation · fallback={0}")
    @ValueSource(booleans = {false, true})
    void noDispatchNeverClaimsAnExecutedNetworkRequest(boolean fallback) throws Exception {
        Path file = Files.createDirectories(Path.of("target", "terminal-execution-report", UUID.randomUUID().toString()))
                .resolve(fallback ? "cancelled-fallback.html" : "cancelled-extent.html");
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file);
        try (session) {
            var scenario = session.startScenario("CancelledExecutionFixture", "cancelled before dispatch", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            try (var request = phase.startRequest("Cancelled API execution", "GET", "/read")) {
                ApiExchangeEvidence.publishFailure(request, evidence(0), "https://fixture.test/read", "CANCELLED", "Cancelled before dispatch");
                request.complete(AtfReportStatus.SKIP);
            }
            phase.complete(AtfReportStatus.SKIP); scenario.complete(AtfReportStatus.SKIP);
        }
        String html = Files.readString(file);
        assertTrue(html.contains("No request dispatched; selected transport: LIVE_HTTP"));
        assertFalse(html.contains("LIVE / HTTP — network was executed"));
        assertTrue(html.contains("NOT VERIFIED"));
        assertFalse(java.util.regex.Pattern.compile("<table[^>]*class=[\"'][^\"']*retry-history-table")
                .matcher(html).find(), "zero dispatches must not render an attempt table");
    }
    private static ReportExecutionEvidence evidence(int attempts) {
        var request = ReportRequest.builder("GET", "/read").build();
        return new ReportExecutionEvidence(null, attempts, Duration.ofMillis(attempts == 0 ? 0 : 14),
                attempts == 0 ? List.of() : List.of(new ReportRetryEvent(2, 503, "", Duration.ofMillis(5), "configured status retry")),
                attempts == 0 ? List.of() : List.of(
                        new ReportAttemptRecord(1, 503, Duration.ofMillis(2), "RETRYABLE_RESPONSE", "configured status retry"),
                        new ReportAttemptRecord(2, -1, Duration.ofMillis(7), "FAILED", "execution stopped without an HTTP response")),
                request, ReportTransportProvenance.LIVE_HTTP);
    }
}
