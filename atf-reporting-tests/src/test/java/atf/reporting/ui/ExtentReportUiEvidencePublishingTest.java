package atf.reporting.ui;
import atf.testkit.AtfUnitTest;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.reporting.ui.support.ExtentReportUiEvidenceTestSupport;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.reporting.ui.support.ExtentReportUiEvidenceTestSupport.EXTENT_UI_TEST_PACKAGE;
import static atf.reporting.contract.AtfGherkinKeyword.AND;
import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportUiEvidencePublishingTest {
    @Test
    void publishesUiAuditWithoutImplicitTeardownPhase() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-no-teardown-");
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "ui-no-teardown.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", "atf.api.tests", "visual audit", List.of("ui"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(THEN, "the report is visually auditable", "#test-view", null,
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("<span>SETUP</span>")),
                () -> assertTrue(html.contains("<span>TEST BODY</span>")),
                () -> assertFalse(html.contains("<span>TEARDOWN</span>")),
                () -> assertFalse(html.contains("<details class=\"step-detail request-evidence\"")),
                () -> assertFalse(html.contains("REQUEST ACTION")),
                () -> assertFalse(html.contains("REQUEST TARGET")),
                () -> assertTrue(html.contains("the report is visually auditable")));
    }

    @Test
    void publishesArtifactOutcomeWithoutSyntheticUiValidationChild() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-outcome-");
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "ui-outcome.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", "atf.api.tests", "artifact publication", List.of("ui"));
            var teardown = scenario.startPhase(ReportUiContract.Phase.TEARDOWN.displayName());
            teardown.outcome(THEN, "the UI artifact was published", "UI audit report", AtfReportStatus.PASS);
            teardown.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("THEN the UI artifact was published")),
                () -> assertTrue(html.contains("OUTCOME TYPE")),
                () -> assertTrue(html.contains("artifact-publication")),
                () -> assertFalse(html.contains("UI validation")));
    }

    @Test
    void publishesSetupAsArtifactEvidenceWithoutSyntheticUiValidationChild() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-setup-evidence-");
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "ui-setup.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "report setup", List.of("ui"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.setup(GIVEN, "the input report is available",
                    "input-report.html", List.of(new AtfAssertion(
                            "Input report", "published HTML", "input-report.html", true)),
                    AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int setupStart = html.indexOf("<details class=\"step-detail setup-evidence atf-setup-evidence\"");
        int setupEnd = html.indexOf("</details>", setupStart);
        String setupMarkup = setupStart >= 0 && setupEnd > setupStart
                ? html.substring(setupStart, setupEnd)
                : "";
        assertAll(
                () -> assertTrue(setupStart >= 0 && setupEnd > setupStart),
                () -> assertTrue(html.contains("data-atf-evidence-type=\"report-setup\"")),
                () -> assertTrue(html.contains("<summary aria-expanded=\"false\"><span class=\"atf-disclosure-glyph\"")),
                () -> assertTrue(html.contains("Setup evidence · 1 check")),
                () -> assertTrue(setupMarkup.contains("<div class=\"body\"><div class=\"atf-table-viewport\" tabindex=\"0\"><table")),
                () -> assertTrue(setupMarkup.endsWith("</table></div></div>")),
                () -> assertTrue(html.contains("class=\"atf-setup-evidence-table\"")),
                () -> assertTrue(html.contains("CHECK") && html.contains("EXPECTED")
                        && html.contains("ACTUAL") && html.contains("RESULT")),
                () -> assertTrue(html.contains("✅ PASS")),
                () -> assertTrue(html.contains("input-report.html")),
                () -> assertFalse(setupMarkup.contains("<details open")),
                () -> assertFalse(setupMarkup.contains("Request details")),
                () -> assertFalse(setupMarkup.contains("Response details")),
                () -> assertFalse(setupMarkup.contains("UI validation")),
                () -> assertFalse(setupMarkup.contains("UI validation")));
    }

    @Test
    void keepsSetupEvidenceInsideOneClosedFrameForMultipleChecks() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-setup-multi-checks-");
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "ui-setup-multi-checks.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "multiple setup checks", List.of("ui"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.setup(GIVEN, "the input report is available",
                    "input-report.html", List.of(
                            new AtfAssertion("Input report", "published HTML", "input-report.html", true),
                            new AtfAssertion("Input report is readable", "readable", "readable", true)),
                    AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int disclosure = html.indexOf("<details class=\"step-detail setup-evidence atf-setup-evidence\"");
        int closing = html.indexOf("</table></div></div></details>", disclosure);
        assertAll(
                () -> assertTrue(disclosure >= 0),
                () -> assertTrue(html.contains("Setup evidence · 2 checks")),
                () -> assertTrue(closing > disclosure),
                () -> assertEquals(1, ExtentReportUiEvidenceTestSupport.count(html,
                        "step-detail setup-evidence atf-setup-evidence")));
    }

    @Test
    void keepsRegistrationChecksActionsAndApiEvidenceInsideAuthenticatedSetup() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-authenticated-setup-");
        Path report;
        var assertion = new AtfAssertion("Registration response", "HTTP 2xx", "HTTP 201", true);
        var action = new AtfUiActionRecord(1, "Click", "button[name=Register]",
                "registration submitted", "click completed", 4, AtfReportStatus.PASS);
        var apiRequest = new ReportRequest("POST", "/users/register", Map.of(),
                Map.of("Content-Type", "application/json"), "none", "{\"email\":\"redacted\"}");
        var apiResponse = new ReportResponse(201, "{\"message\":\"registered\"}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(12),
                "https://api.practicesoftwaretesting.com/users/register");
        var apiExecution = new ReportExecutionEvidence(apiResponse, 1, Duration.ofMillis(12),
                List.of(), List.of(new ReportAttemptRecord(1, 201, Duration.ofMillis(12),
                "SUCCESS", "registration accepted")), apiRequest, ReportTransportProvenance.LIVE_HTTP);

        try (var session = UiExtentReportSession.openConfigured(root, "ui-authenticated-setup.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("AuthenticatedUiTest", EXTENT_UI_TEST_PACKAGE,
                    "registration before authenticated flow", List.of("ui", "authenticated"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.setup(GIVEN, "a new account is registered", "/auth/register", List.of(assertion),
                    List.of(action), observedExchanges(List.of(apiExecution)), AtfReportStatus.PASS);
            setup.setup(AND, "the account is signed in", "/account", List.of(
                    new AtfAssertion("Account session", "authenticated", "authenticated", true)),
                    List.of(action), UiApiValidationEvidence.empty(), new ReportScreenshot(
                            "Authenticated account screen",
                            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="),
                    AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(THEN, "the authenticated account is ready", "/account", null,
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int setupPosition = html.indexOf("<span>SETUP</span>");
        int registrationPosition = html.indexOf("GIVEN a new account is registered");
        int setupEvidencePosition = html.indexOf("class=\"atf-setup-evidence-table\"");
        int actionTablePosition = html.indexOf("Playwright action sequence (1 entry)");
        int screenshotPosition = html.indexOf("Authenticated account screen");
        int actionTableEnd = html.lastIndexOf("</table>", screenshotPosition);
        int testBodyPosition = html.indexOf("<span>TEST BODY</span>");
        assertAll(
                () -> assertTrue(setupPosition >= 0 && registrationPosition > setupPosition),
                () -> assertTrue(setupEvidencePosition > registrationPosition),
                () -> assertTrue(html.contains("Setup evidence · 1 check")),
                () -> assertTrue(html.contains("Playwright action sequence (1 entry)")),
                () -> assertTrue(html.contains("API validation")),
                () -> assertTrue(html.contains("POST /users/register")),
                () -> assertTrue(actionTablePosition >= 0 && actionTableEnd > actionTablePosition
                                && screenshotPosition > actionTableEnd,
                        "authenticated SETUP screenshot must follow its Playwright action table"),
                () -> assertTrue(html.contains("Authenticated account screen")),
                () -> assertTrue(testBodyPosition > setupEvidencePosition),
                () -> assertFalse(html.contains("UI validation")));
    }

    @Test
    void projectsApiEvidenceInsideTheUiCheckpointWithClosedDisclosures() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-api-evidence-");
        Path report;
        var request = new ReportRequest("POST", "/checkout", Map.of("step", "payment"),
                Map.of("Authorization", "Bearer secret", "Content-Type", "application/json"),
                "bearer", "{\"payment\":{\"method\":\"cash-on-delivery\"},\"items\":[1,2]}");
        var response = new ReportResponse(200, "{\"message\":\"Payment was successful\",\"meta\":{\"retryable\":false}}",
                Map.of("Content-Type", "application/json", "X-Request-Id", "req-123"), Duration.ofMillis(12),
                "https://api.practicesoftwaretesting.com/payment/check");
        var execution = new ReportExecutionEvidence(response, 1, Duration.ofMillis(12), List.of(),
                List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(12), "SUCCESS", "final response")),
                request, ReportTransportProvenance.LIVE_HTTP);

        try (var session = UiExtentReportSession.openConfigured(root, "ui-api-evidence.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "API evidence in UI step", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(GIVEN, "the payment response is visible", "/checkout", null,
                    List.of(new AtfAssertion("Payment", "success", "success", true)), List.of(),
                    observedExchanges(List.of(execution)), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("Request details")),
                () -> assertTrue(html.contains("Response details")),
                () -> assertTrue(html.contains("Execution evidence")),
                () -> assertTrue(html.contains("REQUEST JSON")),
                () -> assertTrue(html.contains("RESPONSE JSON")),
                () -> assertTrue(html.contains("<td>AUTH PROFILE</td><td>bearer</td>")),
                () -> assertTrue(html.contains("<td>AUTH TYPE</td><td>bearer</td>")),
                () -> assertFalse(html.contains("<td>AUTHENTICATION</td>")),
                () -> assertTrue(html.contains("REQUEST HEADERS")),
                () -> assertTrue(html.contains("MANUAL REPLAY")),
                () -> assertTrue(html.contains("API validation")),
                () -> assertTrue(html.contains("Exchange 1")),
                () -> assertTrue(html.contains("METHOD")),
                () -> assertTrue(html.contains("HTTP STATUS")),
                () -> assertFalse(html.contains("AUTH SOURCE")),
                () -> assertFalse(html.contains("REQUEST API 1 HEADERS")),
                () -> assertFalse(html.contains("RESPONSE API 1 HEADERS")),
                () -> assertFalse(html.contains("sec-ch-ua")),
                () -> assertFalse(html.contains("User-Agent")),
                () -> assertFalse(html.contains("Referer")),
                () -> assertTrue(html.contains("RESPONSE STATUS")),
                () -> assertTrue(html.contains("\"payment\" : {") || html.contains("&quot;payment&quot; : {")),
                () -> assertTrue(html.contains("\"retryable\" : false")
                        || html.contains("&quot;retryable&quot; : false")),
                () -> assertFalse(html.contains("Bearer ***REDACTED***")),
                () -> assertFalse(html.contains("Bearer secret")),
                () -> assertFalse(html.contains("<details open")));
    }

    @Test
    void keepsUiAndApiValidationUnderOneGherkinCheckpoint() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-api-trace-combined-");
        Path report;
        var request = new ReportRequest("POST", "/checkout", Map.of("step", "payment"),
                Map.of("Content-Type", "application/json"), "none",
                "{\"payment\":{\"method\":\"cash-on-delivery\"}}");
        var response = new ReportResponse(200, "{\"message\":\"Payment was successful\"}",
                Map.of("Content-Type", "application/json", "X-Request-Id", "req-456"),
                Duration.ofMillis(12),
                "https://api.practicesoftwaretesting.com/payment/check");
        var execution = new ReportExecutionEvidence(response, 1, Duration.ofMillis(12), List.of(),
                List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(12), "SUCCESS", "final response")),
                request, ReportTransportProvenance.LIVE_HTTP);

        try (var session = UiExtentReportSession.openConfigured(root, "ui-api-trace-sibling.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "combined UI and API validation", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(GIVEN, "the payment response is visible", "/checkout", null,
                    List.of(new AtfAssertion("Payment", "success", "success", true)), List.of(),
                    observedExchanges(List.of(execution)), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        String traceTitle = "<span>GIVEN the payment response is visible</span>";
        int uiStep = html.indexOf(traceTitle);
        int requestJson = html.indexOf("REQUEST JSON");
        assertAll(
                () -> assertTrue(uiStep >= 0),
                // The checkpoint context remains internal; only its card publishes the proposition.
                () -> assertEquals(1, ExtentReportUiEvidenceTestSupport.count(
                        html, "<span>GIVEN the payment response is visible</span>")),
                () -> assertFalse(html.contains("REQUEST ACTION")),
                () -> assertFalse(html.contains("REQUEST TARGET")),
                () -> assertEquals(1, ExtentReportUiEvidenceTestSupport.count(html,
                        "<details class=\"step-detail request-evidence\""),
                        "only the actual API exchange may publish request details"),
                () -> assertEquals(1, ExtentReportUiEvidenceTestSupport.count(html,
                        "<details class=\"step-detail response-evidence\""),
                        "only the actual API exchange may publish response details"),
                () -> assertFalse(html.contains("<td>EVIDENCE TYPE</td>")),
                () -> assertFalse(html.contains("<td>EVIDENCE POLICY</td>")),
                 () -> assertTrue(html.contains("UI validation")),
                 () -> assertTrue(html.contains("API validation")),
                 () -> assertTrue(html.contains("Request details")),
                 () -> assertTrue(html.contains("Response details")),
                 () -> assertTrue(html.contains("TECHNICAL TRACE")),
                 () -> assertTrue(html.contains("Execution evidence")),
                 () -> assertTrue(html.contains("<td>AUTH TYPE</td><td>none</td>")),
                 () -> assertFalse(html.contains("<td>AUTHENTICATION</td>")),
                 () -> assertTrue(html.contains("REQUEST HEADERS")),
                 () -> assertTrue(html.contains("MANUAL REPLAY")),
                 () -> assertTrue(html.contains("API validation")),
                 () -> assertTrue(html.contains("Exchange 1")),
                 () -> assertTrue(html.contains("METHOD")),
                 () -> assertTrue(html.contains("HTTP STATUS")),
                 () -> assertFalse(html.contains("API trace · GIVEN the payment response is visible")),
                 () -> assertFalse(html.contains("TRACE /checkout — GIVEN")),
                 () -> assertFalse(html.contains("TRACE https://api.practicesoftwaretesting.com")),
                  () -> assertTrue(html.contains("RESPONSE HEADERS")),
                  () -> assertTrue(requestJson > uiStep),
                () -> assertFalse(html.contains("REQUEST JSON</span></summary>")));
    }

    @Test
    void keepsUiPassAndExchangeLocalApiFailureSeparate() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-api-owned-verdicts-");
        Path report;
        var add = execution("POST", "/carts/1", "add-contract");
        var read = execution("GET", "/carts/1", "read-contract");
        var api = new UiApiValidationEvidence(List.of(), List.of(
                new UiApiValidationEvidence.Exchange(add,
                        List.of(new AtfAssertion("add contract fails", "valid cart line", "wrong quantity", false))),
                new UiApiValidationEvidence.Exchange(read,
                        List.of(new AtfAssertion("read contract passes", "cart line visible", "cart line visible", true)))));

        try (var session = UiExtentReportSession.openConfigured(root, "ui-api-owned-verdicts.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "owned API verdict", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(THEN, "the cart page displays the configured line", "/cart", null,
                    List.of(new AtfAssertion("screen contract passes", "cart is visible", "cart is visible", true)),
                    List.of(), api, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        int uiValidation = html.indexOf("UI validation");
        int apiValidation = html.indexOf("API validation");
        assertAll(
                () -> assertTrue(uiValidation >= 0 && apiValidation > uiValidation),
                () -> assertTrue(html.contains("screen contract passes")),
                () -> assertTrue(html.contains("add contract fails")),
                () -> assertTrue(html.contains("read contract passes")),
                () -> assertTrue(html.contains("Exchange 1")),
                () -> assertTrue(html.contains("Exchange 2")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"fail\"")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"pass\"")),
                () -> assertTrue(html.contains("❌")),
                () -> assertTrue(html.contains("✅")));
    }

    @Test
    void keepsApiPassWhenTheIndependentUiAssertionFails() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-fail-api-pass-");
        Path report;
        var exchange = execution("POST", "/auth/login", "expected-rejection", 401);
        var api = new UiApiValidationEvidence(List.of(), List.of(
                new UiApiValidationEvidence.Exchange(exchange,
                        List.of(new AtfAssertion("expected 401 rejection", "401 rejected", "401 rejected", true)))));

        try (var session = UiExtentReportSession.openConfigured(root, "ui-fail-api-pass.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "independent channel failure", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(THEN, "the rejection notice is visible", "/login", null,
                    List.of(new AtfAssertion("screen assertion fails", "notice visible", "notice absent", false)),
                    List.of(), api, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.FAIL);
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        int uiAssertion = html.indexOf("screen assertion fails");
        int apiAssertion = html.indexOf("expected 401 rejection");
        assertAll(
                () -> assertTrue(uiAssertion >= 0 && apiAssertion > uiAssertion),
                () -> assertTrue(html.contains("notice absent")),
                () -> assertTrue(html.contains("401 rejected")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"fail\"")),
                () -> assertTrue(html.contains("data-atf-evidence-status=\"pass\"")));
    }

    private static ReportExecutionEvidence execution(String method, String path, String responseBody) {
        return execution(method, path, responseBody, 200);
    }

    private static ReportExecutionEvidence execution(String method, String path,
                                                     String responseBody, int status) {
        var request = new ReportRequest(method, path, Map.of(), Map.of("Accept", "application/json"),
                "none", "{}");
        var response = new ReportResponse(status, "{\"result\":\"" + responseBody + "\"}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(2),
                "https://api.practicesoftwaretesting.com" + path);
        return new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(), List.of(),
                request, ReportTransportProvenance.LIVE_HTTP);
    }

    @Test
    void rendersExecutionEvidenceForEveryExchangeInOneUiApiTrace() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-api-trace-exchanges-");
        Path report;
        var firstRequest = new ReportRequest("GET", "/products/1", Map.of(),
                Map.of("Accept", "application/json"), "none", "");
        var firstResponse = new ReportResponse(200, "{\"id\":1}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(8),
                "https://api.practicesoftwaretesting.com/products/1");
        var first = new ReportExecutionEvidence(firstResponse, 1, Duration.ofMillis(8), List.of(),
                List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(8), "SUCCESS", "first-exchange-evidence")),
                firstRequest, ReportTransportProvenance.LIVE_HTTP);
        var secondRequest = new ReportRequest("GET", "/cart", Map.of(),
                Map.of("Accept", "application/json"), "none", "");
        var secondResponse = new ReportResponse(200, "{\"quantity\":1}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(9),
                "https://api.practicesoftwaretesting.com/cart");
        var second = new ReportExecutionEvidence(secondResponse, 1, Duration.ofMillis(9), List.of(),
                List.of(new ReportAttemptRecord(1, 200, Duration.ofMillis(9), "SUCCESS", "second-exchange-evidence")),
                secondRequest, ReportTransportProvenance.LIVE_HTTP);

        try (var session = UiExtentReportSession.openConfigured(root, "ui-api-trace-exchanges.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "two causal exchanges", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.apiTrace(THEN, "cart is ready", "/checkout", observedExchanges(List.of(first, second)), AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("products/1")),
                () -> assertTrue(html.contains("/cart")),
                () -> assertTrue(html.contains("first-exchange-evidence")),
                () -> assertTrue(html.contains("second-exchange-evidence")),
                () -> assertTrue(ExtentReportUiEvidenceTestSupport.count(html,
                        "RESPONSE JSON") >= 2),
                () -> assertTrue(ExtentReportUiEvidenceTestSupport.count(html,
                        "API validation") == 1),
                () -> assertTrue(ExtentReportUiEvidenceTestSupport.count(html,
                        "Exchange 1") == 1),
                () -> assertTrue(ExtentReportUiEvidenceTestSupport.count(html,
                        "Exchange 2") == 1),
                () -> assertFalse(html.contains("API 1 · METHOD")),
                () -> assertFalse(html.contains("RESPONSE JSON · API")));
    }

    @Test
    void keepsRetryHistoryInsideOneExchangeNode() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-api-trace-retry-");
        Path report;
        var request = new ReportRequest("GET", "/products/1", Map.of(),
                Map.of("Accept", "application/json"), "none", "");
        var response = new ReportResponse(200, "{\"id\":1}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(12),
                "https://api.practicesoftwaretesting.com/products/1");
        var evidence = new ReportExecutionEvidence(response, 2, Duration.ofMillis(30), List.of(),
                List.of(new ReportAttemptRecord(1, 503, Duration.ofMillis(18), "RETRY", "temporary upstream"),
                        new ReportAttemptRecord(2, 200, Duration.ofMillis(12), "SUCCESS", "final response")),
                request, ReportTransportProvenance.LIVE_HTTP);

        try (var session = UiExtentReportSession.openConfigured(root, "ui-api-trace-retry.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "retry is one exchange", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.apiTrace(THEN, "product is ready", "/products/1", observedExchanges(List.of(evidence)), AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertEquals(1, ExtentReportUiEvidenceTestSupport.count(html, "Exchange 1")),
                () -> assertTrue(html.contains("Execution evidence · 2 attempts")),
                () -> assertTrue(html.contains("temporary upstream")),
                () -> assertTrue(html.contains("final response")));
    }

    @Test
    void publishesTheJourneyAggregateAsOneCollapsedInspectableRequest() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-network-aggregate-");
        Path report;
        var request = new ReportRequest("GET", "/session", Map.of(),
                Map.of("Accept", "application/json"), "none", "");
        var response = new ReportResponse(401, "{\"message\":\"Unauthorized\"}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(4),
                "https://api.example.test/session");
        var evidence = new ReportExecutionEvidence(response, 1, Duration.ofMillis(4), List.of(),
                List.of(new ReportAttemptRecord(1, 401, Duration.ofMillis(4), "FAILURE", "unauthorized response")),
                request, ReportTransportProvenance.LIVE_HTTP);
        var step = ReportNetworkInspector.Step.from("GIVEN", "session response is observed",
                List.of(new AtfAssertion("Session response", "401", "401", true)), List.of(evidence));
        try (var session = UiExtentReportSession.openConfigured(root, "ui-network-aggregate.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "journey network aggregate", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.networkInspector("API network inspector — ordered UI journey",
                    List.of(evidence), List.of(step));
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report).toLowerCase();
        long alignedAggregateRows = Pattern.compile(
                "<tr class=\"event-row\">(?:(?!</tr>).)*<span class=\"badge log info-bg\">info</span>"
                        + "(?:(?!</tr>).)*<details class=\"step-detail network-aggregate-section",
                Pattern.DOTALL).matcher(html).results().count();
        assertAll(
                () -> assertTrue(html.contains("api network inspector")),
                () -> assertTrue(html.indexOf("network aggregate · 1 exchanges")
                                > html.indexOf("api network inspector")),
                () -> assertEquals(1, html.split("network aggregate · 1 exchanges", -1).length - 1),
                () -> assertTrue(html.contains("authentication evidence · 1")),
                () -> assertTrue(html.contains("scenario correlation · 1")),
                () -> assertTrue(html.contains("transport counters")),
                () -> assertFalse(html.contains("transport counters · 1")),
                () -> assertTrue(html.contains("unique request signatures · 1")),
                () -> assertFalse(html.contains("technical diagnostics")),
                () -> assertTrue(html.contains("network-signatures")),
                () -> assertEquals(4, alignedAggregateRows,
                        "each inspector section must own an INFO badge in its Status/Details event row"),
                () -> assertTrue(html.contains("response details · 5 fields")),
                () -> assertFalse(html.contains("network ui step")),
                () -> assertFalse(html.contains("network exchange")),
                () -> assertTrue(html.contains("evidence type") && html.contains("api-trace"),
                        "the ordered network inspector must expose its typed technical evidence contract"),
                () -> assertFalse(html.contains("network.aggregate.authentication")),
                () -> assertFalse(html.contains("unauthorized bootstrap")));
    }

    @Test
    void publishesRequestedJourneyInspectorWhenThereAreNoExchangesOrTechnicalDiagnostics() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-empty-network-aggregate-");
        Path report;
        var emptyApiStep = ReportNetworkInspector.Step.from("GIVEN", "catalog page is ready",
                List.of(new AtfAssertion("Catalog", "ready", "ready", true)), List.of());

        try (var session = UiExtentReportSession.openConfigured(root, "ui-empty-network-aggregate.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "catalog without API traffic", List.of("ui"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.networkInspector("API network inspector — ordered UI journey",
                    List.of(), List.of(emptyApiStep));
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report).toLowerCase();
        assertAll(
                () -> assertTrue(html.contains("api network inspector — ordered ui journey")),
                () -> assertTrue(html.contains("network aggregate · 0 exchanges")),
                () -> assertEquals(1, html.split("api network inspector — ordered ui journey", -1).length - 1));
    }

    @Test
    void rendersBrowserRequestFailureAsUnavailableWarningWithoutInventingAResponse() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-request-failed-");
        var request = new ReportRequest("POST", "/carts", Map.of(), Map.of(), "none", "");
        var response = new ReportResponse(-1, "response body unavailable", Map.of(),
                Duration.ofMillis(7), "https://api.example.test/carts");
        var evidence = new ReportExecutionEvidence(response, 1, Duration.ofMillis(7), List.of(),
                List.of(new ReportAttemptRecord(1, -1, Duration.ofMillis(7),
                        "FAILURE", "net::ERR_CONNECTION_RESET")), request,
                ReportTransportProvenance.LIVE_HTTP);
        Path report;

        try (var session = UiExtentReportSession.openConfigured(root, "ui-request-failed.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiTest", EXTENT_UI_TEST_PACKAGE,
                    "browser request failure", List.of("ui", "api"));
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.apiTrace(THEN, "cart mutation is observable", "/carts", observedExchanges(List.of(evidence)),
                    AtfReportStatus.PASS);
            body.networkInspector("API network inspector", List.of(evidence));
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int exchangeTitle = html.indexOf("POST /carts — Exchange 1");
        int scenarioTitle = html.indexOf("<span>browser request failure</span>");
        assertAll(
                () -> assertTrue(html.contains("API validation")),
                () -> assertTrue(scenarioTitle > 0 && html.substring(
                        Math.max(0, scenarioTitle - 500), scenarioTitle).contains("warning-bg")),
                () -> assertTrue(exchangeTitle > 0),
                () -> assertTrue(exchangeTitle > 0 && html.substring(
                        Math.max(0, exchangeTitle - 500), exchangeTitle).contains("warning-bg")),
                () -> assertTrue(html.contains("Unavailable")),
                () -> assertTrue(html.contains("net::ERR_CONNECTION_RESET")),
                () -> assertTrue(html.contains("Technical diagnostics")),
                () -> assertFalse(html.contains("RESPONSE JSON")),
                () -> assertFalse(html.contains("HTTP STATUS</td><td>-1")));
    }
}


