package atf.ui.tests.extent.component.verdict;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.verdict.VerdictReportLocators;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static atf.reporting.contract.AtfReportStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Actual browser projections must agree with evaluated evidence and preserve disclosure access. */
@AtfComponentTest
@ReportBrowserTest
class ReportingVerdictProjectionComponentTest {
    private static final VerdictReportLocators UI = new VerdictReportLocators();

    @ParameterizedTest(name = "Contradictory completion · fallback={0}")
    @ValueSource(booleans = {false, true})
    void bothRenderersShowTheSameFailureAndKeepPositiveAndSkippedSiblings(boolean fallback) throws Exception {
        Path file = output(fallback ? "fallback.html" : "extent.html");
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file);
        try (session) {
            publish(session, "negative", comparisons(), PASS);
            publish(session, "positive sibling", List.of(new AtfAssertion("Sibling remains PASS", "ready", "ready", true)), PASS);
            publish(session, "skipped sibling", List.of(new AtfAssertion("Aborted comparison", "ready", "unavailable", false, SKIP)), SKIP);
        }
        verifyBrowser(file, fallback, 1, 1);
    }

    @ParameterizedTest(name = "UI facade buffered mode={0}")
    @ValueSource(strings = {"SCREEN_CHECKPOINTS", "UNIFIED_FLOW", "CROSS_PHASE"})
    void uiFacadeRetainsFailureEvenWhenTheCallerCompletesBufferedPhasesWithPass(String mode) throws Exception {
        Path file = output(mode.toLowerCase() + ".html");
        boolean crossPhase = mode.equals("CROSS_PHASE");
        var flow = mode.equals("SCREEN_CHECKPOINTS") ? ReportEvidenceFlowMode.SCREEN_CHECKPOINTS : ReportEvidenceFlowMode.UNIFIED_FLOW;
        var profile = new ReportEvidenceProfile(flow, ReportScreenshotPolicy.NONE, ReportEvidenceCaptureSource.CUSTOM_PROVIDER);
        var options = UiExtentReportOptions.builder(file.getParent()).fileName(file.getFileName().toString())
                .timestampEnabled(false).evidenceProfile(profile).build();
        try (var session = UiExtentReportSession.openConfigured(options)) {
            var scenario = session.startScenario("VerdictFixture", "standalone", "negative", List.of("component"));
            if (crossPhase) scenario.unifyAcrossPhases();
            var phase = scenario.startPhase(crossPhase ? "SETUP" : "TEST BODY");
            phase.step(AtfGherkinKeyword.THEN, "Independent result is validated", "local fixture", "", comparisons(), PASS);
            if (crossPhase) {
                phase.complete(PASS);
                var terminal = scenario.startPhase("TEST BODY");
                terminal.step(AtfGherkinKeyword.THEN, "Final observation", "local fixture", "", PASS);
                terminal.completeFinal(PASS);
            } else phase.completeFinal(PASS);
            scenario.complete(PASS);
            var recorded = session.semanticSnapshot().classes().getFirst().scenarios().getFirst();
            assertEquals(FAIL, recorded.status());
            assertEquals(FAIL, recorded.phases().getFirst().status());
        }
        verifyBrowser(file, false, 0, 1);
    }

    private static void verifyBrowser(Path file, boolean fallback, int passed, int failed) throws Exception {
        assertFalse(Files.readString(file).contains("bounded-prefix ".repeat(30) + "actual suffix"));
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                page.navigate(file.toAbsolutePath().toUri().toString());
                UI.overview(page, fallback).click();
                assertEquals(Integer.toString(passed), UI.metric(page, fallback, fallback ? "Passed" : "Tests Passed").innerText());
                assertEquals(Integer.toString(failed), UI.metric(page, fallback, fallback ? "Failed" : "Tests Failed").innerText());
                UI.tests(page, fallback).click();
                if (!fallback) UI.expandAll(page).click();
                var table = UI.comparisonTable(page, fallback);
                assertEquals(1, table.count(), "Inspect the canonical visible table, excluding source and navigation clones");
                assertEquals(42, UI.rows(table).count());
                assertTrue(table.isVisible());
                UI.rows(table).last().scrollIntoViewIfNeeded();
                assertTrue(UI.rows(table).last().isVisible());
                for (String value : UI.valueCells(table).allInnerTexts()) assertTrue(value.length() <= 240);
                UI.fails(page, fallback).click();
                assertEquals(1, UI.failures(page, fallback).count());
                if (!fallback) UI.failures(page, false).first().click();
                UI.openFailure(page, fallback).click();
                var scenario = UI.scenario(page, fallback, "negative");
                assertThat(scenario).isVisible();
                if (!fallback) {
                    var header = UI.scenarioHeader(scenario);
                    assertThat(header).hasAttribute("aria-expanded", "true");
                    header.press("Enter");
                    assertThat(header).hasAttribute("aria-expanded", "false");
                    header.press("Space");
                    assertThat(header).hasAttribute("aria-expanded", "true");
                    UI.expandAll(page).click();
                }
                assertThat(UI.comparisonTable(page, fallback)).isVisible();
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static List<AtfAssertion> comparisons() {
        String prefix = "bounded-prefix ".repeat(30);
        return IntStream.rangeClosed(1, 42).mapToObj(index -> index == 1
                ? new AtfAssertion("Independent comparison", prefix + "expected suffix", prefix + "actual suffix", false)
                : new AtfAssertion("Audit comparison " + index, "expected", "expected", true)).toList();
    }

    private static void publish(AtfReportPorts.Session session, String name, List<AtfAssertion> rows, AtfReportStatus status) {
        var scenario = session.startScenario("VerdictFixture", "standalone", name, List.of("component"));
        try (var phase = scenario.startPhase("TEST BODY");
             var request = phase.startRequest("Existing checkpoint", "TRACE", "local");
             var validation = request.startValidation("UI validation")) {
            validation.assertions(rows); validation.complete(status); request.complete(status); phase.complete(status);
        }
        scenario.complete(status);
    }

    private static Path output(String name) throws Exception {
        return Files.createDirectories(Path.of("target", "verdict-consistency", UUID.randomUUID().toString())).resolve(name);
    }
}
