package atf.ui.tests.extent.support.inspection.execution;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.execution.UiTerminalExchangeFixture;
import atf.ui.tests.extent.support.locators.execution.TerminalExecutionLocators;
import atf.ui.tests.extent.support.locators.execution.UiTerminalExchangeLocators;
import atf.ui.tests.extent.support.locators.parity.ApiExchangeParityLocators;
import atf.ui.tests.extent.support.locators.verdict.VerdictReportLocators;
import com.microsoft.playwright.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static atf.reporting.contract.AtfReportStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real published DOM and semantic oracles; no calls to private projection helpers. */
public final class UiTerminalExchangeProbe {
    private static final TerminalExecutionLocators EXECUTION = new TerminalExecutionLocators();
    private static final UiTerminalExchangeLocators NESTED = new UiTerminalExchangeLocators();
    private static final VerdictReportLocators VIEWS = new VerdictReportLocators();
    private static final ApiExchangeParityLocators OWNERS = new ApiExchangeParityLocators();

    private UiTerminalExchangeProbe() { }

    public static void verify(boolean fallback, int limit) throws Exception {
        var artifact = UiTerminalExchangeFixture.publish(fallback, limit);
        var scenarios = artifact.snapshot().classes().getFirst().scenarios();
        assertEquals(artifact.cases().size(), scenarios.size());
        for (int i = 0; i < scenarios.size(); i++) verifySnapshot(scenarios.get(i), artifact.cases().get(i).expected());
        var html = Files.readString(artifact.path());
        assertFalse(html.contains(UiTerminalExchangeFixture.SECRET));
        assertFalse(html.contains("HTTP 0"));
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                var errors = new ArrayList<String>();
                page.onPageError(errors::add);
                page.setViewportSize(1280, 900);
                page.navigate(artifact.path().toUri().toString());
                VIEWS.tests(page, fallback).click();
                if (!fallback) { OWNERS.owners(page).first().click(); VIEWS.expandAll(page).click(); }
                for (var item : artifact.cases()) {
                    var scenario = VIEWS.scenario(page, fallback, item.name());
                    var exchanges = NESTED.exchanges(scenario, fallback);
                    assertEquals(3, exchanges.count(), item.name() + ": no exchange may disappear or renumber its siblings");
                    var terminal = exchanges.nth(1);
                    var requestDetails = NESTED.requestDetails(terminal);
                    assertEquals(1, requestDetails.count());
                    EXECUTION.summary(requestDetails).press("Enter");
                    assertEquals(0, NESTED.responses(terminal).count(), "no captured response means no Response details");
                    assertEquals(1, NESTED.responses(exchanges.first()).count());
                    assertEquals(1, NESTED.responses(exchanges.last()).count());
                    var execution = NESTED.execution(terminal);
                    assertEquals(1, execution.count()); assertTrue(execution.isVisible());
                    assertFalse((Boolean) execution.evaluate("element => element.open"));
                    var summary = EXECUTION.summary(execution);
                    summary.press("Enter");
                    assertTrue((Boolean) execution.evaluate("element => element.open"));
                    assertTrue(EXECUTION.executionSummary(execution).innerText().contains("No HTTP response received"));
                    assertFalse(EXECUTION.executionSummary(execution).innerText().contains("Response duration"));
                    var rows = NESTED.summaryRows(execution);
                    if (!item.outcome().isEmpty()) {
                        assertEquals(item.outcome(), NESTED.cell(rows.filter(new Locator.FilterOptions().setHasText("Execution termination"))).innerText());
                        String diagnostic = NESTED.cell(rows.filter(new Locator.FilterOptions().setHasText("Execution failure"))).innerText();
                        assertEquals(limit, diagnostic.length()); assertTrue(diagnostic.endsWith("…"));
                    }
                    var attempts = EXECUTION.attempts(execution);
                    assertEquals(item.attempts(), attempts.count());
                    if (item.attempts() > 0) {
                        assertEquals("503", EXECUTION.cells(attempts.first()).nth(1).innerText());
                        assertEquals("Unavailable", EXECUTION.cells(attempts.last()).nth(1).innerText());
                        attempts.last().scrollIntoViewIfNeeded(); assertTrue(attempts.last().isVisible());
                    } else {
                        assertTrue(terminal.innerText().contains("No request dispatched"));
                        assertFalse(terminal.innerText().contains("network was executed"));
                    }
                    assertFalse(terminal.innerText().contains("curl --request"), "unknown resolved URL must not become a replay command");
                    summary.press("Space"); assertFalse((Boolean) execution.evaluate("element => element.open"));
                }
                VIEWS.overview(page, fallback).click();
                assertEquals("7", VIEWS.metric(page, fallback, fallback ? "Failed" : "Tests Failed").innerText());
                assertEquals("2", VIEWS.metric(page, fallback, fallback ? "Passed" : "Tests Passed").innerText(),
                        "Overview counts PASS + WARNING without converting the warning verdict");
                assertTrue(errors.isEmpty(), errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static void verifySnapshot(AtfReportSemanticSnapshot.ScenarioSnapshot scenario, AtfReportStatus expected) {
        assertEquals(expected, scenario.status(), scenario.name() + ": caller PASS cannot overwrite nested evidence");
        var validations = scenario.phases().stream().flatMap(phase -> phase.nodes().stream())
                .filter(AtfReportSemanticSnapshot.RequestSnapshot.class::isInstance)
                .map(AtfReportSemanticSnapshot.RequestSnapshot.class::cast)
                .flatMap(request -> request.validations().stream())
                .filter(validation -> validation.evidenceOwner() == ReportEvidenceOwner.API).toList();
        assertEquals(1, validations.size(), scenario.name());
        assertEquals(expected, validations.getFirst().status());
        var exchanges = validations.getFirst().requests();
        assertEquals(List.of("/before", "/terminal", "/after"), exchanges.stream().map(AtfReportSemanticSnapshot.RequestSnapshot::path).toList());
        assertEquals(List.of(PASS, expected, PASS), exchanges.stream().map(AtfReportSemanticSnapshot.RequestSnapshot::status).toList());
        assertTrue(exchanges.get(1).executionPresent(), "the partial execution snapshot remains attached to its exchange");
    }
}
