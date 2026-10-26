package atf.reporting.api.acceptance;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.reporting.api.ApiEvidenceField;
import atf.reporting.api.ApiJsonEvidence;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.api.ReportRequest;
import atf.reporting.support.CoreReportPortsAdapter;
import atf.reporting.model.ReportExecutionEvidence;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ApiValidationException;
import atf.validation.evaluation.ValidationFailureEvidence;
import atf.validation.publishing.ResponseContractReporter;
import atf.validation.evaluation.ValidationDecision;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

/** Test-scope publisher for API execution evidence and contract decisions. */
public final class ApiAcceptancePublisher {
    private static final ResponseContractReporter VALIDATOR = new ResponseContractReporter();

    private ApiAcceptancePublisher() { }

    public static void publish(AtfReportPorts.Phase phase, String title,
                               ApiExecutionResult execution, ResponseContract contract,
                               boolean warningMode, String requestJson) {
        publish(phase, title, execution, contract, warningMode, requestJson, null);
    }

    /** Publishes request-owned metadata when a scenario builds a custom route. */
    public static void publish(AtfReportPorts.Phase phase, String title,
                               ApiExecutionResult execution, ResponseContract contract,
                               boolean warningMode, String requestJson, ApiRequest sourceRequest) {
        publishDecisionAndSignal(phase, title, execution, contract, warningMode, requestJson,
                sourceRequest, null);
    }

    /** Publishes a response and appends flow-level assertions inside that request's evidence. */
    public static void publish(AtfReportPorts.Phase phase, String title,
                               ApiExecutionResult execution, ResponseContract contract,
                               boolean warningMode, String requestJson, ApiRequest sourceRequest,
                               Consumer<AtfReportPorts.Request> flowEvidence) {
        publishDecisionAndSignal(phase, title, execution, contract, warningMode, requestJson,
                sourceRequest, flowEvidence);
    }

    /** Publishes evidence while leaving the final decision to the calling JUnit test. */
    public static ValidationDecision publishDecision(
            AtfReportPorts.Phase phase, String title,
            ApiExecutionResult execution, ResponseContract contract,
            boolean warningMode, String requestJson, ApiRequest sourceRequest) {
        return publishDecisionInternal(phase, title, execution, contract, warningMode, requestJson,
                sourceRequest, null);
    }

    public static ValidationDecision publishDecision(
            AtfReportPorts.Phase phase, String title,
            ApiExecutionResult execution, ResponseContract contract,
            boolean warningMode, String requestJson, ApiRequest sourceRequest,
            Consumer<AtfReportPorts.Request> flowEvidence) {
        return publishDecisionInternal(phase, title, execution, contract, warningMode, requestJson,
                sourceRequest, flowEvidence);
    }

    /** Publishes a modeled negative outcome while retaining the normal JSON write contract. */
    public static ValidationDecision publishNegativeDecision(
            AtfReportPorts.Phase phase, String title,
            ApiExecutionResult execution, ResponseContract contract,
            boolean warningMode, ApiRequest sourceRequest) {
        return publishDecisionInternal(phase, title, execution, contract, warningMode, null,
                sourceRequest, null);
    }

    private static void publishDecisionAndSignal(AtfReportPorts.Phase phase, String title,
                                                  ApiExecutionResult execution,
                                                  ResponseContract contract, boolean warningMode,
                                                  String requestJson, ApiRequest sourceRequest,
                                                  Consumer<AtfReportPorts.Request> flowEvidence) {
        var decision = publishDecisionInternal(phase, title, execution, contract, warningMode,
                requestJson, sourceRequest, flowEvidence);
        if (decision.status() == AtfReportStatus.WARNING) {
            throw new ApiScenarioLifecycleRunner.WarningSignal(decision.message());
        }
        if (decision.status() == AtfReportStatus.FAIL) {
            throw failureSignal(decision, execution);
        }
    }

    private static ValidationDecision publishDecisionInternal(
            AtfReportPorts.Phase phase, String title,
            ApiExecutionResult execution, ResponseContract contract,
            boolean warningMode, String requestJson, ApiRequest sourceRequest,
            Consumer<AtfReportPorts.Request> flowEvidence) {
        var response = execution.response();
        ApiRequest capturedRequest = sourceRequest != null ? sourceRequest : execution.request();
        String method = capturedRequest == null ? executionRequestMethod(title) : capturedRequest.method();
        String path = capturedRequest == null ? URI.create(response.url()).getPath() : capturedRequest.path();
        var request = phase.startRequest(title, method, path);
        String effectiveRequestJson = requestJson;
        if (effectiveRequestJson == null && capturedRequest != null && !capturedRequest.bodyText().isBlank()) {
            effectiveRequestJson = capturedRequest.bodyText();
        }
        var adapted = ReportModelAdapters.execution(execution);
        var reportRequest = capturedRequest == null
                ? new ReportRequest(method, path, Map.of(), Map.of(), "none", effectiveRequestJson)
                : ReportModelAdapters.request(capturedRequest);
        reportRequest = new ReportRequest(reportRequest.method(), reportRequest.path(),
                reportRequest.query(), reportRequest.headers(), reportRequest.authProfile(),
                effectiveRequestJson == null ? reportRequest.bodyText() : effectiveRequestJson, reportRequest.authObservation());
        CoreReportPortsAdapter.publishEvidence(request,
                new ReportExecutionEvidence(adapted.response(), adapted.attempts(),
                        adapted.totalDuration(), adapted.retryEvents(), adapted.attemptHistory(), reportRequest, adapted.provenance()),
                execution);
        var jsonContractViolations = ApiJsonEvidence.writeContractViolations(
                method, effectiveRequestJson, response.body());
        if (!jsonContractViolations.isEmpty()) {
            String message = "JSON request/response contract failed for " + method + " " + path
                    + ": " + String.join("; ", jsonContractViolations);
            detail(request, ApiEvidenceField.JSON_CONTRACT, message);
            request.complete(AtfReportStatus.FAIL);
            request.close();
            throw new ApiScenarioLifecycleRunner.FailureSignal(message);
        }
        ValidationDecision decision;
        try {
            decision = VALIDATOR.validate(request, "Contract validation", response, contract,
                    warningMode, false);
            if (flowEvidence != null) {
                flowEvidence.accept(request);
            }
            request.complete(decision.status());
        } catch (ApiValidationException | RuntimeException failure) {
            request.complete(AtfReportStatus.FAIL);
            throw failure;
        } finally {
            request.close();
        }
        return decision;
    }

    private static String executionRequestMethod(String title) {
        return title.substring(0, title.indexOf(' '));
    }

    private static ApiScenarioLifecycleRunner.FailureSignal failureSignal(
            ValidationDecision decision,
            ApiExecutionResult execution) {
        ValidationFailureEvidence evidence = decision.failureEvidence();
        var details = new java.util.LinkedHashMap<>(evidence.details());
        details.put("Attempts", Integer.toString(execution.attempts()));
        return new ApiScenarioLifecycleRunner.FailureSignal(evidence.cause(), details);
    }

    private static void detail(AtfReportPorts.Request request, ApiEvidenceField field, String value) {
        request.detail(field.label(), value);
    }

}
