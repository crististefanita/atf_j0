package atf.ui.tests.extent.component.timing;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.testkit.AtfComponentTest;
import atf.reporting.audit.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import atf.ui.tests.extent.support.locators.timing.ReportPhaseTimingLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.*;

/** Numeric expectations are independent of publication speed, native timestamps and browser formatting. */
@AtfComponentTest
@ReportBrowserTest
class ReportPhaseTimingComponentTest {
    private static final ReportPhaseAuditLocators PHASES = new ReportPhaseAuditLocators();
    private static final ReportPhaseTimingLocators TIMING = new ReportPhaseTimingLocators();
    private record Published(Path path, AtfReportSemanticSnapshot snapshot) { }

    @Test void nativeAndFallbackSnapshotsKeepTheSameTypedPhaseTiming(@TempDir Path directory) throws Exception {
        var extent = publish(directory.resolve("extent.html"), false);
        var fallback = publish(directory.resolve("fallback.html"), true);
        assertEquals(extent.snapshot(), fallback.snapshot());
        for (var type : extent.snapshot().classes()) for (var scenario : type.scenarios()) {
            assertEquals(List.of(12L, 345L, 0L), scenario.phases().stream()
                    .map(phase -> phase.timing().orElseThrow().durationMillis()).toList());
            assertEquals(List.of(2, 1, 1), scenario.phases().stream()
                    .map(phase -> phase.timing().orElseThrow().intervals()).toList());
        }
        assertEquals(0, extent.snapshot().requestCount());
        assertEquals(0, extent.snapshot().validationCount());
    }

    @Test void browserUsesExecutionInsteadOfSnapshotPublicationAndDoesNotCountItTwice(@TempDir Path directory) throws Exception {
        var nativeReport = publish(directory.resolve("extent.html"), false);
        var fallback = publish(directory.resolve("fallback.html"), true);
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)))) {
            try {
                var browser = browserLease.value();
                for (boolean plain : List.of(false, true)) try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
                try {
                    var page = pageLease.value();
                        page.navigate((plain ? fallback : nativeReport).path().toUri() + (plain ? "" : "#test-view"));
                        if (plain) PHASES.testsTab(page, true).click();
                        else {
                            PHASES.firstOwner(page).click();
                            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                        }
                        var scenario = PHASES.scenarios(page, plain).first();
                        if (!plain) PHASES.header(scenario).click();
                        var phases = PHASES.phases(scenario);
                        assertEquals(3, phases.count());
                        for (int index = 0; index < 3; index++) {
                            var phase = phases.nth(index);
                            if (!plain) PHASES.header(phase).click();
                            String expected = List.of("12", "345", "0").get(index);
                            assertEquals(expected, TIMING.metadata(phase).getAttribute(EXECUTION_DURATION.value()));
                            assertEquals(expected, TIMING.badge(phase, plain).getAttribute(DURATION.value()));
                            assertEquals("execution", TIMING.badge(phase, plain).getAttribute(DURATION_SOURCE.value()));
                            assertEquals(index == 0 ? "2" : "1", TIMING.metadata(phase).getAttribute(EXECUTION_INTERVALS.value()));
                            assertTrue(Long.parseLong(TIMING.metadata(phase).getAttribute(PUBLICATION_DURATION.value())) >= 0);
                            var step = PHASES.assertionStep(phase);
                            if (plain) PHASES.diagnosticSummary(step).click(); else PHASES.header(step).click();
                            assertTrue(PHASES.assertionTable(step, plain).isVisible());
                            assertEquals(1, PHASES.assertionRows(PHASES.assertionTable(step, plain)).count());
                            if (!plain) {
                                assertEquals("publication", TIMING.stepBadge(step).getAttribute(DURATION_SOURCE.value()));
                                PHASES.header(step).press("Enter"); PHASES.header(step).press("Space");
                            }
                            assertEquals(expected, TIMING.badge(phase, plain).getAttribute(DURATION.value()), "reopening must not replace execution timing");
                        }
                        if (plain) {
                            TIMING.overviewTab(page).click();
                            assertEquals("1071", TIMING.fallbackOverviewDuration(page).getAttribute(DURATION.value()));
                        } else {
                            assertEquals("714", TIMING.classes(page).nth(0).getAttribute(DURATION.value()));
                            assertEquals("357", TIMING.classes(page).nth(1).getAttribute(DURATION.value()));
                            assertTrue(TIMING.timeline(page).getAttribute("aria-label").contains("1.071"), "Overview uses the same class execution totals");
                        }
            
                } catch (Exception | Error reportingBrowserFailure) {
                    BrowserResources.recordFailure(reportingBrowserFailure);
                    throw reportingBrowserFailure;
                }
            }
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static Published publish(Path output, boolean fallback) {
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(output) : new ExtentReportSession(output);
        try (var run = new AssertionAuditRun(session, new SensitiveDataRedactor())) {
            for (String type : List.of("First", "First", "Second"))
                try (var report = run.open(new AssertionAuditReport.Identity(type, "fixture.timing", "same case", "body", List.of("component")))) {
                    int index = 0;
                    for (String phase : List.of("SETUP", "TEST BODY", "TEARDOWN")) {
                        long millis = List.of(12L, 345L, 0L).get(index);
                        report.phase(phase, List.of(new AtfAssertion("independent comparison", "ready", "ready", true)),
                                AtfReportStatus.PASS, "", new ReportPhaseTiming(Duration.ofMillis(millis), index++ == 0 ? 2 : 1));
                    }
                    report.complete(AtfReportStatus.PASS);
                }
        }
        return new Published(output, fallback ? ((HtmlReportSession) session).semanticSnapshot()
                : ((ExtentReportSession) session).semanticSnapshot());
    }
}
