package atf.ui.tests.extent.component.timing;

import atf.reporting.audit.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.diagnostic.*;
import atf.ui.tests.extent.support.locators.timing.ReportPhaseTimingLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

/** Empty measured lifecycle phases and runtime errors after successful checks remain auditable. */
@AtfComponentTest
class ComponentExecutionDiagnosticReportTest {
    private static final ReportPhaseAuditLocators PHASES = new ReportPhaseAuditLocators();
    private static final ReportPhaseTimingLocators TIMING = new ReportPhaseTimingLocators();
    private static final ComponentDiagnosticLocators DIAGNOSTICS = new ComponentDiagnosticLocators();

    @Test void runtimeErrorsDoNotBlamePassingComparisonsOrInventHttpFields(@TempDir Path output) throws Exception {
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            for (boolean fallback : List.of(false, true)) try (var page = browser.newPage()) {
                Path report = publish(output.resolve(fallback ? "fallback.html" : "extent.html"), fallback);
                page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
                if (!fallback) {
                    PHASES.firstOwner(page).click();
                    page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                }
                PHASES.failsTab(page, fallback).click();
                if (fallback) {
                    assertEquals(2, DIAGNOSTICS.fallbackFailures(page).count());
                    assertTrue(PHASES.fails(page, true).innerText().contains("controlled runtime error"));
                    assertFalse(PHASES.fails(page, true).innerText().contains("previous passing check"));
                    DIAGNOSTICS.fallbackFailures(page).filter(new Locator.FilterOptions().setHasText("controlled runtime error")).click();
                    assertTrue(PHASES.scenarios(page, true).first().isVisible());
                } else {
                    DIAGNOSTICS.nativeFailures(page).filter(new Locator.FilterOptions().setHasText("runtime error")).click();
                    assertEquals("component", DIAGNOSTICS.failureIndex(page).getAttribute("data-atf-evidence-owner"));
                    assertFalse(DIAGNOSTICS.labels(page).allTextContents().contains("REQUEST"));
                    assertFalse(DIAGNOSTICS.labels(page).allTextContents().contains("DURATION"));
                    assertFalse(DIAGNOSTICS.failureIndex(page).innerText().contains("previous passing check"));
                    assertTrue(DIAGNOSTICS.failureIndex(page).innerText().contains("controlled runtime error"));
                    PHASES.openFailureInTests(page).click();
                    var scenario = PHASES.scenarios(page, false).first();
                    assertEquals("357", scenario.getAttribute("data-duration-ms"));
                    assertEquals("345", TIMING.badge(PHASES.phases(scenario).nth(1), false).getAttribute("data-duration-ms"));
                    PHASES.failsTab(page, false).click();
                    DIAGNOSTICS.nativeFailures(page).filter(new Locator.FilterOptions().setHasText("error before checks")).click();
                    assertEquals("execution", DIAGNOSTICS.failureIndex(page).getAttribute("data-atf-evidence-owner"));
                    assertFalse(DIAGNOSTICS.labels(page).allTextContents().contains("REQUEST"));
                    assertFalse(DIAGNOSTICS.labels(page).allTextContents().contains("DURATION"));
                }
            }
        }
    }

    @Test void emptyMeasuredPhasesStayNonExpandableAndAllBoundedChecksRemainReachable(@TempDir Path output) throws Exception {
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            for (boolean fallback : List.of(false, true)) try (var page = browser.newPage()) {
                Path report = publish(output.resolve(fallback ? "fallback.html" : "extent.html"), fallback);
                page.navigate(report.toUri() + (fallback ? "" : "#test-view"));
                if (fallback) PHASES.testsTab(page, true).click();
                else {
                    PHASES.firstOwner(page).click();
                    page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                }
                var scenario = PHASES.scenarios(page, fallback).first();
                if (!fallback) PHASES.header(scenario).click();
                var phases = PHASES.phases(scenario);
                assertEquals(3, phases.count());
                for (int index : List.of(0, 2)) {
                    assertEquals(index == 0 ? "12" : "0", TIMING.badge(phases.nth(index), fallback).getAttribute("data-duration-ms"));
                    assertEquals(0, PHASES.assertionStep(phases.nth(index)).count());
                    if (!fallback) {
                        assertEquals("false", phases.nth(index).getAttribute("data-atf-node-expandable"));
                        assertNull(PHASES.header(phases.nth(index)).getAttribute("aria-expanded"));
                    }
                }
                var body = phases.nth(1);
                if (!fallback) PHASES.header(body).click();
                var step = PHASES.assertionStep(body);
                var header = fallback ? PHASES.diagnosticSummary(step) : PHASES.header(step);
                assertFalse(PHASES.assertionTable(step, fallback).isVisible());
                header.press("Enter");
                var table = PHASES.assertionTable(step, fallback);
                assertTrue(table.isVisible());
                assertEquals(42, PHASES.assertionRows(table).count());
                assertTrue(DIAGNOSTICS.assertionValues(table).allTextContents().stream().allMatch(value -> value.length() <= 240));
                PHASES.assertionRows(table).last().scrollIntoViewIfNeeded();
                assertTrue(PHASES.assertionRows(table).last().isVisible());
                header.press("Space"); assertFalse(table.isVisible());
                header.press("Enter"); assertTrue(table.isVisible());
                assertEquals("345", TIMING.badge(body, fallback).getAttribute("data-duration-ms"));
            }
        }
    }
    private static Path publish(Path path, boolean fallback) {
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(path) : new ExtentReportSession(path);
        try (var run = new AssertionAuditRun(session, new SensitiveDataRedactor())) {
            try (var report = run.open(new AssertionAuditReport.Identity("RuntimeCase", "fixture.component", "runtime error", "body", List.of("component")))) {
                report.phase("SETUP", List.of(), AtfReportStatus.PASS, "", timing(12));
                String bounded = "safe-value-".repeat(30).substring(0, 239) + "…";
                var checks = IntStream.rangeClosed(1, 42).mapToObj(index -> new AtfAssertion(
                        index == 1 ? "previous passing check" : "check " + index,
                        index == 1 ? "expected ❌ indicator" : bounded,
                        index == 1 ? "expected ❌ indicator" : bounded, true)).toList();
                report.phase("TEST BODY", checks, AtfReportStatus.FAIL, "controlled runtime error", timing(345));
                report.phase("TEARDOWN", List.of(), AtfReportStatus.PASS, "", timing(0));
                report.complete(AtfReportStatus.FAIL);
            }
            try (var report = run.open(new AssertionAuditReport.Identity("EmptyFailure", "fixture.component", "error before checks", "setup", List.of("component")))) {
                report.phase("SETUP", List.of(), AtfReportStatus.FAIL, "failure before checks", timing(25));
                report.complete(AtfReportStatus.FAIL);
            }
            try (var report = run.open(new AssertionAuditReport.Identity("AbortedCase", "fixture.component", "aborted", "body", List.of("component")))) {
                report.phase("TEST BODY", List.of(), AtfReportStatus.SKIP, "controlled assumption", timing(40));
                report.complete(AtfReportStatus.SKIP);
            }
        }
        return path;
    }
    private static ReportPhaseTiming timing(long millis) { return new ReportPhaseTiming(Duration.ofMillis(millis), 1); }
}
