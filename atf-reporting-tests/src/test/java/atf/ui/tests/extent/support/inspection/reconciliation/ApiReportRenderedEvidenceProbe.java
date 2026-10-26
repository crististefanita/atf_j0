package atf.ui.tests.extent.support.inspection.reconciliation;

import atf.core.http.request.HttpMethod;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.api.contract.ApiReportEvidenceField;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.support.TestReportPaths;
import atf.mock.MockRoute;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.ui.tests.extent.support.audit.api.ApiReportContractValidation;
import atf.ui.tests.extent.support.audit.mock.MockReportContractAuditor;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.section.extract.EvidenceSectionExtractor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static atf.reporting.support.MockApiReportContractFixtures.acceptedInput;
import static atf.reporting.support.MockApiReportContractFixtures.acceptedPostRoute;
import static atf.reporting.support.MockApiReportContractFixtures.acceptedUrl;
import static atf.reporting.support.MockApiReportContractFixtures.configuration;
import static atf.reporting.support.MockApiReportContractFixtures.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the same DOM adapters as E2E against a small, controlled Extent publication. */
public final class ApiReportRenderedEvidenceProbe {
    private static final String OWNER = "RenderedApiEvidence";
    private static final String SIBLING_OWNER = "RenderedApiEvidenceSibling";
    private static final String AUDIT = "RenderedEvidenceAudit";
    private static final ReportUiLocators LOCATORS = new ReportUiLocators();

    private ApiReportRenderedEvidenceProbe() { }

    public static void verifyMetadata(boolean missingRequestQuery) {
        Path report = publish(missingRequestQuery ? "rendered-api-query-negative.html" : "rendered-api-fields.html",
                missingRequestQuery);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var flow = new ApiReportUiFlow(audit, LOCATORS);
            var page = audit.openTab(ReportUiContract.Tab.TESTS.hash());
            var owner = flow.openOwner(page, OWNER);
            assertEquals(1, flow.expandOwnerRequests(owner));
            var request = LOCATORS.requests(owner.detail()).first();
            assertEquals(acceptedUrl(), LOCATORS.responseUrl(request), "response URL remains independently observable");
            assertEquals(List.of(String.valueOf(acceptedInput().actualStatus())),
                    ApiReportEvidenceField.RESPONSE_STATUS.valuesIn(LOCATORS.executionFields(request)));
            var validator = new ApiReportContractValidation(LOCATORS,
                    new MockReportContractAuditor(configuration(acceptedPostRoute())));
            var result = validator.inspect(owner);
            assertEquals(1, result.requestCount());
            if (missingRequestQuery) {
                assertFalse(result.violations().isEmpty());
                assertTrue(result.violations().stream().anyMatch(value -> value.contains("expected query mode")),
                        () -> result.violations().toString());
            } else {
                assertTrue(result.violations().isEmpty(), () -> result.violations().toString());
            }
            assertTrue(context.browserDiagnostics().valid(), () -> context.browserDiagnostics().violations().toString());
        }
    }

    public static void verifyMissingResponseDisclosure() {
        Path report = publish("rendered-api-missing-response-disclosure.html", false);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var owner = new ApiReportUiFlow(audit, LOCATORS)
                    .openOwner(audit.openTab(ReportUiContract.Tab.TESTS.hash()), OWNER);
            assertEquals(1, LOCATORS.requests(owner.detail()).count());
            var request = LOCATORS.requests(owner.detail()).first();
            var responseDisclosure = LOCATORS.responseEvidenceDisclosure(request);
            assertEquals(1, responseDisclosure.count(), "the baseline fixture must contain response evidence");
            LOCATORS.parentDisclosure(responseDisclosure).evaluate("details => details.remove()");

            var validator = new ApiReportContractValidation(LOCATORS,
                    new MockReportContractAuditor(configuration(acceptedPostRoute())));
            var result = validator.inspect(owner);
            assertFalse(result.compliant(), "missing response evidence must not pass as a valid exchange");
            assertTrue(result.violations().stream().anyMatch(value -> value.contains("response JSON body count")),
                    () -> result.violations().toString());
            assertEquals(0, LOCATORS.responseDetails(request).count(),
                    "the negative fixture removes the rendered response disclosure itself");
            assertTrue(context.browserDiagnostics().valid(),
                    () -> context.browserDiagnostics().violations().toString());
        }
    }

    public static void verifyMissingRequestDisclosure() {
        Path report = publish("rendered-api-missing-request-disclosure.html", false);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var owner = new ApiReportUiFlow(audit, LOCATORS)
                    .openOwner(audit.openTab(ReportUiContract.Tab.TESTS.hash()), OWNER);
            var request = LOCATORS.requests(owner.detail()).first();
            var disclosure = LOCATORS.requestEvidenceDisclosure(request);
            assertEquals(1, disclosure.count(), "the baseline fixture must contain request evidence");
            LOCATORS.parentDisclosure(disclosure).evaluate("details => details.remove()");

            var result = validator().inspect(owner);
            assertFalse(result.compliant(), "missing request evidence must not pass as a valid exchange");
            assertTrue(result.violations().stream().anyMatch(value -> value.contains("request JSON body count")),
                    () -> result.violations().toString());
            assertEquals(0, LOCATORS.requestDetails(request).count(),
                    "the negative fixture removes the rendered request disclosure itself");
            assertTrue(context.browserDiagnostics().valid(),
                    () -> context.browserDiagnostics().violations().toString());
        }
    }

    public static void verifyMisownedResponseDisclosure() {
        Path report = publish("rendered-api-misowned-response-disclosure.html", false);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var owner = new ApiReportUiFlow(audit, LOCATORS)
                    .openOwner(audit.openTab(ReportUiContract.Tab.TESTS.hash()), OWNER);
            var request = LOCATORS.requests(owner.detail()).first();
            var responseDisclosure = LOCATORS.responseEvidenceDisclosure(request);
            assertEquals(1, responseDisclosure.count(), "the baseline fixture must contain response evidence");

            LOCATORS.moveResponseEvidenceToOwner(owner.detail());

            assertEquals(0, LOCATORS.responseDetails(request).count(),
                    "response evidence moved to the scenario owner must no longer belong to the exchange");
            assertEquals(1, LOCATORS.responseEvidenceDisclosure(owner.detail()).count(),
                    "the malformed response evidence remains visible, but under the wrong owner");
            var result = validator().inspect(owner);
            assertFalse(result.compliant(), "evidence under the scenario cannot satisfy the request contract");
            assertTrue(result.violations().stream().anyMatch(value -> value.contains("response JSON body count")),
                    () -> result.violations().toString());
            assertTrue(context.browserDiagnostics().valid(),
                    () -> context.browserDiagnostics().violations().toString());
        }
    }

    public static void verifyMisownedRequestDisclosure() {
        Path report = publish("rendered-api-misowned-request-disclosure.html", false);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var owner = new ApiReportUiFlow(audit, LOCATORS)
                    .openOwner(audit.openTab(ReportUiContract.Tab.TESTS.hash()), OWNER);
            var request = LOCATORS.requests(owner.detail()).first();
            assertEquals(1, LOCATORS.requestEvidenceDisclosure(request).count(),
                    "the baseline fixture must contain request evidence");
            LOCATORS.moveRequestEvidenceToOwner(owner.detail());

            assertEquals(0, LOCATORS.requestDetails(request).count(),
                    "request evidence moved to the scenario owner cannot belong to the exchange");
            assertEquals(1, LOCATORS.requestEvidenceDisclosure(owner.detail()).count(),
                    "the malformed request evidence remains visible under the wrong owner");
            var result = validator().inspect(owner);
            assertFalse(result.compliant(), "evidence under the scenario cannot satisfy the request contract");
            assertTrue(result.violations().stream().anyMatch(value -> value.contains("request JSON body count")),
                    () -> result.violations().toString());
            assertTrue(context.browserDiagnostics().valid(),
                    () -> context.browserDiagnostics().violations().toString());
        }
    }

    public static void verifySiblingOwnersAreIsolated() {
        Path report = publishWithSiblingOwner("rendered-api-sibling-owners.html");
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var flow = new ApiReportUiFlow(audit, LOCATORS);
            var page = flow.openTests();
            assertEquals(List.of(OWNER, SIBLING_OWNER), flow.ownerNames(page),
                    "both API owners must be independently selectable in one report");

            for (String ownerName : List.of(OWNER, SIBLING_OWNER)) {
                var owner = flow.openOwner(page, ownerName);
                assertEquals(1, LOCATORS.requests(owner.detail()).count(),
                        () -> ownerName + " must expose only its own exchange");
                assertTrue(LOCATORS.requestTitle(LOCATORS.requests(owner.detail()).first()).contains(ownerName),
                        "the selected detail panel must not retain a sibling owner's request");
                var result = validator().inspect(owner);
                assertEquals(1, result.requestCount(),
                        () -> ownerName + " audit must not include sibling exchanges");
                assertTrue(result.violations().isEmpty(), () -> result.violations().toString());
            }
            assertTrue(context.browserDiagnostics().valid(),
                    () -> context.browserDiagnostics().violations().toString());
        }
    }

    private static ApiReportContractValidation validator() {
        return new ApiReportContractValidation(LOCATORS,
                new MockReportContractAuditor(configuration(acceptedPostRoute())));
    }

    public static void verifyPhaseNavigation() {
        Path report = publish("rendered-api-phase-navigation.html", false);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var extractor = new EvidenceSectionExtractor(audit, LOCATORS);
            var page = audit.openTab(ReportUiContract.Tab.TESTS.hash());
            for (int repeat = 0; repeat < 2; repeat++) {
                var evidence = extractor.extractFirstRequestWithEvidence(page);
                assertTrue(evidence.tablesVisible(), () -> "request/response/execution evidence: " + evidence);
                assertEquals(1, evidence.requestTableCount());
                assertEquals(1, evidence.responseTableCount());
                assertEquals(1, evidence.executionTableCount());
            }
            var inspectedActions = audit.actions().snapshot();
            assertTrue(inspectedActions.size() > 5, "the evidence trace includes phase and request disclosure actions");
            assertSame(page, audit.evidencePage(ReportUiContract.Tab.TESTS));
            assertEquals(inspectedActions, audit.actions().snapshot(), "capture must not reset the action trace");
            var inspectedRequest = LOCATORS.requests(LOCATORS.visibleTestDetail(page)).first();
            assertTrue(LOCATORS.visibleResponseTable(inspectedRequest).isVisible(),
                    "capture must retain the expanded evidence rather than a freshly collapsed page");
            audit.actions().click(LOCATORS.requestNodeToggleAction(inspectedRequest),
                    "collapse API exchange and hide its evidence tables");
            audit.actions().waitForAttribute(page, LOCATORS.nodeCardAction(inspectedRequest),
                    "data-atf-node-open", "false", "API exchange is collapsed");
            assertEquals(0, LOCATORS.visibleRequestTable(inspectedRequest).count());
            assertEquals(0, LOCATORS.visibleResponseTable(inspectedRequest).count());
            assertEquals(0, LOCATORS.visibleExecutionTable(inspectedRequest).count());
            audit.actions().click(LOCATORS.requestNodeToggleAction(inspectedRequest),
                    "reopen API exchange and restore its evidence tables");
            audit.actions().waitForAttribute(page, LOCATORS.nodeCardAction(inspectedRequest),
                    "data-atf-node-open", "true", "API exchange is reopened");
            assertEquals(1, LOCATORS.visibleRequestTable(inspectedRequest).count(),
                    "request evidence must return visibly after reopening the exchange");
            assertEquals(1, LOCATORS.visibleResponseTable(inspectedRequest).count(),
                    "response evidence must return visibly after reopening the exchange");
            assertEquals(1, LOCATORS.visibleExecutionTable(inspectedRequest).count(),
                    "execution evidence must return visibly after reopening the exchange");
            audit.actions().click(LOCATORS.navTabAction(page, ReportUiContract.Tab.OVERVIEW), "leave Tests");
            page = audit.evidencePage(ReportUiContract.Tab.TESTS);
            assertTrue(audit.actions().snapshot().size() > inspectedActions.size());
            assertEquals(inspectedActions, audit.actions().snapshot().subList(0, inspectedActions.size()),
                    "cross-tab evidence navigation must retain the preceding audit actions");
            assertTrue(extractor.extractFirstRequestWithEvidence(page).tablesVisible());
            page = audit.openTab(ReportUiContract.Tab.TESTS.hash());
            assertTrue(extractor.extractFirstRequestWithEvidence(page).tablesVisible(), "fresh document navigation rebinds the phase");
            assertTrue(context.browserDiagnostics().valid(), () -> context.browserDiagnostics().violations().toString());
        }
    }

    public static void verifyMissingPhase() {
        Path report = publish("rendered-api-missing-test-body.html", false, ReportUiContract.Phase.SETUP);
        var policy = UiConfiguration.defaults(report.getParent()).withReportDirectory(report.getParent());
        try (var context = ReportAuditContext.open(policy, report, AUDIT)) {
            var audit = audit(context, policy);
            var page = audit.openTab(ReportUiContract.Tab.TESTS.hash());
            var evidence = new EvidenceSectionExtractor(audit, LOCATORS).extractFirstRequestWithEvidence(page);
            assertFalse(evidence.tablesVisible());
            assertEquals(0, evidence.requestTableCount());
            assertEquals(List.of("TEST BODY is missing"), evidence.jsonEvidence().violations());
        }
    }

    private static UiAuditFlow audit(ReportAuditContext context, UiConfiguration policy) {
        return new UiAuditFlow(context, policy, AUDIT, ApiReportRenderedEvidenceProbe.class.getPackageName(),
                List.of("component"), "the controlled API report is available", "Evidence audit failed", OWNER);
    }

    private static Path publish(String fileName, boolean missingRequestQuery) {
        return publish(fileName, missingRequestQuery, ReportUiContract.Phase.TEST_BODY);
    }

    private static Path publish(String fileName, boolean missingRequestQuery, ReportUiContract.Phase phaseKind) {
        Path report = TestReportPaths.report(fileName);
        var input = acceptedInput();
        var route = acceptedPostRoute();
        Duration elapsed = Duration.ofMillis(8);
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario(OWNER, "controlled API evidence", List.of("api", "component"));
            var phase = scenario.startPhase(phaseKind.displayName());
            var request = phase.startRequest(input.requestTitle(), HttpMethod.POST.value(), route.path());
            input.requestFields().forEach(request::detail);
            request.detail(ApiReportEvidenceField.REQUEST_URL.key(),
                    missingRequestQuery ? acceptedUrl().split("\\?")[0] : acceptedUrl());
            input.responseFields().entrySet().stream()
                    .filter(entry -> !ApiReportEvidenceField.CONTENT_TYPE.matches(entry.getKey()))
                    .forEach(entry -> request.detail(entry.getKey(), entry.getValue()));
            request.detail("response.content.type", JSON);
            request.json("REQUEST JSON", input.requestBody());
            request.json("RESPONSE JSON", input.responseBody());
            var response = new ReportResponse(route.status(), route.body(), route.headers(), elapsed, acceptedUrl());
            var transport = new ReportRequest(route.method(), route.path(), Map.of(), route.requiredHeaders(),
                    route.authProfile(), input.requestBody());
            request.execution(new ReportExecutionEvidence(response, 1, elapsed, List.of(),
                    List.of(new ReportAttemptRecord(1, route.status(), elapsed, "SUCCESS", "controlled fixture")),
                    transport, ReportTransportProvenance.ACCEPTANCE_MOCK));
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        return report;
    }

    private static Path publishWithSiblingOwner(String fileName) {
        Path report = TestReportPaths.report(fileName);
        var input = acceptedInput();
        var route = acceptedPostRoute();
        Duration elapsed = Duration.ofMillis(8);
        try (var session = new ExtentReportSession(report)) {
            publishOwner(session, OWNER, input, route, elapsed);
            publishOwner(session, SIBLING_OWNER, input, route, elapsed);
        }
        return report;
    }

    private static void publishOwner(ExtentReportSession session, String ownerName,
                                     ApiReportContractInput input, MockRoute route,
                                     Duration elapsed) {
        var scenario = session.startScenario(ownerName, "controlled API evidence", List.of("api", "component"));
        var phase = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        var request = phase.startRequest(ownerName + " request", HttpMethod.POST.value(), route.path());
        input.requestFields().forEach(request::detail);
        request.detail(ApiReportEvidenceField.REQUEST_URL.key(), acceptedUrl());
        input.responseFields().entrySet().stream()
                .filter(entry -> !ApiReportEvidenceField.CONTENT_TYPE.matches(entry.getKey()))
                .forEach(entry -> request.detail(entry.getKey(), entry.getValue()));
        request.detail("response.content.type", JSON);
        request.json("REQUEST JSON", input.requestBody());
        request.json("RESPONSE JSON", input.responseBody());
        var response = new ReportResponse(route.status(), route.body(), route.headers(), elapsed, acceptedUrl());
        var transport = new ReportRequest(route.method(), route.path(), Map.of(), route.requiredHeaders(),
                route.authProfile(), input.requestBody());
        request.execution(new ReportExecutionEvidence(response, 1, elapsed, List.of(),
                List.of(new ReportAttemptRecord(1, route.status(), elapsed, "SUCCESS", "controlled fixture")),
                transport, ReportTransportProvenance.ACCEPTANCE_MOCK));
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
    }
}
