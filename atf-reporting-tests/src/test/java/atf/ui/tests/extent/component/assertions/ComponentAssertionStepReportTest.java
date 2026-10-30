package atf.ui.tests.extent.component.assertions;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A component assertion step has one semantic owner in Extent and fallback HTML. */
@AtfComponentTest
class ComponentAssertionStepReportTest {
    private static final ReportPhaseAuditLocators PHASES = new ReportPhaseAuditLocators();
    @Test
    void publishesTheSameCollapsibleAssertionEvidenceWithoutChangingApiValidation() throws Exception {
        Path directory = Files.createTempDirectory("atf-component-assertions-");
        Path extentPath = directory.resolve("extent.html");
        Path fallbackPath = directory.resolve("fallback.html");
        AtfReportSemanticSnapshot extentSnapshot;
        AtfReportSemanticSnapshot fallbackSnapshot;
        try (var extent = new ExtentReportSession(extentPath)) {
            publish(extent);
            extentSnapshot = extent.semanticSnapshot();
        }
        try (var fallback = new HtmlReportSession(fallbackPath)) {
            publish(fallback);
            fallbackSnapshot = fallback.semanticSnapshot();
        }

        assertEquals(extentSnapshot, fallbackSnapshot);
        var step = assertInstanceOf(AtfReportSemanticSnapshot.AssertionStepSnapshot.class,
                extentSnapshot.classes().getFirst().scenarios().getFirst()
                        .phases().getFirst().nodes().getFirst());
        assertEquals(42, step.assertionNames().size());
        assertEquals("Configured product", step.assertionNames().getFirst());
        assertEquals("Check 42", step.assertionNames().getLast());
        assertEquals(AtfReportStatus.PASS, step.status());
        assertEquals(0, extentSnapshot.requestCount());
        assertEquals(0, extentSnapshot.validationCount());

        String extentHtml = Files.readString(extentPath);
        String fallbackHtml = Files.readString(fallbackPath);
        assertTrue(extentHtml.contains("Component assertions"));
        assertTrue(extentHtml.contains("Configured product"));
        assertTrue(fallbackHtml.contains("data-atf-node-role=\"assertion-step\""));
        assertTrue(fallbackHtml.contains("<details class=\"step-detail assertion-evidence\""));
        assertFalse(fallbackHtml.contains("<details class=\"step-detail assertion-evidence\" open"));
        assertTrue(fallbackHtml.contains("Configured product"));
    }

    @Test
    void opensTheAssertionStepAndRowsInBothRenderedReports() throws Exception {
        Path directory = Files.createTempDirectory("atf-component-assertion-browser-");
        Path extentPath = directory.resolve("extent.html");
        Path fallbackPath = directory.resolve("fallback.html");
        try (var extent = new ExtentReportSession(extentPath)) { publish(extent); }
        try (var fallback = new HtmlReportSession(fallbackPath)) { publish(fallback); }

        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            try (var page = browser.newPage()) {
                page.navigate(extentPath.toUri() + "#test-view");
                PHASES.firstOwner(page).click();
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
                var scenario = PHASES.scenarios(page, false).first();
                PHASES.header(scenario).click();
                var phase = PHASES.phases(scenario).first();
                PHASES.header(phase).click();
                var step = PHASES.assertionStep(phase);
                assertEquals(1, step.count());
                assertEquals("false", step.getAttribute("data-atf-node-open"));
                PHASES.header(step).click();
                assertEquals("true", step.getAttribute("data-atf-node-open"));
                var rows = PHASES.assertionRows(PHASES.assertionTable(step, false));
                assertTrue(PHASES.assertionTable(step, false).isVisible());
                assertEquals(42, rows.count());
                rows.last().scrollIntoViewIfNeeded();
                assertTrue(rows.last().isVisible());
                PHASES.header(step).focus();
                page.keyboard().press("Enter");
                assertEquals("false", step.getAttribute("data-atf-node-open"));
                page.keyboard().press("Space");
                assertEquals("true", step.getAttribute("data-atf-node-open"));
            }
            try (var page = browser.newPage()) {
                page.navigate(fallbackPath.toUri().toString());
                PHASES.testsTab(page, true).click();
                var step = PHASES.fallbackAssertionStep(page);
                assertEquals(1, step.count());
                assertFalse((Boolean) step.evaluate("element => element.open"));
                PHASES.diagnosticSummary(step).click();
                assertTrue((Boolean) step.evaluate("element => element.open"));
                var rows = PHASES.assertionRows(PHASES.assertionTable(step, true));
                assertTrue(PHASES.assertionTable(step, true).isVisible());
                assertEquals(42, rows.count());
                rows.last().scrollIntoViewIfNeeded();
                assertTrue(rows.last().isVisible());
            }
        }
    }

    @Test
    void failedAssertionIsVisibleInFailsWithoutAnApiRequest() throws Exception {
        Path report = Files.createTempDirectory("atf-component-assertion-failure-")
                .resolve("fallback.html");
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("ConfigurationTest", "invalid profile", List.of("component"));
            var body = scenario.startPhase("TEST BODY");
            var step = body.startAssertionStep("Component assertions");
            step.assertions(List.of(new AtfAssertion("Configured quantity", "2", "3", false)));
            step.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);

            var snapshot = session.semanticSnapshot();
            var published = assertInstanceOf(AtfReportSemanticSnapshot.AssertionStepSnapshot.class,
                    snapshot.classes().getFirst().scenarios().getFirst()
                            .phases().getFirst().nodes().getFirst());
            assertEquals(AtfReportStatus.FAIL, published.status());
            assertEquals(AtfReportStatus.FAIL,
                    snapshot.classes().getFirst().scenarios().getFirst().phases().getFirst().status());
            assertEquals(AtfReportStatus.FAIL,
                    snapshot.classes().getFirst().scenarios().getFirst().status());
            assertEquals(0, snapshot.requestCount());
            assertEquals(1, session.executionModel().failures().items().size());
            assertEquals("Configured quantity",
                    session.executionModel().failures().items().getFirst().assertionName());
        }
        assertTrue(Files.readString(report).contains("Configured quantity"));
    }

    @Test
    void failedComponentAssertionKeepsItsOwnerAndRawValuesInExtentFails() throws Exception {
        Path report = Files.createTempDirectory("atf-component-assertion-extent-failure-")
                .resolve("extent.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ConfigurationTest", "invalid quantity", List.of("component"));
            var body = scenario.startPhase("TEST BODY");
            var step = body.startAssertionStep("Component assertions");
            step.assertions(List.of(new AtfAssertion("Configured quantity", "200", "201", false)));
            step.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        try (var playwright = Playwright.create();
             var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             var page = browser.newPage()) {
            page.navigate(report.toUri() + "#test-view");
            PHASES.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            PHASES.failsTab(page, false).click();
            var index = PHASES.failureIndex(page);
            assertEquals("component", index.getAttribute("data-atf-evidence-owner"));
            String text = index.innerText().replaceAll("\\s+", " ");
            assertTrue(text.contains("STEP Component assertions"), text);
            assertTrue(text.contains("Configured quantity expected 200, actual 201"), text);
            assertFalse(text.contains("UI assertion") || text.contains("REQUEST")
                    || text.contains("DURATION") || text.contains("200 OK"), text);
            PHASES.openFailureInTests(page).click();
            var scenario = PHASES.scenarios(page, false).first();
            page.waitForCondition(() -> "true".equals(scenario.getAttribute("data-atf-scenario-open")));
            assertEquals("true", scenario.getAttribute("data-atf-scenario-open"));
            var phase = PHASES.phases(scenario).first();
            var step = PHASES.assertionStep(phase);
            page.waitForCondition(() -> PHASES.assertionTable(step, false).isVisible());
            assertEquals("true", phase.getAttribute("data-atf-phase-open"));
            assertEquals("true", step.getAttribute("data-atf-node-open"));
            assertTrue(PHASES.assertionTable(step, false).isVisible());
            assertEquals(1, PHASES.assertionRows(PHASES.assertionTable(step, false)).count());
        }
    }

    private static void publish(AtfReportPorts.Session session) {
        var scenario = session.startScenario("ConfigurationTest", "configured profile", List.of("component"));
        var body = scenario.startPhase("TEST BODY");
        var step = body.startAssertionStep("Component assertions");
        step.assertions(IntStream.rangeClosed(1, 42)
                .mapToObj(index -> new AtfAssertion(
                        index == 1 ? "Configured product" : "Check " + index,
                        "expected-" + index, "expected-" + index, true))
                .toList());
        step.complete(AtfReportStatus.PASS);
        body.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
    }
}
