package atf.shop.support.api.run;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.support.api.ShopApiReportValidation;
import atf.shop.support.reporting.ShopApiReportPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Audit exists before execution; final publication includes every observed exchange and failure exit. */
public final class ShopApiRunReport implements ShopApiBusinessRun.Audit {
    private record FailedRequest(ApiRequest request, Throwable failure) { }
    private final AtfReportPorts.Session session;
    private final AtfReportPorts.Scenario scenario;
    private final AtfReportPorts.Phase phase;
    private final AtfConsoleSink console;
    private final boolean requireLive;
    private final List<ApiExecutionResult> executions = new ArrayList<>();
    private final List<FailedRequest> failedRequests = new ArrayList<>();
    private final List<ShopApiReportValidation> validations = new ArrayList<>();
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    private Throwable failure;
    private boolean closed;

    public ShopApiRunReport(AtfReportPorts.Session session, String testClass, String testPackage,
                            String scenarioName, List<String> tags, boolean requireLive, AtfConsoleSink console) {
        this.session = Objects.requireNonNull(session);
        this.requireLive = requireLive;
        this.console = Objects.requireNonNull(console);
        AtfReportPorts.Scenario acquired = null;
        try {
            acquired = session.startScenario(testClass, testPackage, scenarioName, tags);
            scenario = acquired;
            phase = scenario.startPhase("TEST BODY");
        } catch (RuntimeException | Error error) {
            var acquiredScenario = acquired;
            try (session) {
                if (acquiredScenario != null) try (acquiredScenario) { acquiredScenario.complete(AtfReportStatus.FAIL); }
            } catch (RuntimeException | Error closeFailure) { if (error != closeFailure) error.addSuppressed(closeFailure); }
            throw error;
        }
    }

    @Override public void observed(ApiExecutionResult execution) { executions.add(Objects.requireNonNull(execution)); }
    @Override public void requestFailed(ApiRequest request, Throwable error) {
        failedRequests.add(new FailedRequest(request, error));
    }
    @Override public void validation(ShopApiReportValidation validation) { validations.add(validation); }
    @Override public void failed(Throwable error) { if (failure == null) failure = error; }

    @Override public void close() {
        if (closed) return;
        closed = true;
        try (session; scenario; phase) {
            try {
                boolean passed = failure == null && failedRequests.isEmpty();
                for (var execution : executions)
                    passed &= ShopApiReportPublisher.publish(phase, execution, console) == AtfReportStatus.PASS;
                for (var request : failedRequests) publishFailure(request);
                if (!executions.isEmpty()) ShopApiReportPublisher.publishTraceSummary(phase, executions);
                if (requireLive && !executions.isEmpty())
                    passed &= ShopApiReportPublisher.publishLiveProvenance(phase, executions) == AtfReportStatus.PASS;
                for (var validation : validations)
                    passed &= ShopApiReportPublisher.publishValidation(phase, validation) == AtfReportStatus.PASS;
                if (failure != null) phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, diagnostic(failure));
                var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                phase.complete(status);
                scenario.complete(status);
            } catch (RuntimeException | Error publicationFailure) {
                try { phase.complete(AtfReportStatus.FAIL); } catch (RuntimeException | Error secondary) {
                    if (publicationFailure != secondary) publicationFailure.addSuppressed(secondary);
                }
                try { scenario.complete(AtfReportStatus.FAIL); } catch (RuntimeException | Error secondary) {
                    if (publicationFailure != secondary) publicationFailure.addSuppressed(secondary);
                }
                throw publicationFailure;
            }
        }
    }

    private void publishFailure(FailedRequest failed) {
        var request = failed.request();
        try (var node = phase.startRequest("Failed API execution", request.method(), request.path())) {
            var captured = atf.core.http.execution.evidence.ApiExecutionFailure.find(failed.failure());
            if (captured.isPresent()) {
                var snapshot = captured.orElseThrow();
                atf.reporting.api.evidence.ApiExchangeEvidence.publishFailure(node,
                        atf.shop.support.api.ShopApiEvidenceMapper.toExecution(snapshot), snapshot.url(),
                        snapshot.termination().name(), diagnostic(failed.failure()));
                node.complete(AtfReportStatus.FAIL);
                return;
            }
            if (request.bodyText() != null && !request.bodyText().isBlank())
                node.json("REQUEST JSON", ShopJsonEvidence.redact(request.bodyText()));
            node.detail("Response availability", "No HTTP response received");
            node.detail("Execution failure", diagnostic(failed.failure()));
            node.complete(AtfReportStatus.FAIL);
        }
    }

    private String diagnostic(Throwable error) {
        Throwable cause = error;
        for (int depth = 0; depth < 4 && cause.getCause() != null && cause.getCause() != cause; depth++)
            cause = cause.getCause();
        String text = cause.toString() + (cause == error ? "" : " (during " + error + ")");
        String safe = redactor.redactText(text);
        return safe.length() <= 240 ? safe : safe.substring(0, 239) + "…";
    }
}
