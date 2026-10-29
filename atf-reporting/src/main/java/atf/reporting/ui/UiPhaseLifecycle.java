package atf.reporting.ui;
import atf.reporting.ui.timing.UiPhaseExecution;
import atf.reporting.model.timing.ReportPhaseTiming;

import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.model.ReportExecutionEvidence;

import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.network.ReportNetworkCaptureStatus;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Owns phase state and delegates all evidence publication to one publisher. */
public final class UiPhaseLifecycle implements AutoCloseable {
    private final AtfReportPorts.Phase delegate;
    private final UiEvidencePublisher evidencePublisher;
    private final ReportEvidenceFlowMode flowMode;
    private final String scenarioTitle;
    private final String phaseName;
    private final UiScenarioLifecycle scenario;
    private final List<UiUnifiedFlowCheckpoint> unifiedCheckpoints = new ArrayList<>();
    private PendingNetworkSummary networkSummary;
    private boolean completed;
    private ReportPhaseTiming suppliedTiming;
    private UiPhaseExecution execution;
    private boolean unifiedFlowPublished;

    UiPhaseLifecycle(AtfReportPorts.Phase delegate,
                     ReportScreenshotPolicy screenshotPolicy,
                     ReportEvidenceFlowMode flowMode,
                     ReportUiActionDetailsMode detailsMode,
                     String scenarioTitle,
                     String phaseName,
                     UiScenarioLifecycle scenario) {
        this.delegate = delegate;
        this.evidencePublisher = new UiEvidencePublisher(delegate, screenshotPolicy, detailsMode);
        this.flowMode = flowMode;
        this.scenarioTitle = scenarioTitle == null ? "" : scenarioTitle.trim();
        this.phaseName = phaseName == null ? "" : phaseName.trim();
        this.scenario = scenario;
    }

    void executionTiming(ReportPhaseTiming timing) {
        ensureOpen();
        if (suppliedTiming != null || execution != null) throw new IllegalStateException("UI phase timing is already supplied");
        suppliedTiming = java.util.Objects.requireNonNull(timing, "timing");
    }

    UiPhaseExecution measureExecution(java.util.function.LongSupplier ticks) {
        ensureOpen();
        if (suppliedTiming != null || execution != null) throw new IllegalStateException("UI phase timing is already supplied");
        execution = new UiPhaseExecution(ticks);
        return execution;
    }

    private void publish(Runnable work) {
        if (execution == null) work.run(); else execution.publish(work);
    }

    void step(String keyword, String title, String target, String base64Png,
              AtfReportStatus status) {
        step(keyword, title, target, base64Png, List.of(), status);
    }

    void step(AtfGherkinKeyword keyword, String title, String target, String base64Png,
              Collection<AtfAssertion> uiAssertions, Collection<AtfUiActionRecord> actions,
              UiApiValidationEvidence apiValidation,
              ReportScreenshotPolicy.CapturePoint capturePoint, AtfReportStatus status) {
        action(keyword.text(), title, target, base64Png, uiAssertions, status,
                UiValidationKind.STANDARD, actions, apiValidation, capturePoint);
    }

    void step(AtfGherkinKeyword keyword, String title, String target, String base64Png,
              AtfReportStatus status) {
        step(keyword.text(), title, target, base64Png, status);
    }

    void step(String keyword, String title, String target, String base64Png,
              Collection<AtfAssertion> assertions, AtfReportStatus status) {
        action(keyword, title, target, base64Png, assertions, status,
                UiValidationKind.STANDARD, List.of());
    }

    void step(AtfGherkinKeyword keyword, String title, String target, String base64Png,
              Collection<AtfAssertion> assertions, AtfReportStatus status) {
        step(keyword.text(), title, target, base64Png, assertions, status);
    }

    void step(AtfGherkinKeyword keyword, String title, String target, String base64Png,
              Collection<AtfAssertion> assertions, Collection<AtfUiActionRecord> actions,
              AtfReportStatus status) {
        action(keyword.text(), title, target, base64Png, assertions, status,
                UiValidationKind.STANDARD, actions, ReportScreenshotPolicy.CapturePoint.CHECKPOINT);
    }

    void step(AtfGherkinKeyword keyword, String title, String target, String base64Png,
              Collection<AtfAssertion> assertions, Collection<AtfUiActionRecord> actions,
              ReportScreenshotPolicy.CapturePoint capturePoint, AtfReportStatus status) {
        action(keyword.text(), title, target, base64Png, assertions, status,
                UiValidationKind.STANDARD, actions, capturePoint);
    }

    void setup(String keyword, String title, String target,
               Collection<AtfAssertion> assertions, AtfReportStatus status) {
        setup(keyword, title, target, assertions, List.of(), UiApiValidationEvidence.empty(), status);
    }

    void setup(String keyword, String title, String target,
               Collection<AtfAssertion> assertions,
               Collection<AtfUiActionRecord> actions,
               UiApiValidationEvidence apiValidation,
               AtfReportStatus status) {
        setup(keyword, title, target, assertions, actions, apiValidation, null, status);
    }

    void setup(String keyword, String title, String target,
               Collection<AtfAssertion> assertions,
               Collection<AtfUiActionRecord> actions,
               UiApiValidationEvidence apiValidation,
               ReportScreenshot screenshot,
               AtfReportStatus status) {
        ensureOpen();
        java.util.Objects.requireNonNull(apiValidation, "apiValidation");
        if (scenario.crossPhaseUnifiedFlow()) {
            unifiedCheckpoints.add(new UiUnifiedFlowCheckpoint(keyword, title, target,
                    screenshot == null ? "" : screenshot.base64Png(), immutableCopy(assertions),
                    status, UiValidationKind.SETUP, immutableCopy(actions), apiValidation,
                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT, false));
            scenario.retainUiFailures(assertions);
            return;
        }
        publish(() -> evidencePublisher.publishSetup(keyword, title, target, assertions, actions, apiValidation, screenshot, status));
        scenario.retainUiFailures(assertions);
    }

    void setup(AtfGherkinKeyword keyword, String title, String target,
               Collection<AtfAssertion> assertions, AtfReportStatus status) {
        setup(keyword.text(), title, target, assertions, status);
    }

    void setup(AtfGherkinKeyword keyword, String title, String target,
               Collection<AtfAssertion> assertions,
               Collection<AtfUiActionRecord> actions,
               UiApiValidationEvidence apiValidation,
               AtfReportStatus status) {
        setup(keyword.text(), title, target, assertions, actions, apiValidation, status);
    }

    void setup(AtfGherkinKeyword keyword, String title, String target,
               Collection<AtfAssertion> assertions,
               Collection<AtfUiActionRecord> actions,
               UiApiValidationEvidence apiValidation,
               ReportScreenshot screenshot,
               AtfReportStatus status) {
        setup(keyword.text(), title, target, assertions, actions, apiValidation, screenshot, status);
    }

    void networkInspector(String title,
                          Collection<ReportExecutionEvidence> executionEvidence) {
        networkInspector(title, executionEvidence, List.of());
    }

    void networkInspector(String title,
                          Collection<ReportExecutionEvidence> executionEvidence,
                          Collection<ReportNetworkInspector.Step> steps) {
        networkInspector(title, executionEvidence, steps, null);
    }

    void networkInspector(String title,
                          Collection<ReportExecutionEvidence> executionEvidence,
                          Collection<ReportNetworkInspector.Step> steps,
                          ReportNetworkCaptureStatus capture) {
        ensureOpen();
        if (networkSummary != null) {
            throw new IllegalStateException(
                    "Only one journey-level network inspector summary can be registered per UI phase");
        }
        networkSummary = new PendingNetworkSummary(
                title == null || title.isBlank() ? "API network inspector" : title.trim(),
                immutableCopy(executionEvidence), immutableCopy(steps), capture);
    }

    private void publishNetworkSummary() {
        if (networkSummary == null) return;
        PendingNetworkSummary summary = networkSummary;
        var request = delegate.startRequest(
                summary.title(), "TRACE", "network");
        try {
            request.detail("request.action", "API network inspector");
            request.detail("request.target", "ordered exchanges from the same UI browser session");
            request.detail("evidence.role", "journey summary; not an additional business step");
            request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE,
                    AtfReportEvidenceKeys.API_TRACE);
            ReportNetworkInspector.fields(summary.evidence(), summary.steps(), summary.capture()).stream()
                    .filter(field -> !field.name().startsWith("network.aggregate."))
                    .forEach(field -> request.detail(field.name(), field.value()));
            request.networkAggregate(ReportNetworkInspector.aggregate(
                    summary.evidence(), summary.steps(), summary.capture()));
            // This node is a passive journey summary, not an API assertion.
            // Failed exchanges retain their evidence here; checkpoint/API validation
            // nodes own the verdict. No independent browser runtime signals are accepted.
            request.complete(AtfReportStatus.PASS);
        } finally {
            request.close();
        }
    }

    void outcome(String keyword, String title, String target, AtfReportStatus status) {
        ensureOpen();
        publish(() -> evidencePublisher.publishOutcome(keyword, title, target, status));
    }

    void outcome(AtfGherkinKeyword keyword, String title, String target, AtfReportStatus status) {
        outcome(keyword.text(), title, target, status);
    }

    void apiTrace(AtfGherkinKeyword keyword, String title, String target,
                  UiApiValidationEvidence apiValidation, AtfReportStatus status) {
        ensureOpen();
        publish(() -> evidencePublisher.publishApiTrace(keyword.text(), title, target, apiValidation, status));
    }

    void uiValidation(String title, String target, String base64Png,
                          Collection<AtfAssertion> assertions, AtfReportStatus status) {
        action(AtfGherkinKeyword.THEN.text(), title, target, base64Png, assertions, status,
                UiValidationKind.VISUAL, List.of(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT);
    }

    void uiValidation(String title, String target, String base64Png,
                          Collection<AtfAssertion> assertions,
                          Collection<AtfUiActionRecord> actions,
                          AtfReportStatus status) {
        action(AtfGherkinKeyword.THEN.text(), title, target, base64Png, assertions, status,
                UiValidationKind.VISUAL, actions, ReportScreenshotPolicy.CapturePoint.CHECKPOINT);
    }

    void uiValidation(String title, String target, String base64Png,
                          Collection<AtfAssertion> assertions,
                          Collection<AtfUiActionRecord> actions,
                          ReportScreenshotPolicy.CapturePoint capturePoint,
                          AtfReportStatus status) {
        action(AtfGherkinKeyword.THEN.text(), title, target, base64Png, assertions, status,
                UiValidationKind.VISUAL, actions, capturePoint);
    }

    void uiValidation(String title, String target, String base64Png,
                          Collection<AtfAssertion> assertions, Collection<AtfUiActionRecord> actions,
                          UiApiValidationEvidence apiValidation,
                          ReportScreenshotPolicy.CapturePoint capturePoint, AtfReportStatus status) {
        action(AtfGherkinKeyword.THEN.text(), title, target, base64Png, assertions, status,
                UiValidationKind.VISUAL, actions, apiValidation, capturePoint);
    }

    void uiValidationAsMedia(String title, String target, String base64Png,
                                 Collection<AtfAssertion> assertions,
                                 Collection<AtfUiActionRecord> actions,
                                 AtfReportStatus status) {
        ensureOpen();
        if (flowMode == ReportEvidenceFlowMode.UNIFIED_FLOW) {
            unifiedCheckpoints.add(new UiUnifiedFlowCheckpoint(
                    AtfGherkinKeyword.THEN.text(), title, target,
                    checkpointScreenshot(base64Png,
                            ReportScreenshotPolicy.CapturePoint.CHECKPOINT),
                    assertions == null ? List.of() : List.copyOf(assertions), status,
                    UiValidationKind.VISUAL, immutableCopy(actions), UiApiValidationEvidence.empty(),
                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT, true));
            scenario.retainUiFailures(assertions);
            return;
        }
        publish(() -> evidencePublisher.publishUiValidationAsMedia(title, target, base64Png,
                assertions, actions, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, status));
        scenario.retainUiFailures(assertions);
    }

    void failureEvidence(String title, String target,
                         Collection<AtfUiActionRecord> actions,
                         Throwable failure, String base64Png) {
        ensureOpen();
        boolean noIndependentAction = actions == null || actions.stream()
                .noneMatch(action -> action.status() == AtfReportStatus.FAIL);
        publish(() -> evidencePublisher.publishFailure(title, target, actions, failure, base64Png,
                noIndependentAction && scenario.reports(failure)));
    }

    void complete(AtfReportStatus status) {
        complete(status, false);
    }

    void completeFinal(AtfReportStatus status) {
        complete(status, true);
    }

    private void complete(AtfReportStatus status, boolean finalPhase) {
        ensureOpen();
        if (execution != null) execution.finishExecution();
        var checkpoints = new ReportStatusAggregator();
        unifiedCheckpoints.forEach(checkpoint -> checkpoints.record(checkpoint.status()));
        var effective = checkpoints.resolve(status);
        if (scenario.crossPhaseUnifiedFlow()) {
            scenario.retain(phaseName, effective, List.copyOf(unifiedCheckpoints));
            unifiedCheckpoints.clear();
            if (finalPhase) {
                publish(() -> evidencePublisher.publishCrossPhaseUnifiedFlow(scenarioTitle,
                        scenario.publishableCheckpoints(), effective));
            }
        } else {
            publishUnifiedFlow(effective);
        }
        publish(this::publishNetworkSummary);
        var timing = execution == null ? suppliedTiming : execution.complete();
        if (timing != null) delegate.executionTiming(timing);
        delegate.complete(effective);
        completed = true;
    }

    @Override
    public void close() {
        if (!completed) complete(AtfReportStatus.FAIL);
    }

    private void action(String keyword, String title, String target,
                        String base64Png, Collection<AtfAssertion> assertions,
                        AtfReportStatus status, UiValidationKind validationKind,
                        Collection<AtfUiActionRecord> actions) {
        action(keyword, title, target, base64Png, assertions, status,
                validationKind, actions, UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.CHECKPOINT);
    }

    private void action(String keyword, String title, String target,
                        String base64Png, Collection<AtfAssertion> assertions,
                        AtfReportStatus status, UiValidationKind validationKind,
                        Collection<AtfUiActionRecord> actions,
                        UiApiValidationEvidence apiValidation,
                        ReportScreenshotPolicy.CapturePoint capturePoint) {
        ensureOpen();
        java.util.Objects.requireNonNull(apiValidation, "apiValidation");
        if (flowMode == ReportEvidenceFlowMode.UNIFIED_FLOW) {
            String eligibleScreenshot = checkpointScreenshot(base64Png, capturePoint);
            unifiedCheckpoints.add(new UiUnifiedFlowCheckpoint(keyword, title, target,
                    eligibleScreenshot, immutableCopy(assertions), status, validationKind,
                    immutableCopy(actions), apiValidation, capturePoint, false));
            scenario.retainUiFailures(assertions);
            return;
        }
        publish(() -> evidencePublisher.publishAction(keyword, title, target, base64Png, assertions,
                status, validationKind, actions, apiValidation, capturePoint));
        scenario.retainUiFailures(assertions);
    }

    private void action(String keyword, String title, String target,
                        String base64Png, Collection<AtfAssertion> assertions,
                        AtfReportStatus status, UiValidationKind validationKind,
                        Collection<AtfUiActionRecord> actions,
                        ReportScreenshotPolicy.CapturePoint capturePoint) {
        action(keyword, title, target, base64Png, assertions, status,
                validationKind, actions, UiApiValidationEvidence.empty(), capturePoint);
    }

    private void ensureOpen() {
        if (completed) throw new IllegalStateException("UI phase is already complete");
    }

    private void publishUnifiedFlow(AtfReportStatus status) {
        if (flowMode != ReportEvidenceFlowMode.UNIFIED_FLOW
                || unifiedCheckpoints.isEmpty() || unifiedFlowPublished) return;
        unifiedFlowPublished = true;
        publish(() -> evidencePublisher.publishUnifiedFlow(scenarioTitle, phaseName,
                List.copyOf(unifiedCheckpoints), status));
        unifiedCheckpoints.clear();
    }

    private static <T> List<T> immutableCopy(Collection<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String checkpointScreenshot(String base64Png,
                                        ReportScreenshotPolicy.CapturePoint capturePoint) {
        return evidencePublisher.acceptsScreenshot(base64Png, capturePoint) ? base64Png : "";
    }

    private record PendingNetworkSummary(String title,
                                        List<ReportExecutionEvidence> evidence,
                                        List<ReportNetworkInspector.Step> steps,
                                        ReportNetworkCaptureStatus capture) { }
}
