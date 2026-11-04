package atf.reporting.ui;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Owns the completion boundary for one UI audit scenario.
 *
 * <p>This is reporting infrastructure, not browser infrastructure. The
 * caller supplies captured UI observations and actions; this component
 * guarantees that evidence is published before the phase and scenario are
 * completed, including the failure path.</p>
 */
public final class UiAuditLifecycle {
    private final UiExtentReportSession.UiScenario scenario;
    private final UiExtentReportSession.UiPhase body;
    private boolean bodyCompleted;

    public UiAuditLifecycle(UiExtentReportSession.UiScenario scenario,
                            UiExtentReportSession.UiPhase body) {
        this.scenario = Objects.requireNonNull(scenario, "scenario");
        this.body = Objects.requireNonNull(body, "body");
    }

    public void uiValidation(String title, String target, String screenshot,
                                 Collection<AtfAssertion> assertions,
                                 Collection<AtfUiActionRecord> actions,
                                 AtfReportStatus status) {
        body.uiValidation(title, target, screenshot,
                assertions == null ? List.of() : assertions,
                actions == null ? List.of() : actions, status);
    }

    public void pass() {
        complete(AtfReportStatus.PASS);
    }

    /** Completes the scenario with the already computed renderer-neutral status. */
    public void complete(AtfReportStatus status) {
        completeBody(Objects.requireNonNull(status, "status"));
        scenario.complete(status);
    }

    public void fail(String title, String target,
                     Collection<AtfUiActionRecord> executedActions,
                     Throwable failure) {
        fail(title, target, executedActions, failure, null);
    }

    public void fail(String title, String target,
                     Collection<AtfUiActionRecord> executedActions,
                     Throwable failure, String failureScreenshot) {
        body.failureEvidence(title, target,
                executedActions == null ? List.of() : executedActions, failure, failureScreenshot);
        completeBody(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private void completeBody(AtfReportStatus status) {
        if (!bodyCompleted) {
            body.complete(status);
            bodyCompleted = true;
        }
    }
}
