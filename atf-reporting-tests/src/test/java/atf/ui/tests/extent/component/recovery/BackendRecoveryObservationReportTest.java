package atf.ui.tests.extent.component.recovery;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.contract.*;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.*;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import atf.ui.tests.extent.support.locators.failurecontext.ReportFailureContextLocators;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Renderer contract for neutral INFO observations; no backend policy or application execution lives here. */
@AtfComponentTest
@ReportBrowserTest
class BackendRecoveryObservationReportTest {
    private static final ReportPhaseAuditLocators PHASES = new ReportPhaseAuditLocators();
    private static final ReportFailureContextLocators EVIDENCE = new ReportFailureContextLocators();
    private static final ReportDisclosureLocators TREE = new ReportDisclosureLocators();

    @Test void infoDecisionsKeepOneLogicalOutcomeAndBoundedCollapsibleEvidence() throws Exception {
        Path directory = Files.createDirectories(Path.of("target", "backend-recovery-report", UUID.randomUUID().toString()));
        Path file;
        try (var report = UiExtentReportSession.openConfigured(directory, "backend-observations.html",
                AtfConsoleSink.noOp(), ReportScreenshotPolicy.NONE)) {
            file = report.reportPath();
            for (boolean recovered : List.of(true, false)) {
                var scenario = report.startScenario("BackendObservationFixture", getClass().getPackageName(),
                        recovered ? "Recovered catalog" : "Exhausted catalog", List.of("component", "reporting"));
                var phase = scenario.startPhase("TEST BODY");
                String outcome = recovered ? "RECOVERED" : "EXHAUSTED";
                var rows = new ArrayList<AtfUiActionRecord>();
                for (int index = 1; index <= 12; index++) {
                    rows.add(new AtfUiActionRecord(index, "BE observation #" + index, "catalog-readiness",
                            "profile=read-only; maxAttempts=12; totalTimeout=PT5S; source=YAML",
                            (index == 12 ? outcome : "REOBSERVE") + "; observation only; no business action replay; "
                                    + "bounded-safe-metadata ".repeat(30), 0, AtfReportStatus.INFO, AtfUiEvidenceSource.OBSERVE));
                }
                phase.step(AtfGherkinKeyword.THEN, "Catalog decision", "catalog-readiness", null,
                        List.of(new AtfAssertion("Catalog business readiness", "READY",
                                recovered ? "READY" : "PENDING", recovered)), rows,
                        recovered ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                phase.complete(recovered ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                scenario.complete(recovered ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            }
            assertEquals(2, report.semanticSnapshot().scenarioCount());
            assertEquals(List.of(AtfReportStatus.PASS, AtfReportStatus.FAIL),
                    report.semanticSnapshot().classes().getFirst().scenarios().stream().map(value -> value.status()).toList());
        }
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                page.navigate(file.toAbsolutePath().toUri() + "#test-view");
                PHASES.firstOwner(page).click();
                var scenario = PHASES.scenarios(page, false).first();
                PHASES.header(scenario).click();
                var phase = PHASES.phases(scenario).first();
                PHASES.header(phase).click();
                var request = TREE.requests(phase).first();
                TREE.header(request).click();
                TREE.header(TREE.validations(request).first()).click();
                // Native hierarchy is open, while the evidence disclosure is initially collapsed.
                var observation = EVIDENCE.observations(page).first();
                assertFalse((Boolean) observation.evaluate("node => node.open"));
                EVIDENCE.summary(observation).click();
                var rows = EVIDENCE.observationRows(page);
                assertEquals(12, rows.count());
                assertTrue(rows.last().innerText().contains("RECOVERED"));
                assertTrue(rows.last().innerText().contains("ℹ"));
                for (var row : rows.all()) {
                    for (String cell : EVIDENCE.cells(row).allTextContents()) assertTrue(cell.trim().length() <= 240, cell);
                }
                rows.last().scrollIntoViewIfNeeded(); assertTrue(rows.last().isVisible());
                EVIDENCE.summary(observation).focus(); page.keyboard().press("Enter");
                assertFalse((Boolean) observation.evaluate("node => node.open"));
                page.keyboard().press("Space"); assertTrue((Boolean) observation.evaluate("node => node.open"));
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}
