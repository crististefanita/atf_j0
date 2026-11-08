package atf.ui.tests.extent.component.lifecycle;

import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.core.lifecycle.ApiScenarioPlan;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.browser.junit.ReportBrowserTest;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.CoreReportPortsAdapter;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the real Core lifecycle through both renderers and inspects failure disclosure behavior. */
@AtfComponentTest
@ReportBrowserTest
class ApiScenarioLifecycleReportComponentTest {
    private static final ReportPhaseAuditLocators REPORT = new ReportPhaseAuditLocators();
    private static final ReportDisclosureLocators DISCLOSURES = new ReportDisclosureLocators();
    private static final String SCENARIO = "controlled lifecycle failure";
    private static final String CAUSE = "controlled API contract mismatch";

    @Test
    void extentAndFallbackKeepTheFailureAuditableAndCollapsedUntilOpened(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path output)
            throws Exception {
        Path extent = publish(output.resolve("extent.html"), false);
        Path fallback = publish(output.resolve("fallback.html"), true);
        for (Path report : List.of(extent, fallback)) {
            assertTrue(Files.isRegularFile(report));
            String html = Files.readString(report);
            assertTrue(html.contains(SCENARIO));
            assertTrue(html.contains(CAUSE));
        }

        try (var playwrightLease = BrowserResources.open(Playwright::create)) {
            try {
                var browser = BrowserResources.launch(playwrightLease.value(),
                        new BrowserType.LaunchOptions().setHeadless(true));
                for (boolean plain : List.of(false, true)) {
                    Path report = plain ? fallback : extent;
                    try (var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browser))) {
                        try {
                            var page = pageLease.value();
                            page.setDefaultTimeout(5_000);
                            page.setDefaultNavigationTimeout(30_000);
                            page.navigate(report.toAbsolutePath().toUri().toString()
                                    + (plain ? "" : "#test-view"));
                            if (plain) {
                                REPORT.testsTab(page, true).click();
                            } else {
                                REPORT.firstOwner(page).click();
                                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                            }
        
                            var scenario = REPORT.scenarios(page, plain).first();
                            assertTrue(scenario.isVisible());
                            assertTrue(scenario.innerText().contains(SCENARIO));
                            assertTrue(scenario.innerText().contains("Fail"));
                            if (!plain) {
                                var scenarioHeader = REPORT.header(scenario);
                                assertEquals("false", scenarioHeader.getAttribute("aria-expanded"));
                                scenarioHeader.click();
                                assertEquals("true", scenarioHeader.getAttribute("aria-expanded"));
                            }
                            var phases = REPORT.phases(scenario);
                            assertEquals(1, phases.count());
                            var phase = phases.first();
                            assertTrue(phase.isVisible());
                            if (!plain) {
                                var phaseHeader = REPORT.header(phase);
                                assertEquals("false", phaseHeader.getAttribute("aria-expanded"));
                                phaseHeader.click();
                                assertEquals("true", phaseHeader.getAttribute("aria-expanded"));
                            }
        
                            var request = REPORT.requests(phase, plain).first();
                            var validations = plain
                                    ? request.locator(":scope > [data-atf-node-role='validation']")
                                    : DISCLOSURES.validations(request);
                            assertEquals(1, validations.count(), "one validation node; fallback=" + plain);
                            var validation = validations.first();
                            if (!plain) {
                                var requestHeader = REPORT.header(request);
                                assertEquals("false", requestHeader.getAttribute("aria-expanded"));
                                requestHeader.click();
                                assertEquals("true", requestHeader.getAttribute("aria-expanded"));
                                var validationHeader = REPORT.header(validation);
                                assertEquals("false", validationHeader.getAttribute("aria-expanded"));
                                validationHeader.click();
                                assertEquals("true", validationHeader.getAttribute("aria-expanded"));
                            }
                            var assertionDisclosure = validation.locator("details[class~='assertion-evidence']");
                            var assertionTable = validation.locator(plain
                                    ? "table[class~='assertions']"
                                    : "table[class~='helm-validation-table']").first();
                            assertEquals(1, assertionDisclosure.count(),
                                    "renderer keeps assertion evidence in one disclosure; fallback=" + plain);
                            assertEquals(Boolean.FALSE, assertionDisclosure.evaluate("node => node.open"));
                            assertionDisclosure.locator(":scope > summary").click();
                            assertEquals(Boolean.TRUE, assertionDisclosure.evaluate("node => node.open"));
                            assertEquals(1, assertionTable.count(), "canonical assertion table; fallback=" + plain);
                            assertTrue(assertionTable.isVisible());
                            assertEquals(1, assertionTable.locator("tbody tr").count());
                            assertTrue(assertionTable.innerText().length() <= 240,
                                    "the controlled assertion stays within the report preview budget");
        
                            var diagnostics = REPORT.diagnostics(scenario);
                            assertEquals(1, diagnostics.count());
                            var disclosure = diagnostics.first();
                            assertEquals(Boolean.FALSE, disclosure.evaluate("node => node.open"));
                            var summary = REPORT.diagnosticSummary(disclosure);
                            assertTrue(summary.isVisible());
                            summary.click();
                            assertEquals(Boolean.TRUE, disclosure.evaluate("node => node.open"));
                            var diagnosticTable = disclosure.locator("table");
                            assertTrue(diagnosticTable.isVisible());
                            assertTrue(diagnosticTable.innerText().contains(CAUSE));
                            assertTrue(diagnosticTable.innerText().length() <= 240,
                                    "the concise failure cause stays within the report preview budget");
        
                            REPORT.failsTab(page, plain).click();
                            if (plain) {
                                assertTrue(REPORT.fails(page, true).innerText().contains(CAUSE));
                            } else {
                                var failureRows = page.locator("#atf-failure-list > .test-item");
                                assertEquals(2, failureRows.count(), "assertion plus one diagnostic-chain entry");
                                assertEquals("assertion", failureRows.nth(0).getAttribute("data-atf-failure-kind"));
                                assertEquals("diagnostic", failureRows.nth(1).getAttribute("data-atf-failure-kind"));
                                assertEquals("ApiLifecycleFixture · Assertion",
                                        failureRows.nth(0).locator(".text-sm").innerText());
                                assertEquals("ApiLifecycleFixture · Execution diagnostic",
                                        failureRows.nth(1).locator(".text-sm").innerText());
                                var detail = page.locator("#atf-failure-detail");
                                failureRows.nth(0).click();
                                assertFalse(detail.innerText().contains(CAUSE),
                                        "assertion details must not repeat a terminal cause indexed separately");
                                failureRows.nth(1).click();
                                String diagnostic = detail.innerText();
                                assertTrue(diagnostic.contains("PRIMARY: Failure cause: " + CAUSE));
                                assertEquals(1, occurrences(diagnostic, CAUSE),
                                        "the terminal cause appears once in the aggregated diagnostic entry");
                            }
                                } catch (Exception | Error reportingBrowserFailure) {
                            BrowserResources.recordFailure(reportingBrowserFailure);
                            throw reportingBrowserFailure;
                        }
                    }
                }
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    private static Path publish(Path path, boolean fallback) {
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(path) : new ExtentReportSession(path);
        ApiScenarioLifecycleRunner runner = new ApiScenarioLifecycleRunner();
        try (session) {
            var outcome = runner.run(CoreReportPortsAdapter.wrap(session),
                    ApiScenarioPlan.scenario("ApiLifecycleFixture", SCENARIO)
                            .body(phase -> {
                                try (var request = phase.startRequest("Controlled API evidence", "GET", "/fixture");
                                     var validation = request.startValidation("Contract assertion")) {
                                    validation.assertions(List.of(new AtfAssertion(
                                            "Response is accepted", "accepted", "rejected", false)));
                                    validation.complete(AtfReportStatus.FAIL);
                                    request.complete(AtfReportStatus.FAIL);
                                }
                                throw new ApiScenarioLifecycleRunner.FailureSignal(CAUSE);
                            })
                            .build());
            assertEquals(atf.core.reporting.contract.AtfReportStatus.FAIL, outcome.status());
            assertEquals(CAUSE, outcome.primaryFailure().getMessage());
            assertEquals(atf.core.reporting.contract.AtfReportStatus.FAIL,
                    outcome.phases().get(ApiScenarioLifecycleRunner.Phase.TEST_BODY));
        }
        return path;
    }

    private static int occurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
