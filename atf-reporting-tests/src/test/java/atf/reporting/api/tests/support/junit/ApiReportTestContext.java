package atf.reporting.api.tests.support.junit;
import atf.reporting.api.acceptance.ApiAcceptancePublisher;

import atf.core.http.response.ApiExecutionResult;
import atf.core.http.request.ApiRequest;
import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.CoreReportPortsAdapter;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ValidationDecision;

import java.util.Collection;
import java.util.List;

/**
 * One direct-JUnit test's report scope. It owns only the test-to-report
 * adapter; API execution and contract evaluation remain in their modules.
 */
public final class ApiReportTestContext implements AutoCloseable {
    private static final ThreadLocal<ApiReportTestContext> CURRENT = new ThreadLocal<>();

    private final AtfReportPorts.Scenario scenario;
    private final AtfReportPorts.Phase body;
    private AtfReportStatus aggregate = AtfReportStatus.PASS;
    private boolean completed;

    private ApiReportTestContext(AtfReportPorts.Scenario scenario) {
        this.scenario = scenario;
        this.body = scenario.startPhase("TEST BODY");
    }

    public static ApiReportTestContext open(ExtentReportSession session,
                                            String testClass,
                                            String scenarioName,
                                            Collection<String> tags) {
        return open(session, testClass, null, scenarioName, tags);
    }

    public static ApiReportTestContext open(ExtentReportSession session,
                                            String testClass,
                                            String testPackage,
                                            String scenarioName,
                                            Collection<String> tags) {
        var context = new ApiReportTestContext(CoreReportPortsAdapter.wrap(session).startScenario(
                testClass, testPackage, scenarioName, tags == null ? List.of() : tags));
        CURRENT.set(context);
        return context;
    }

    public static ApiReportTestContext current() {
        return CURRENT.get();
    }

    public ValidationDecision publish(String title,
                                     ApiExecutionResult execution,
                                     ResponseContract contract,
                                     boolean warningMode,
                                     ApiRequest sourceRequest) {
        var decision = ApiAcceptancePublisher.publishDecision(
                body, title, execution, contract, warningMode, null, sourceRequest);
        aggregate = AtfReportStatus.worst(aggregate, decision.status());
        return decision;
    }

    public ValidationDecision publishNegative(String title, ApiExecutionResult execution,
                                              ResponseContract contract, boolean warningMode,
                                              ApiRequest sourceRequest) {
        var decision = ApiAcceptancePublisher.publishNegativeDecision(
                body, title, execution, contract, warningMode, sourceRequest);
        aggregate = AtfReportStatus.worst(aggregate, decision.status());
        return decision;
    }

    public void complete(AtfReportStatus status) {
        if (completed) return;
        var effective = AtfReportStatus.worst(aggregate,
                status == null ? AtfReportStatus.FAIL : status);
        body.complete(effective);
        scenario.complete(effective);
        completed = true;
        CURRENT.remove();
    }

    @Override
    public void close() {
        complete(AtfReportStatus.FAIL);
    }
}

