package atf.ui.tests.extent.support.inspection.disclosure;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.locators.disclosure.ReportDisclosureLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.reporting.contract.AtfGherkinKeyword.AND;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.PHASE_DETAILS;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_EVIDENCE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.microsoft.playwright.options.WaitForSelectorState.VISIBLE;

/** Controlled report fixtures and browser probes; executable tests declare only intent. */
public final class ExtentReportDisclosureProbe {
    private static final String CATALOG_CHECKPOINT = "catalog home renders product cards, pagination and footer";
    private static final String UI_TARGET = "https://ui.example.test/catalog";
    private static final ReportDisclosureLocators LOCATORS = new ReportDisclosureLocators();

    private ExtentReportDisclosureProbe() { }

    public static void verifyReferencedScreenshot() {
        Path report = checkpointReport();
        var common = new ReportUiLocators();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            List<String> errors = new ArrayList<>();
            page.onPageError(errors::add);
            page.navigate(report.toUri() + "#test-view");
            LOCATORS.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            Locator scenarios = LOCATORS.scenarios(page);
            assertEquals("false", scenarios.first().getAttribute("data-atf-scenario-open"),
                    "the canonical image belongs to an unexpanded scenario");
            Locator duplicateScenario = scenarios.nth(1);
            openIfClosed(duplicateScenario, "data-atf-scenario-open");
            Locator phase = LOCATORS.firstPhase(duplicateScenario);
            openIfClosed(phase, "data-atf-phase-open");
            Locator checkpoint = LOCATORS.requests(phase).first();
            openIfClosed(checkpoint, "data-atf-node-open");
            Locator validation = LOCATORS.validations(checkpoint).first();
            openIfClosed(validation, "data-atf-node-open");
            LOCATORS.summary(common.actionSequences(validation).first()).click();
            Locator duplicate = common.duplicateScreenshotMarkers(validation).first();
            assertEquals(1, duplicate.count());
            String originalUrl = page.url();
            for (boolean keyboard : List.of(false, true)) {
                if (keyboard) duplicate.press("Enter");
                else duplicate.click();
                Locator lightbox = common.lightbox(page);
                lightbox.waitFor(new Locator.WaitForOptions().setState(VISIBLE));
                Locator image = common.lightboxImage(lightbox);
                assertTrue(image.getAttribute("src").startsWith("data:image/png;base64,"));
                assertEquals(originalUrl, page.url(), "media access must not jump to a hidden anchor");
                assertEquals("false", scenarios.first().getAttribute("data-atf-scenario-open"));
                common.lightboxClose(lightbox).click();
            }
            assertTrue(errors.isEmpty(), () -> "Report runtime errors: " + errors);
        }
    }

    public static void verifyCustomValidationTitleLocator() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            String customTitle = "Catalog evidence audit";
            String markup = """
                    <div id="test-view"><div class="test-content-detail">
                      <div data-atf-node-role="validation">
                        <div class="card-header"><div class="card-title">
                          <a class="node">%s</a>
                        </div></div>
                        %s
                      </div>
                    </div></div>
                    """;
            var locators = new ReportUiLocators();
            page.setContent(markup.formatted(customTitle,
                    "<details class=\"atf-ui-action-sequence\"></details>"));
            assertEquals(customTitle, locators.uiValidationNodes(page).innerText());
            page.setContent(markup.formatted(customTitle, "<details class=\"request-evidence\"></details>"));
            assertEquals(0, locators.uiValidationNodes(page).count(),
                    "an API-only validation is not a UI action-evidence section");
        }
    }

    public static void verifyUiCheckpointEvidence() {
        Path report = checkpointReport();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            List<String> errors = new ArrayList<>();
            page.onPageError(errors::add);
            page.navigate(report.toUri() + "#test-view");
            LOCATORS.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            Locator scenarios = LOCATORS.scenarios(page);
            assertEquals(3, scenarios.count());
            for (int index = 0; index < scenarios.count(); index++) {
                Locator scenario = scenarios.nth(index);
                openIfClosed(scenario, "data-atf-scenario-open");
                Locator phase = LOCATORS.firstPhase(scenario);
                openIfClosed(phase, "data-atf-phase-open");
                Locator checkpoint = LOCATORS.requests(phase).first();
                openIfClosed(checkpoint, "data-atf-node-open");
                assertAll(
                        () -> assertEquals(0, LOCATORS.directLogTables(checkpoint).count(),
                                "UI context must not create a Status/Details log table"),
                        () -> assertEquals(0, LOCATORS.directRequestEvidence(checkpoint).count()),
                        () -> assertEquals(0, LOCATORS.directEvidence(checkpoint, RESPONSE_EVIDENCE).count(),
                                "checkpoint policy must not become a synthetic HTTP response"),
                        () -> assertFalse(checkpoint.innerText().contains("REQUEST ACTION")),
                        () -> assertFalse(checkpoint.innerText().contains("REQUEST TARGET")));
                Locator validation = LOCATORS.validations(checkpoint);
                openIfClosed(validation.first(), "data-atf-node-open");
                Locator assertions = LOCATORS.assertionSummary(validation.first());
                if (assertions.count() > 0) assertions.click();
                assertTrue(LOCATORS.assertionTables(validation.first()).isVisible());
                assertEquals(1, LOCATORS.screenshots(validation.first()).count(),
                        () -> "Selected validation DOM: " + validation.first().innerHTML());
                if (index == 0) {
                    openIfClosed(validation.nth(1), "data-atf-node-open");
                    Locator exchange = LOCATORS.requests(validation.nth(1)).first();
                    openIfClosed(exchange, "data-atf-node-open");
                    assertEquals(1, LOCATORS.evidence(exchange, REQUEST_EVIDENCE).count());
                    assertEquals(1, LOCATORS.evidence(exchange, RESPONSE_EVIDENCE).count());
                    assertEquals(1, LOCATORS.evidence(exchange, EXECUTION_EVIDENCE).count());
                    assertTrue(exchange.textContent().contains("REQUEST JSON"));
                    assertTrue(exchange.textContent().contains("RESPONSE JSON"));
                }
                LOCATORS.header(checkpoint).click();
                assertFalse(LOCATORS.header(validation.first()).isVisible());
            }
            assertAll(
                    () -> assertTrue(errors.isEmpty(), () -> "Report runtime errors: " + errors),
                    () -> assertEquals(0, LOCATORS.separators(page).count()),
                    () -> assertEquals(Boolean.TRUE, page.evaluate(
                            "document.documentElement.scrollWidth <= document.documentElement.clientWidth")));
        }
    }

    public static void verifyRequestSubtreeForMouseAndKeyboard() {
        Path report = TestReportPaths.report("extent-request-subtree-disclosure.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("DisclosureComponent", "request with evidence and child", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            phase.detail("API audit completeness", "1 of 1 exchanges matched");
            var request = phase.startRequest("GET /resource — inspect", "GET", "/resource");
            request.detail("request.header.Accept", "application/json");
            for (int index = 1; index <= 40; index++) {
                request.detail("request.header.X-Diagnostic-" + index, "value-" + index);
            }
            Duration retryDuration = Duration.ofMillis(40);
            var retryRequest = new ReportRequest("GET", "/resource", Map.of(), Map.of(), "none", "");
            var retryResponse = new ReportResponse(200, "{}", Map.of("Content-Type", "application/json"),
                    retryDuration, "https://api.example.test/resource");
            var attempts = new ArrayList<ReportAttemptRecord>();
            for (int number = 1; number <= 40; number++) {
                attempts.add(new ReportAttemptRecord(number, number == 40 ? 200 : 429,
                        Duration.ofMillis(1), number == 40 ? "SUCCESS" : "RETRYABLE_RESPONSE",
                        "attempt " + number));
            }
            request.execution(new ReportExecutionEvidence(retryResponse, attempts.size(), retryDuration,
                    List.of(), attempts, retryRequest, ReportTransportProvenance.ACCEPTANCE_MOCK));
            var validation = request.startValidation("Custom response check");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "200", "200", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            page.navigate(report.toUri() + "#test-view");
            LOCATORS.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            Locator scenario = LOCATORS.scenarios(page).first();
            openIfClosed(scenario, "data-atf-scenario-open");
            Locator phase = LOCATORS.firstPhase(scenario);
            openIfClosed(phase, "data-atf-phase-open");
            assertEquals(0, LOCATORS.evidence(phase, PHASE_DETAILS).count(),
                    "internal phase metadata must not publish an audit disclosure");
            assertEquals(0, LOCATORS.directLogTables(phase).count(),
                    "removing the audit disclosure must not leave a phase log wrapper");
            Locator request = LOCATORS.requests(phase).first();
            assertTrue(request.count() > 0, "request node must be present in the generated report");
            Locator validation = LOCATORS.validations(request).first();
            assertEquals("validation", validation.getAttribute("data-atf-node-role"),
                    "a custom validation title must not change its semantic role");

            Locator header = LOCATORS.header(request);
            header.click();
            assertState(request, true);
            Locator requestEvidence = LOCATORS.evidence(request, REQUEST_EVIDENCE);
            LOCATORS.summary(requestEvidence).click();
            Locator viewport = LOCATORS.viewport(requestEvidence);
            assertTrue(Boolean.TRUE.equals(viewport.evaluate("element => "
                    + "element.scrollHeight > element.clientHeight && "
                    + "getComputedStyle(element).overflowY === 'auto'")),
                    "long request metadata must scroll within the shared viewport");
            assertTrue(lastRowReachableWithStickyHeader(viewport),
                    "the final request metadata row must be reachable below the sticky header");
            Locator executionEvidence = LOCATORS.evidence(request, EXECUTION_EVIDENCE);
            LOCATORS.summary(executionEvidence).click();
            Locator retryViewport = LOCATORS.viewport(executionEvidence);
            assertTrue(Boolean.TRUE.equals(retryViewport.evaluate("element => "
                    + "element.scrollHeight > element.clientHeight && "
                    + "getComputedStyle(element).overflowY === 'auto'")),
                    "long retry history must scroll within the shared viewport");
            assertEquals("40", LOCATORS.finalAttempt(retryViewport).innerText(),
                    "the final retry row must remain in the rendered table");
            assertTrue(lastRowReachableWithStickyHeader(retryViewport),
                    "the final retry row must be reachable below the sticky header");
            page.setViewportSize(760, 900);
            assertTrue(lastRowReachableWithStickyHeader(viewport),
                    "the final request metadata row must remain reachable at a narrow viewport");
            assertTrue(lastRowReachableWithStickyHeader(retryViewport),
                    "the final retry row must remain reachable at a narrow viewport");
            assertEquals(Boolean.TRUE, page.evaluate(
                    "document.documentElement.scrollWidth <= document.documentElement.clientWidth"),
                    "narrow report layout must not introduce document-level horizontal overflow");
            page.evaluate("() => window.scrollTo(0, 0)");
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(report.resolveSibling("extent-request-subtree-disclosure-narrow.png"))
                    .setFullPage(true));
            assertTrue(LOCATORS.assertionTables(validation).count() > 0,
                    "custom-titled API validation must retain the direct assertion table");
            assertEquals(0, LOCATORS.assertionSummary(validation).count());
            header.click();
            assertState(request, false);
            header.focus();
            header.press("Enter");
            assertState(request, true);
            header.press("Space");
            assertState(request, false);
        }
    }

    public static void verifyEmptyPhaseDoesNotAdvertiseExpansion() {
        Path report = TestReportPaths.report("extent-empty-phase-disclosure.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("EmptyDisclosureComponent",
                    "configuration-only test with no report steps", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            page.navigate(report.toUri() + "#test-view");
            LOCATORS.firstOwner(page).click();
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");

            Locator scenario = LOCATORS.scenarios(page).first();
            openIfClosed(scenario, "data-atf-scenario-open");
            Locator phase = LOCATORS.firstPhase(scenario);
            Locator header = LOCATORS.header(phase);
            assertEquals("false", phase.getAttribute("data-atf-node-expandable"),
                    "an empty phase must be marked as a leaf");
            assertNull(header.getAttribute("role"), "an empty phase header is not an interactive control");
            assertNull(header.getAttribute("tabindex"), "an empty phase header is not keyboard-focusable");
            assertNull(header.getAttribute("aria-expanded"), "a leaf must not announce an expansion state");
            assertEquals("none", header.evaluate("element => getComputedStyle(element, '::before').display"),
                    "an empty phase must not show a plus glyph");

            header.click();
            assertNull(phase.getAttribute("data-atf-phase-open"),
                    "clicking an empty phase must not create a false open state");
        }
    }

    private static Path checkpointReport() {
        Path directory = TestReportPaths.report("ui-checkpoint-evidence.html").getParent();
        Path report;
        try (var session = UiExtentReportSession.openConfigured(directory,
                "ui-checkpoint-evidence.html", AtfConsoleSink.noOp())) {
            report = session.reportPath();
            for (var status : List.of(AtfReportStatus.PASS, AtfReportStatus.WARNING, AtfReportStatus.FAIL)) {
                var scenario = session.startScenario("UiCheckpointComponent", "atf.ui.component",
                        "Checkpoint evidence — " + status.displayLabel(), List.of("ui", "component"));
                var phase = scenario.startPhase("TEST BODY");
                String observed = switch (status) {
                    case PASS -> "visible";
                    case WARNING -> "partially visible";
                    default -> "missing";
                };
                var assertion = new AtfAssertion("Catalog grid", "visible", observed,
                        status != AtfReportStatus.FAIL, status);
                var action = new AtfUiActionRecord(1, "Get text", "[data-test=product-name]",
                        "product name", "Pliers", 2, status);
                phase.step(AND, CATALOG_CHECKPOINT, UI_TARGET, ReportTestScreenshots.ONE_PIXEL_PNG,
                        List.of(assertion), List.of(action),
                        observedExchanges(status == AtfReportStatus.PASS ? List.of(checkpointExchange()) : List.of()), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, status);
                phase.complete(status);
                scenario.complete(status);
            }
        }
        return report;
    }

    private static ReportExecutionEvidence checkpointExchange() {
        Duration duration = Duration.ofMillis(8);
        var headers = Map.of("Content-Type", "application/json");
        var request = new ReportRequest("POST", "/cart", Map.of(), headers, "none", "{\"productId\":1}");
        var response = new ReportResponse(201, "{\"cartId\":1}", headers,
                duration, "https://api.example.test/cart");
        return new ReportExecutionEvidence(response, 1, duration, List.of(),
                List.of(new ReportAttemptRecord(1, 201, duration, "SUCCESS", "component fixture")),
                request, ReportTransportProvenance.ACCEPTANCE_MOCK);
    }

    private static void openIfClosed(Locator card, String attribute) {
        if (!"true".equals(card.getAttribute(attribute))) {
            LOCATORS.header(card).click();
        }
    }

    private static void assertState(Locator request, boolean open) {
        String state = Boolean.toString(open);
        assertEquals(state, request.getAttribute("data-atf-node-open"));
        assertEquals(state, LOCATORS.header(request).getAttribute("aria-expanded"));
        Object visibility = request.evaluate("card => Array.from(card.children)"
                + ".filter(child => !child.classList.contains('card-header'))"
                + ".map(child => getComputedStyle(child).display)");
        assertEquals(List.of(open ? "block" : "none", open ? "block" : "none"), visibility,
                "the evidence panel and child accordion must share the parent state");
    }

    private static boolean lastRowReachableWithStickyHeader(Locator viewport) {
        return Boolean.TRUE.equals(viewport.evaluate("element => {"
                + "element.scrollTop=element.scrollHeight;"
                + "const bounds=element.getBoundingClientRect(),"
                + "last=element.querySelector('tbody tr:last-child'),"
                + "heading=element.querySelector('thead th');"
                + "return !!last&&!!heading&&last.getBoundingClientRect().bottom<=bounds.bottom+1"
                + "&&heading.getBoundingClientRect().top>=bounds.top-1;}"));
    }
}
