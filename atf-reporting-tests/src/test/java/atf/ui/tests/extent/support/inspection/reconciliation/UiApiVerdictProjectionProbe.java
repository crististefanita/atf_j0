package atf.ui.tests.extent.support.inspection.reconciliation;

import atf.core.http.request.HttpMethod;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.semantic.AtfReportSemanticSnapshot.RequestSnapshot;
import atf.reporting.api.request.ReportRequestHeaders;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.audit.ReportAuditChecks.check;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Browser proof that mixed UI/API verdicts preserve their failure owner and replay link. */
public final class UiApiVerdictProjectionProbe {
    private static final String OWNER = "UiApiVerdictProjectionFixture";
    private static final String UI_PASS_API_FAIL = "UI passes while API cart contract fails";
    private static final String UI_FAIL_API_PASS = "UI notice fails while API contract passes";
    private static final String BOTH_FAIL = "UI and API fail independently in one checkpoint";
    private static final String CORRELATION_FAIL = "API correlation fails with passing exchanges";
    private static final String PASSING_ROUTE = "/unrelated/passing";
    private static final ReportUiLocators LOCATORS = new ReportUiLocators();

    private UiApiVerdictProjectionProbe() { }

    public static void verifyMixedVerdictsAndFailsReplay() {
        Path report = publish();
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, "ui-api-verdict-projection-audit")) {
            var audit = new UiAuditFlow(context, policy, OWNER,
                    UiApiVerdictProjectionProbe.class.getPackageName(), List.of("component", "ui", "api"),
                    "the controlled UI/API report is available", "mixed verdict audit failed", OWNER);
            var page = audit.openTab(ReportUiContract.Tab.TESTS.hash());
            var flow = new ApiReportUiFlow(audit, LOCATORS);
            var owner = flow.openOwner(page, OWNER);
            assertEquals(4, LOCATORS.scenarios(owner.detail()).count(),
                    "both independent verdict scenarios must remain under one test owner");

            assertScenarioStatus(owner.detail(), UI_PASS_API_FAIL);
            assertScenarioStatus(owner.detail(), UI_FAIL_API_PASS);
            assertScenarioStatus(owner.detail(), BOTH_FAIL);
            assertScenarioStatus(owner.detail(), CORRELATION_FAIL);

            var expectations = List.of(
                    new FailureExpectation(UI_PASS_API_FAIL, "API cart quantity contract failed", ReportEvidenceOwner.API, "QUERY /cart/quantity", 0),
                    new FailureExpectation(UI_FAIL_API_PASS, "UI confirmation notice is missing", ReportEvidenceOwner.UI, "", 1),
                    new FailureExpectation(BOTH_FAIL, "API cart quantity contract failed", ReportEvidenceOwner.API, "QUERY /cart/quantity", 2),
                    new FailureExpectation(BOTH_FAIL, "API-looking UI notice contract failed", ReportEvidenceOwner.UI, "", 2),
                    new FailureExpectation(CORRELATION_FAIL, "API correlation is inconsistent", ReportEvidenceOwner.API, "API correlation", 3));
            for (int ordinal = 0; ordinal < expectations.size(); ordinal++) {
                int index = ordinal;
                audit.audit("mixed failure " + (index + 1) + " retains its owner and exact link", inspected -> {
                    Page failurePage = audit.openTab(ReportUiContract.Tab.FAILS.hash());
                    Locator fails = LOCATORS.failsView(failurePage);
                    assertEquals(expectations.size(), LOCATORS.failureItems(fails).count(),
                            "each failed assertion retains its owner, including a mixed-failure scenario");
                    audit.actions().click(LOCATORS.failureItemAction(failurePage, index), "inspect the failure owner");
                    Locator detail = LOCATORS.failureDetail(fails);
                    String text = detail.innerText();
                    var matches = expectations.stream().filter(expected -> text.contains(expected.scenario())
                            && text.contains(expected.cause())).toList();
                    assertEquals(1, matches.size(), () -> "missing, merged or wrong failure cause: " + text);
                    var expected = matches.getFirst();
                    String ownerMarker = AtfReportHtmlVocabulary.attribute(
                            AtfReportHtmlVocabulary.Attribute.EVIDENCE_OWNER, expected.owner().attributeValue());
                    assertTrue(detail.innerHTML().contains(ownerMarker), () -> "wrong semantic owner: " + text);
                    if (expected.owner() == ReportEvidenceOwner.API) {
                        assertTrue(text.contains("REQUEST") && text.contains(expected.request()), text);
                        assertTrue(text.contains("CHECKPOINT") && text.contains(expected.scenario()), text);
                        assertTrue(!text.contains("STEP UI assertion") && !text.contains("GET " + PASSING_ROUTE), text);
                        if (expected.request().startsWith("QUERY ")) {
                            assertTrue(text.contains("5 ms") && !text.contains("91 ms"),
                                    "failure duration must belong to its exchange: " + text);
                        }
                    } else {
                        assertTrue(text.contains("STEP") && text.contains("UI assertion"), text);
                    }
                    inspected.screen("the failure index preserves its typed owner and checkpoint", "Fails / " + expected.scenario(),
                            audit.capture(failurePage, "Typed failure owner: " + expected.owner()), List.of(
                                    check("Evidence owner", expected.owner().attributeValue(), ownerMarker,
                                            detail.innerHTML().contains(ownerMarker)),
                                    check("Failure cause", expected.cause(), text, text.contains(expected.cause())),
                                    check("Scenario", expected.scenario(), text, text.contains(expected.scenario()))),
                            AtfReportStatus.PASS);
                    Locator openLink = LOCATORS.failureDetailOpenLink(fails);
                    assertEquals(1, openLink.count());
                    String href = openLink.getAttribute("href");
                    String scenarioAnchor = "scenario-index=" + expected.scenarioIndex();
                    assertTrue(href != null && href.endsWith(scenarioAnchor), href);
                    audit.actions().click(LOCATORS.failureDetailOpenAction(failurePage), "open the exact failure in Tests");
                    audit.actions().waitForVisible(LOCATORS.testsView(failurePage), ReportUiContract.Tab.TESTS.hash());
                    assertTrue(failurePage.url().contains("#test-id=") && failurePage.url().endsWith(scenarioAnchor));
                    assertEquals("true", LOCATORS.scenarios(LOCATORS.visibleTestDetail(failurePage))
                            .nth(expected.scenarioIndex()).getAttribute("data-atf-scenario-open"));
                    String selectedScenario = LOCATORS.visibleTestDetail(failurePage).innerText();
                    assertTrue(selectedScenario.contains(expected.scenario()));
                    inspected.screen("Open in Tests selects the exact original scenario", "Tests / " + expected.scenario(),
                            audit.capture(failurePage, "Original scenario: " + expected.scenario()), List.of(
                                    check("Scenario deep-link", scenarioAnchor, failurePage.url(),
                                            failurePage.url().endsWith(scenarioAnchor)),
                                    check("Selected scenario", expected.scenario(), selectedScenario,
                                            selectedScenario.contains(expected.scenario()))), AtfReportStatus.PASS);
                });
            }
            assertTrue(context.browserDiagnostics().valid(),
                    () -> context.browserDiagnostics().violations().toString());
        }
    }

    private static void assertScenarioStatus(Locator ownerDetail, String scenarioTitle) {
        Locator scenarios = LOCATORS.scenarios(ownerDetail);
        for (int index = 0; index < scenarios.count(); index++) {
            Locator scenario = scenarios.nth(index);
            if (scenario.innerText().contains(scenarioTitle)) {
                assertTrue(LOCATORS.nodeStatusBadge(scenario).innerText().contains("Fail"),
                        () -> "scenario must aggregate the failing channel: " + scenarioTitle);
                boolean uiFails = scenarioTitle.equals(UI_FAIL_API_PASS) || scenarioTitle.equals(BOTH_FAIL);
                assertChannelBadge(scenario, ReportEvidenceOwner.UI, uiFails ? "Fail" : "Pass");
                assertChannelBadge(scenario, ReportEvidenceOwner.API,
                        scenarioTitle.equals(UI_FAIL_API_PASS) ? "Pass" : "Fail");
                return;
            }
        }
        throw new AssertionError("scenario is missing from the visible owner: " + scenarioTitle);
    }

    private static void assertChannelBadge(Locator scenario, ReportEvidenceOwner owner, String expected) {
        Locator card = LOCATORS.validationForOwner(scenario, owner);
        assertEquals(expected, LOCATORS.nodeStatusBadge(card).innerText().trim(),
                "the browser badge must preserve the local channel verdict");
    }

    private static Path publish() {
        Path requested = TestReportPaths.report("ui-api-verdict-projection.html");
        Path report;
        try (var session = UiExtentReportSession.openConfigured(requested.getParent(),
                requested.getFileName().toString(), AtfConsoleSink.noOp())) {
            report = session.reportPath();
            publishScenario(session, UI_PASS_API_FAIL,
                    assertion("UI cart checkpoint passes", "cart line visible", "cart line visible", true),
                    exchangeAssertion(false), AtfReportStatus.PASS);
            publishScenario(session, UI_FAIL_API_PASS,
                    assertion("UI confirmation notice is missing", "notice visible", "notice absent", false),
                    exchangeAssertion(true), AtfReportStatus.FAIL);
            publishScenario(session, BOTH_FAIL,
                    assertion("API-looking UI notice contract failed", "notice visible", "notice absent", false),
                    exchangeAssertion(false), AtfReportStatus.FAIL);
            publishCorrelationFailure(session);
            assertIndependentStatuses(session);
        }
        return report;
    }

    private static void publishScenario(UiExtentReportSession session, String title,
                                        AtfAssertion uiAssertion, AtfAssertion apiAssertion,
                                        AtfReportStatus uiStatus) {
        var scenario = session.startScenario(OWNER, "atf.ui.tests.extent.component.audit",
                title, List.of("ui", "api", "mixed-verdict"));
        var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        var api = new UiApiValidationEvidence(List.of(), List.of(
                new UiApiValidationEvidence.Exchange(exchange(title), List.of(apiAssertion)),
                new UiApiValidationEvidence.Exchange(exchange(PASSING_ROUTE),
                        List.of(assertion("unrelated API sibling passes", "ok", "ok", true)))));
        body.step(THEN, title, "/cart", null, List.of(uiAssertion), List.of(), api,
                ReportScreenshotPolicy.CapturePoint.CHECKPOINT, uiStatus);
        AtfReportStatus phaseStatus = uiStatus == AtfReportStatus.FAIL || !apiAssertion.passed()
                ? AtfReportStatus.FAIL : AtfReportStatus.PASS;
        body.complete(phaseStatus);
        scenario.complete(phaseStatus);
    }

    private static ReportExecutionEvidence exchange(String title) {
        String path = title.equals(PASSING_ROUTE) ? PASSING_ROUTE
                : title.equals(UI_FAIL_API_PASS) ? "/checkout/confirmation" : "/cart/quantity";
        String url = "https://report-fixture.invalid" + path;
        Duration elapsed = Duration.ofMillis(title.equals(PASSING_ROUTE) ? 91 : 5);
        String method = path.equals("/cart/quantity") ? "QUERY" : HttpMethod.GET.value();
        var request = ReportRequest.builder(method, path)
                .headers(ReportRequestHeaders.observed(Map.of("Accept", "application/json")))
                .body("", ReportBodyAvailability.ABSENT).build();
        var response = new ReportResponse(200, "{\"ok\":true}",
                Map.of("Content-Type", "application/json"), elapsed, url);
        return new ReportExecutionEvidence(response, 1, elapsed, List.of(),
                List.of(new ReportAttemptRecord(1, 200, elapsed, "SUCCESS", "controlled fixture")),
                request, ReportTransportProvenance.ACCEPTANCE_MOCK);
    }

    private static AtfAssertion exchangeAssertion(boolean passed) {
        return assertion(passed ? "API confirmation contract passes" : "API cart quantity contract failed",
                passed ? "confirmed" : "quantity=2", passed ? "confirmed" : "quantity=1", passed);
    }

    private static AtfAssertion assertion(String label, String expected, String actual, boolean passed) {
        return new AtfAssertion(label, expected, actual, passed);
    }

    private static void publishCorrelationFailure(UiExtentReportSession session) {
        var scenario = session.startScenario(OWNER, "atf.ui.tests.extent.component.audit",
                CORRELATION_FAIL, List.of("ui", "api", "correlation"));
        var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        var api = new UiApiValidationEvidence(
                List.of(assertion("API correlation is inconsistent", "matched", "unmatched", false)),
                List.of(new UiApiValidationEvidence.Exchange(exchange(PASSING_ROUTE),
                        List.of(assertion("API exchange passes", "ok", "ok", true)))));
        body.step(THEN, CORRELATION_FAIL, "/cart", null,
                List.of(assertion("UI remains passed", "visible", "visible", true)), List.of(), api,
                ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
        body.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private static void assertIndependentStatuses(UiExtentReportSession session) {
        for (var scenario : session.semanticSnapshot().classes().getFirst().scenarios()) {
            var checkpoint = (RequestSnapshot)
                    scenario.phases().getFirst().nodes().getFirst();
            var ui = checkpoint.validations().stream().filter(node -> node.evidenceOwner() == ReportEvidenceOwner.UI)
                    .findFirst().orElseThrow();
            var api = checkpoint.validations().stream().filter(node -> node.evidenceOwner() == ReportEvidenceOwner.API)
                    .findFirst().orElseThrow();
            boolean uiFail = scenario.name().equals(UI_FAIL_API_PASS) || scenario.name().equals(BOTH_FAIL);
            assertEquals(uiFail ? AtfReportStatus.FAIL : AtfReportStatus.PASS, ui.status());
            assertEquals(scenario.name().equals(UI_FAIL_API_PASS) ? AtfReportStatus.PASS : AtfReportStatus.FAIL, api.status());
            assertEquals(AtfReportStatus.PASS, api.requests().getLast().status(),
                    "a passing API sibling must not inherit the failed contract");
        }
    }

    private record FailureExpectation(String scenario, String cause, ReportEvidenceOwner owner,
                                      String request, int scenarioIndex) { }
}
