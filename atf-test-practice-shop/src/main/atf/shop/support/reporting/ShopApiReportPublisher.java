package atf.shop.support.reporting;

import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.core.http.transport.ApiTransportProvenance;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.ShopApiRoutes;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.support.api.ShopApiEvidenceMapper;
import atf.shop.support.api.ShopApiReportValidation;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Publishes one Practice Shop API execution as a complete Extent evidence node. */
public final class ShopApiReportPublisher {
    private ShopApiReportPublisher() { }

    /**
     * Owns the standalone report lifecycle so component tests only describe
     * the API evidence they want to publish. Close/flush ordering stays here,
     * beside the publisher that owns the report nodes.
     */
    public static Path publishStandalone(Path reportPath,
                                         String testClass,
                                         String testPackage,
                                         String scenarioName,
                                         List<String> tags,
                                         List<ApiExecutionResult> executions,
                                         AtfConsoleSink console) {
        return publishStandalone(reportPath, testClass, testPackage, scenarioName, tags,
                executions, List.of(), false, console);
    }

    /**
     * Publishes the authoritative business artifact. It is deliberately a
     * separate entry point: deterministic component evidence must never be
     * mistaken for a live business verdict.
     */
    public static Path publishLiveStandalone(Path reportPath,
                                             String testClass,
                                             String testPackage,
                                             String scenarioName,
                                             List<String> tags,
                                             List<ApiExecutionResult> executions,
                                             List<ShopApiReportValidation> validations,
                                             AtfConsoleSink console) {
        return publishStandalone(reportPath, testClass, testPackage, scenarioName, tags,
                executions, validations, true, console);
    }

    private static Path publishStandalone(Path reportPath,
                                         String testClass,
                                         String testPackage,
                                         String scenarioName,
                                         List<String> tags,
                                         List<ApiExecutionResult> executions,
                                         List<ShopApiReportValidation> validations,
                                         boolean requireLive,
                                         AtfConsoleSink console) {
        Objects.requireNonNull(reportPath, "reportPath");
        Objects.requireNonNull(executions, "executions");
        Objects.requireNonNull(console, "console");
        // Match the configured Extent settings used by UI reports while
        // retaining the API-specific output path and evidence tree.
        try (ExtentReportSession session = new ExtentReportSession(reportPath,
                AtfReportConfigurationLoader.load().reportingSettings())) {
            var scenario = session.startScenario(testClass, testPackage, scenarioName, tags);
            var phase = scenario.startPhase("TEST BODY");
            boolean passed = true;
            try {
                long sequence = 0;
                for (ApiExecutionResult execution : executions) {
                    passed &= publish(phase, execution, ++sequence, console) == AtfReportStatus.PASS;
                }
                publishTraceSummary(phase, executions);
                if (requireLive) {
                    passed &= publishLiveProvenance(phase, executions) == AtfReportStatus.PASS;
                }
                for (ShopApiReportValidation validation : validations) {
                    passed &= publishValidation(phase, validation) == AtfReportStatus.PASS;
                }
                AtfReportStatus status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                phase.complete(status);
                scenario.complete(status);
            } finally {
                phase.close();
                scenario.close();
            }
            return session.reportPath();
        }
    }

    public static AtfReportStatus publish(AtfReportPorts.Phase phase, ApiExecutionResult execution) {
        return publish(phase, execution, AtfConsoleSink.noOp());
    }

    /**
     * Publishes only the observed exchange and its explicitly supplied contract.
     * A JUnit verdict or an observed status alone is never treated as an HTTP assertion.
     */
    public static void publishObserved(AtfReportPorts.Phase phase, ApiExecutionResult execution,
                                       long sequence, ShopApiReportValidation contractValidation) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(execution, "execution");
        var request = execution.request();
        var response = execution.response();
        var operation = ShopApiOperation.resolve(request.method(), request.path());
        String intent = contractValidation == null ? "" : contractValidation.operationIntent();
        String title = ShopApiOperation.reportTitle(request.method(), request.path(), intent);
        var evidence = ShopApiEvidenceMapper.toExecution(execution, sequence,
                phaseFor(request.path()), "ApiCallComponentTest", phaseFor(request.path()),
                title, execution.provenance().reportLabel(),
                expectedContractSummary(contractValidation), "status=" + response.status());
        var node = phase.startRequest(title, request.method(), request.path());
        try {
            node.detail("request.operation.kind", operation.name());
            if (!operation.routeTemplate().isBlank()) {
                node.detail("request.route.template", operation.routeTemplate());
            }
            ApiExchangeEvidence.publish(node, evidence);
            if (contractValidation == null) {
                node.complete(AtfReportStatus.INFO);
            } else {
                var validation = node.startValidation("Contract validation");
                try {
                    validation.assertions(contractValidation.assertions());
                    AtfReportStatus status = contractValidation.passed()
                            ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                    validation.complete(status);
                    node.complete(status);
                } finally {
                    validation.close();
                }
            }
        } finally {
            node.close();
        }
    }

    private static String expectedContractSummary(ShopApiReportValidation validation) {
        if (validation == null) return "No typed HTTP contract supplied";
        return atf.reporting.evidence.diagnostic.ReportAssertionSummary.expected(validation.assertions());
    }

    /**
     * Publishes API evidence and optionally mirrors the exchange lifecycle to
     * the configured ATF console. Console output is diagnostic only and never
     * changes the report verdict.
     */
    public static AtfReportStatus publish(AtfReportPorts.Phase phase, ApiExecutionResult execution,
                               AtfConsoleSink console) {
        return publish(phase, execution, 0, console);
    }

    private static AtfReportStatus publish(AtfReportPorts.Phase phase, ApiExecutionResult execution,
                                           long sequence, AtfConsoleSink console) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(execution, "execution");
        Objects.requireNonNull(console, "console");
        var request = execution.request();
        var response = execution.response();
        var responseBody = response.body();
        var requestBody = request.bodyText();
        var redactedRequestBody = ShopApiEvidenceMapper.redactJson(requestBody);
        var redactedResponseBody = ShopApiEvidenceMapper.redactJson(responseBody);
        var responseContentType = String.valueOf(response.header(HttpHeader.CONTENT_TYPE.value()));
        ShopApiOperation operation = ShopApiOperation.resolve(
                request.method(), request.path());
        String requestName = ShopApiOperation.reportTitle(request.method(), request.path());
        var evidence = ShopApiEvidenceMapper.toExecution(execution, sequence,
                phaseFor(request.path()), "BusinessFlow", phaseFor(request.path()),
                requestName, execution.provenance().reportLabel(),
                "2xx response with JSON contract", "status=" + response.status());
        long requestStartedAt = System.nanoTime();
        consolePublish(console, AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST, requestName));
        consolePublish(console, AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "auth.profile", request.authProfile()));
        consolePublish(console, AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "auth.type", evidence.request().authMethod().wireValue()));
        consolePublish(console, AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "transport", execution.provenance().reportLabel()));
        AtfReportPorts.Request node = phase.startRequest(
                requestName, request.method(), request.path());
        AtfReportStatus requestStatus = AtfReportStatus.FAIL;
        boolean validationStarted = false;
        boolean validationCompleted = false;
        try {
            node.detail("request.operation.kind", operation.name());
            if (!operation.routeTemplate().isBlank()) {
                node.detail("request.route.template", operation.routeTemplate());
            }
            ApiExchangeEvidence.publish(node, evidence);
            AtfReportPorts.Validation validation = node.startValidation("Contract validation");
            validationStarted = true;
            long validationStartedAt = System.nanoTime();
            consolePublish(console, AtfConsoleEvent.started(AtfConsoleEvent.Scope.VALIDATION,
                    "Contract validation"));
            try {
                boolean responseIsEmpty = responseBody == null || responseBody.isBlank();
                boolean noContent = ShopApiHttpStatus.NO_CONTENT.matches(response.status());
                boolean requestNeedsJson = requiresJsonRequest(request.method());
                List<AtfAssertion> assertions = List.of(
                        new AtfAssertion("HTTP status", "2xx", Integer.toString(response.status()),
                                ShopApiHttpStatus.isSuccess(response.status())),
                        new AtfAssertion("Request JSON", requestNeedsJson
                                ? "non-empty parseable JSON" : "not required for " + request.method(),
                                requestBody.isBlank() ? "empty" : redactedRequestBody,
                                !requestNeedsJson || ShopJsonEvidence.isJson(requestBody)),
                        new AtfAssertion(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON,
                                responseContentType,
                                isJsonResponse(response)),
                        new AtfAssertion("Response JSON", noContent
                                ? "204 may have no body" : "non-empty parseable JSON",
                                redactedResponseBody, noContent || (!responseIsEmpty
                                && ShopJsonEvidence.isJson(responseBody))));
                validation.assertions(assertions);
                boolean passed = assertions.stream().allMatch(AtfAssertion::passed);
                requestStatus = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                validation.complete(requestStatus);
                validationCompleted = true;
                consolePublish(console, AtfConsoleEvent.completed(AtfConsoleEvent.Scope.VALIDATION,
                        "Contract validation", consoleStatus(requestStatus), elapsedMillis(validationStartedAt)));
                node.complete(requestStatus);
            } finally {
                validation.close();
            }
        } finally {
            if (validationStarted && !validationCompleted) {
                consolePublish(console, AtfConsoleEvent.completed(AtfConsoleEvent.Scope.VALIDATION,
                        "Contract validation", consoleStatus(AtfReportStatus.FAIL), 0));
            }
            consolePublish(console, AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                    requestName, consoleStatus(requestStatus), elapsedMillis(requestStartedAt)));
            node.close();
        }
        return requestStatus;
    }

    /** Publishes the compact projection without discarding the ordered exchanges above. */
    public static void publishTraceSummary(AtfReportPorts.Phase phase,
                                            List<ApiExecutionResult> executions) {
        var node = phase.startRequest("TRACE — summarize unique request signatures",
                "TRACE", "api-trace");
        try {
            List<ReportExecutionEvidence> evidence = new java.util.ArrayList<>();
            long sequence = 0;
            for (ApiExecutionResult execution : executions) {
                evidence.add(ShopApiEvidenceMapper.toExecution(execution, ++sequence,
                        phaseFor(execution.request().path()), "BusinessFlow", phaseFor(execution.request().path()),
                        ShopApiOperation.reportTitle(execution.request().method(), execution.request().path()),
                        execution.provenance().reportLabel(), "2xx response with JSON contract",
                        "status=" + execution.response().status()));
            }
            node.detail("trace.contract", "logical signature = method + safe URL + canonical JSON body digest");
            node.detail("trace.observations", Integer.toString(executions.size()));
            node.networkAggregate(ReportNetworkInspector.aggregate(evidence, List.of()));
            node.complete(AtfReportStatus.PASS);
        } finally {
            node.close();
        }
    }

    public static AtfReportStatus publishLiveProvenance(AtfReportPorts.Phase phase,
                                                          List<ApiExecutionResult> executions) {
        var node = phase.startRequest("LIVE_HTTP provenance", "ASSERT", "transport");
        AtfReportStatus status = AtfReportStatus.FAIL;
        try {
            boolean passed = !executions.isEmpty() && executions.stream()
                    .allMatch(execution -> execution.provenance() == ApiTransportProvenance.LIVE_HTTP);
            String observed = executions.stream()
                    .map(execution -> execution.provenance().reportLabel())
                    .distinct().toList().toString();
            var validation = node.startValidation("Transport provenance");
            try {
                validation.assertions(List.of(new AtfAssertion(
                        "Network provenance", "LIVE_HTTP for every exchange", observed, passed)));
                status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                validation.complete(status);
                node.complete(status);
            } finally {
                validation.close();
            }
        } finally {
            node.close();
        }
        return status;
    }

    public static AtfReportStatus publishValidation(AtfReportPorts.Phase phase,
                                                       ShopApiReportValidation reportValidation) {
        return atf.reporting.audit.AssertionAuditReport.publishStep(phase, reportValidation.title(),
                reportValidation.assertions(), reportValidation.passed() ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
    }

    private static long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private static void consolePublish(AtfConsoleSink console, AtfConsoleEvent event) {
        try {
            console.publish(event);
        } catch (RuntimeException ignored) {
            // The console is a best-effort diagnostic projection only.
        }
    }

    private static atf.core.reporting.contract.AtfReportStatus consoleStatus(
            AtfReportStatus status) {
        return atf.core.reporting.contract.AtfReportStatus.valueOf(status.name());
    }

    private static boolean requiresJsonRequest(String method) {
        return HttpMethod.POST.value().equalsIgnoreCase(method)
                || HttpMethod.PUT.value().equalsIgnoreCase(method);
    }

    private static boolean isJsonResponse(ApiResponse response) {
        if (ShopApiHttpStatus.NO_CONTENT.matches(response.status())) return true;
        return MediaTypes.isJson(response.header(HttpHeader.CONTENT_TYPE.value()));
    }

    private static String phaseFor(String path) {
        if (path == null) return "unknown";
        if (path.startsWith(ShopApiRoutes.LOGIN)) return "authentication";
        if (path.startsWith(ShopApiRoutes.PRODUCTS)
                || path.startsWith(ShopApiRoutes.PRODUCT_COLLECTION)) return "catalog";
        if (path.startsWith(ShopApiRoutes.CARTS)) return "cart";
        if (path.startsWith(ShopApiRoutes.PAYMENT_CHECK)
                || path.startsWith(ShopApiRoutes.POSTCODE_LOOKUP)) return "checkout";
        return "unknown";
    }

}
