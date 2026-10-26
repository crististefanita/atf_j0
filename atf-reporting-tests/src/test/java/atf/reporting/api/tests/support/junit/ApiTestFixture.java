package atf.reporting.api.tests.support.junit;
import atf.core.reporting.contract.AtfReportStatus;
import atf.ui.tests.extent.support.config.ReportingTestResources;


import atf.core.http.response.ApiExecutionResult;
import atf.console.junit.AtfConsoleTestContext;
import atf.console.AtfConsoleReporter;
import atf.reporting.api.execution.ApiModule;
import atf.reporting.api.execution.ApiModuleFactory;
import atf.core.testing.http.RecordingApiTransport;
import atf.core.config.source.ApiEnvironment;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ValidationDecision;
import atf.validation.evaluation.ValidationEvaluator;

/**
 * Explicit composition root for readable offline API tests.
 *
 * <p>The fixture exposes the production API module and the validation boundary
 * without hiding the scenario's service call or contract. It is the Java
 * equivalent of a small test base, but uses composition so each test remains
 * clear about the layer it exercises.</p>
 */
public final class ApiTestFixture {
    private final ApiModule api;
    private final ValidationEvaluator evaluator;
    private final RecordingApiTransport recordingTransport;

    public ApiTestFixture() {
        var lifecycle = AtfConsoleTestContext.current();
        recordingTransport = new RecordingApiTransport(ReportingTestResources.mockTransport());
        api = ApiModuleFactory.fromTransport(recordingTransport, ApiEnvironment.MOCK_BASE_URI,
                lifecycle == null ? AtfConsoleReporter.configuredStdout() : lifecycle.console());
        evaluator = new ValidationEvaluator();
    }

    public ApiModule api() {
        return api;
    }

    public ValidationDecision validate(ApiExecutionResult execution, ResponseContract contract) {
        return validate("API response contract", execution, contract, false);
    }

    public ValidationDecision validate(ApiExecutionResult execution, ResponseContract contract,
                                       boolean warningMode) {
        return validate("API response contract", execution, contract, warningMode);
    }

    public ValidationDecision validate(String reportTitle, ApiExecutionResult execution,
                                       ResponseContract contract, boolean warningMode) {
        var lifecycle = AtfConsoleTestContext.current();
        String validationTitle = contract.ruleNames().size() == 1
                ? "Validate: " + contract.ruleNames().get(0)
                : "Validate response contract";
        if (lifecycle != null) {
            lifecycle.startValidation(validationTitle);
            lifecycle.detail("actual", Integer.toString(execution.response().status()));
        }
        try {
            var report = ApiReportTestContext.current();
            ValidationDecision decision = report == null
                    ? evaluator.evaluate(contract, execution.response(), warningMode)
                    : report.publish(reportTitle, execution, contract, warningMode,
                    recordingTransport.lastRequest());
            if (lifecycle != null) {
                lifecycle.recordStatus(decision.status());
                lifecycle.completeValidation(validationTitle, decision.status());
            }
            return decision;
        } catch (RuntimeException error) {
            if (lifecycle != null) {
                lifecycle.completeValidation(validationTitle,
                        AtfReportStatus.FAIL);
            }
            throw error;
        }
    }

    public ValidationDecision validateNegative(String reportTitle, ApiExecutionResult execution,
                                               ResponseContract contract) {
        var lifecycle = AtfConsoleTestContext.current();
        if (lifecycle != null) lifecycle.startValidation("Validate negative wire contract");
        try {
            var report = ApiReportTestContext.current();
            ValidationDecision decision = report == null
                    ? evaluator.evaluate(contract, execution.response(), false)
                    : report.publishNegative(reportTitle, execution, contract, false,
                    recordingTransport.lastRequest());
            if (lifecycle != null) {
                lifecycle.recordStatus(decision.status());
                lifecycle.completeValidation("Validate negative wire contract", decision.status());
            }
            return decision;
        } catch (RuntimeException error) {
            if (lifecycle != null) lifecycle.completeValidation("Validate negative wire contract",
                    AtfReportStatus.FAIL);
            throw error;
        }
    }
}
