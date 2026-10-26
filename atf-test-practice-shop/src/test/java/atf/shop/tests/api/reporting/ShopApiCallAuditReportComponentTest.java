package atf.shop.tests.api.reporting;

import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.response.ApiAttemptOutcome;
import atf.api.core.http.response.ApiAttemptRecord;
import atf.api.core.http.response.ApiExecutionResult;
import atf.api.core.http.response.RetryEvent;
import atf.api.reporting.contract.AtfReportStatus;
import atf.api.reporting.extent.session.ExtentReportSession;
import atf.api.reporting.html.HtmlReportSession;
import atf.api.reporting.model.ReportExecutionModel;
import atf.api.reporting.semantic.AtfReportSemanticSnapshot;
import atf.api.reporting.api.ReportTransportProvenance;
import atf.api.testkit.AtfComponentTest;
import atf.shop.support.reporting.ShopApiReportPublisher;
import atf.shop.support.api.ShopApiEvidenceMapper;
import atf.shop.tests.api.support.ShopApiCallAuditContextTestSupport;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.api.contract.ShopApiHttpStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects consumer-owned API-call report composition, not renderer internals. */
@AtfComponentTest
class ShopApiCallAuditReportComponentTest {
    private static final String TEST_BODY = "TEST BODY";
    private static final String AUDIT_DETAIL = "API audit completeness";

    @Test
    void explicitExchangeContractIsPublishedFromItsOwnObservedStatus() throws Exception {
        List<ShopApiCallAuditContextTestSupport.CapturedExchange> exchanges = new ArrayList<>();
        ShopApiCallAuditContextTestSupport.recordWith(exchanges::add);
        ShopApiCallAuditContextTestSupport.expectNext("valid login", ShopApiHttpStatus.OK);
        var result = ShopApiComponentFixture.client().login(
                ShopApiScenarioFixture.validCredentials());
        assertTrue(result.authenticated());
        assertEquals(1, exchanges.size());

        var output = Files.createTempDirectory("shop-api-audit-component-").resolve("audit.html");
        try (var report = new ExtentReportSession(output)) {
            var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                    "local login publishes the observed exchange", List.of("api", "component"));
            var phase = scenario.startPhase(TEST_BODY);
            var captured = exchanges.getFirst();
            ShopApiReportPublisher.publishObserved(phase, captured.execution(), 1,
                    captured.contractValidation());
            ShopApiCallAuditTestSupport.publishAuditDetails(phase, null, exchanges.size(), 1, List.of());
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            var snapshot = report.semanticSnapshot();
            assertEquals(1, snapshot.requestCount());
            assertEquals(1, snapshot.validationCount());
            var request = (AtfReportSemanticSnapshot.RequestSnapshot) snapshot.classes().getFirst()
                    .scenarios().getFirst().phases().getFirst().nodes().getFirst();
            assertEquals(HttpMethod.POST.value(), request.method());
            assertEquals("/users/login", request.path());
            var validation = request.validations().getFirst();
            assertEquals("Contract validation", validation.title());
            assertEquals(List.of("HTTP status"), validation.assertionNames());
            assertEquals(AtfReportStatus.PASS, validation.status());
            assertEquals("valid login — OK (200)",
                    captured.contractValidation().assertions().getFirst().expected());
            assertEquals("200", captured.contractValidation().assertions().getFirst().actual());
        } finally {
            ShopApiCallAuditContextTestSupport.clear();
        }
    }

    @Test
    void sameEndpointCallsKeepDistinctIntentAndExchangeLocalContracts() throws Exception {
        List<ShopApiCallAuditContextTestSupport.CapturedExchange> exchanges = new ArrayList<>();
        ShopApiCallAuditContextTestSupport.recordWith(exchanges::add);
        ShopApiCallAuditContextTestSupport.expectNext(
                "sign in with valid credentials", "valid login", ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext(
                "verify invalid credentials are rejected", "invalid login", ShopApiHttpStatus.UNAUTHORIZED);
        try {
            var client = ShopApiComponentFixture.client();
            client.login(ShopApiScenarioFixture.validCredentials());
            client.login(ShopApiScenarioFixture.invalidCredentials());
            assertEquals(2, exchanges.size());
            var validLogin = exchanges.get(0).contractValidation().assertions().getFirst();
            var invalidLogin = exchanges.get(1).contractValidation().assertions().getFirst();
            assertEquals("valid login — OK (200)", validLogin.expected());
            assertEquals("200", validLogin.actual());
            assertTrue(validLogin.passed());
            assertEquals("invalid login — UNAUTHORIZED (401)", invalidLogin.expected());
            assertEquals("401", invalidLogin.actual());
            assertTrue(invalidLogin.passed());
            assertTrue(ShopApiCallAuditContextTestSupport.unconsumedExpectations().isEmpty());

            var output = Files.createTempDirectory("shop-api-audit-multi-").resolve("audit.html");
            try (var report = new ExtentReportSession(output)) {
                var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                        "multiple exchanges keep exchange-local contracts", List.of("api", "component"));
                var phase = scenario.startPhase(TEST_BODY);
                for (int index = 0; index < exchanges.size(); index++) {
                    var exchange = exchanges.get(index);
                    ShopApiReportPublisher.publishObserved(phase, exchange.execution(), index + 1L,
                            exchange.contractValidation());
                }
                ShopApiCallAuditTestSupport.publishAuditDetails(phase, null, exchanges.size(),
                        exchanges.size(), List.of());
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);

                var snapshot = report.semanticSnapshot();
                var scenarioSnapshot = snapshot.classes().getFirst().scenarios().getFirst();
                var phaseSnapshot = scenarioSnapshot.phases().getFirst();
                assertEquals(2, snapshot.requestCount());
                assertEquals(2, snapshot.validationCount());
                assertEquals(List.of(AUDIT_DETAIL), phaseSnapshot.detailNames(),
                        "capture completeness belongs to the phase, not each HTTP exchange");
                assertEquals(2, phaseSnapshot.nodes().size());
                var firstRequest = (AtfReportSemanticSnapshot.RequestSnapshot)
                        phaseSnapshot.nodes().get(0);
                var secondRequest = (AtfReportSemanticSnapshot.RequestSnapshot)
                        phaseSnapshot.nodes().get(1);
                assertEquals("POST /users/login — sign in with valid credentials", firstRequest.title());
                assertEquals("POST /users/login — verify invalid credentials are rejected", secondRequest.title());
                assertEquals(List.of("HTTP status"), firstRequest.validations().getFirst().assertionNames());
                assertEquals(List.of("HTTP status"), secondRequest.validations().getFirst().assertionNames());
            }
        } finally {
            ShopApiCallAuditContextTestSupport.clear();
        }
    }

    @Test
    void retryHistoryAndProvenanceSurviveShopMappingAndAuditPublication() throws Exception {
        List<ShopApiCallAuditContextTestSupport.CapturedExchange> exchanges = new ArrayList<>();
        ShopApiCallAuditContextTestSupport.recordWith(exchanges::add);
        ShopApiCallAuditContextTestSupport.expectNext(
                "sign in after transient response", "login succeeds", ShopApiHttpStatus.OK);
        try {
            ShopApiComponentFixture.client().login(ShopApiScenarioFixture.validCredentials());
            var original = exchanges.getFirst();
            var execution = original.execution();
            var retried = new ApiExecutionResult(execution.response(), 2, Duration.ofMillis(18),
                    List.of(new RetryEvent(1, 503, "Retry-After", Duration.ofMillis(5), "temporary overload")),
                    List.of(
                            new ApiAttemptRecord(1, 503, Duration.ofMillis(7),
                                    ApiAttemptOutcome.RETRYABLE_RESPONSE, "HTTP 503"),
                            new ApiAttemptRecord(2, 200, Duration.ofMillis(11),
                                    ApiAttemptOutcome.SUCCESS, "final response")),
                    execution.request(), execution.provenance());

            var adapted = ShopApiEvidenceMapper.toExecution(retried);
            assertEquals(2, adapted.attempts());
            assertEquals(Duration.ofMillis(18), adapted.totalDuration());
            assertEquals(List.of(503, 200), adapted.attemptHistory().stream()
                    .map(attempt -> attempt.statusCode()).toList());
            assertEquals(List.of("RETRYABLE_RESPONSE", "SUCCESS"), adapted.attemptHistory().stream()
                    .map(attempt -> attempt.outcome()).toList());
            assertEquals(List.of("HTTP 503", "final response"), adapted.attemptHistory().stream()
                    .map(attempt -> attempt.reason()).toList());
            assertEquals(1, adapted.retryEvents().size());
            assertEquals("temporary overload", adapted.retryEvents().getFirst().reason());
            assertEquals(Duration.ofMillis(5), adapted.retryEvents().getFirst().delay());
            assertEquals(ReportTransportProvenance.ACCEPTANCE_MOCK, adapted.provenance());

            var output = Files.createTempDirectory("shop-api-audit-retry-").resolve("audit.html");
            try (var report = new HtmlReportSession(output)) {
                var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                        "retry evidence remains exchange-local", List.of("api", "component"));
                var phase = scenario.startPhase(TEST_BODY);
                ShopApiReportPublisher.publishObserved(phase, retried, 1,
                        original.contractValidation());
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);

                var request = (ReportExecutionModel.Request) report.executionModel()
                        .scenarios().getFirst().phases().getFirst().children().getFirst();
                assertEquals(HttpMethod.POST.value(), request.method());
                assertEquals("/users/login", request.path());
                assertEquals(adapted.attempts(), request.execution().attempts());
                assertEquals(adapted.totalDuration(), request.execution().totalDuration());
                assertEquals(adapted.provenance(), request.execution().provenance());
                assertEquals(adapted.attemptHistory(), request.execution().attemptHistory());
                assertEquals(adapted.retryEvents(), request.execution().retryEvents());
                var validation = (ReportExecutionModel.Validation) request.children().getFirst();
                assertEquals(original.contractValidation().assertions(), validation.assertions());
                assertEquals(AtfReportStatus.PASS, validation.status());
            }
        } finally {
            ShopApiCallAuditContextTestSupport.clear();
        }
    }

    @Test
    void mismatchedExplicitContractIsReportedAsFailedForThatExchange() throws Exception {
        List<ShopApiCallAuditContextTestSupport.CapturedExchange> exchanges = new ArrayList<>();
        ShopApiCallAuditContextTestSupport.recordWith(exchanges::add);
        ShopApiCallAuditContextTestSupport.expectNext("wrongly expected unauthorized", ShopApiHttpStatus.UNAUTHORIZED);
        try {
            ShopApiComponentFixture.client().login(ShopApiScenarioFixture.validCredentials());
            var captured = exchanges.getFirst();
            var output = Files.createTempDirectory("shop-api-audit-mismatch-").resolve("audit.html");
            try (var report = new ExtentReportSession(output)) {
                var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                        "explicit contract mismatch remains exchange-local", List.of("api", "component"));
                var phase = scenario.startPhase(TEST_BODY);
                ShopApiReportPublisher.publishObserved(phase, captured.execution(), 1,
                        captured.contractValidation());
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
                var request = (AtfReportSemanticSnapshot.RequestSnapshot) report.semanticSnapshot()
                        .classes().getFirst().scenarios().getFirst().phases().getFirst().nodes().getFirst();
                assertEquals(AtfReportStatus.FAIL, request.status());
                assertEquals(AtfReportStatus.FAIL, request.validations().getFirst().status());
                assertEquals("wrongly expected unauthorized — UNAUTHORIZED (401)",
                        captured.contractValidation().assertions().getFirst().expected());
                assertEquals("200", captured.contractValidation().assertions().getFirst().actual());
            }
        } finally {
            ShopApiCallAuditContextTestSupport.clear();
        }
    }

    @Test
    void capturedExchangeWithoutTypedContractDoesNotInventValidation() throws Exception {
        List<ShopApiCallAuditContextTestSupport.CapturedExchange> exchanges = new ArrayList<>();
        ShopApiCallAuditContextTestSupport.recordWith(exchanges::add);
        try {
            ShopApiComponentFixture.client().login(ShopApiScenarioFixture.validCredentials());
            var output = Files.createTempDirectory("shop-api-audit-untyped-").resolve("audit.html");
            try (var report = new ExtentReportSession(output)) {
                var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                        "captured exchange without typed contract", List.of("api", "component"));
                var phase = scenario.startPhase(TEST_BODY);
                ShopApiReportPublisher.publishObserved(phase, exchanges.getFirst().execution(), 1,
                        exchanges.getFirst().contractValidation());
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
                var snapshot = report.semanticSnapshot();
                assertEquals(1, snapshot.requestCount());
                assertEquals(0, snapshot.validationCount());
                var request = (AtfReportSemanticSnapshot.RequestSnapshot) snapshot.classes().getFirst()
                        .scenarios().getFirst().phases().getFirst().nodes().getFirst();
                assertEquals(HttpMethod.POST.value(), request.method());
                assertEquals("/users/login", request.path());
            }
        } finally {
            ShopApiCallAuditContextTestSupport.clear();
        }
    }

    @Test
    void missingExchangeDoesNotRewritePassingJUnitVerdictOrInventAContract() throws Exception {
        var output = Files.createTempDirectory("shop-api-audit-empty-").resolve("audit.html");
        try (var report = new ExtentReportSession(output)) {
            var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                    "passing test with no captured exchange", List.of("api", "component"));
            var phase = scenario.startPhase(TEST_BODY);
            ShopApiCallAuditTestSupport.publishAuditDetails(phase, null, 0, 0, List.of());
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);

            var snapshot = report.semanticSnapshot();
            var scenarioSnapshot = snapshot.classes().getFirst().scenarios().getFirst();
            assertEquals(AtfReportStatus.PASS, scenarioSnapshot.status());
            assertEquals(0, snapshot.requestCount());
            assertEquals(0, snapshot.validationCount());
            assertEquals(List.of(AUDIT_DETAIL), scenarioSnapshot.phases().getFirst().detailNames());
        }
    }

    @Test
    void failedJUnitOutcomeRemainsScenarioFailureWithItsActualMessage() throws Exception {
        var output = Files.createTempDirectory("shop-api-audit-failure-").resolve("audit.html");
        try (var report = new ExtentReportSession(output)) {
            var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                    "failed assertion", List.of("api", "component"));
            var phase = scenario.startPhase(TEST_BODY);
            ShopApiCallAuditTestSupport.publishAuditDetails(
                    phase, new AssertionError("observed status mismatch"), 1, 0, List.of());
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);

            var scenarioSnapshot = report.semanticSnapshot().classes().getFirst().scenarios().getFirst();
            assertEquals(AtfReportStatus.FAIL, scenarioSnapshot.status());
            assertEquals(List.of(AUDIT_DETAIL, "JUnit failure"),
                    scenarioSnapshot.phases().getFirst().detailNames());
        }

        var html = Files.readString(output);
        assertTrue(html.contains("AssertionError: observed status mismatch"));
    }

    @Test
    void unconsumedAssociationIsDiagnosedWithoutInventingAnExchangeOrChangingJUnitVerdict() throws Exception {
        var output = Files.createTempDirectory("shop-api-audit-association-").resolve("audit.html");
        try (var report = new ExtentReportSession(output)) {
            var scenario = report.startScenario("ShopApiClientContractTest", "api.reporting",
                    "missing contract association", List.of("api", "component"));
            var phase = scenario.startPhase(TEST_BODY);
            ShopApiCallAuditTestSupport.publishAuditDetails(phase, null, 0, 0,
                    List.of("expected login contract; token=unpublished-secret"));
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            var snapshot = report.semanticSnapshot();
            assertEquals(AtfReportStatus.PASS, snapshot.classes().getFirst().scenarios().getFirst().status());
            assertEquals(0, snapshot.requestCount());
            assertEquals(0, snapshot.validationCount());
        }
        String html = Files.readString(output);
        assertTrue(html.contains("expected login contract; token=***REDACTED***"));
        assertFalse(html.contains("unpublished-secret"));
    }

}
