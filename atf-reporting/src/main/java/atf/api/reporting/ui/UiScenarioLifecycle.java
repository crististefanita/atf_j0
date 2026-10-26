package atf.api.reporting.ui;
import atf.api.reporting.contract.AtfReportPorts;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.ui.failure.UiCheckpointFailure;
import atf.api.reporting.contract.AtfReportStatus;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.api.reporting.evidence.profile.ReportEvidenceProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;


/** Owns scenario completion state while the public UI facade stays thin. */
public final class UiScenarioLifecycle implements AutoCloseable {
    private final AtfReportPorts.Scenario delegate;
    private final ReportScreenshotPolicy screenshotPolicy;
    private final ReportEvidenceFlowMode flowMode;
    private final String scenarioTitle;
    private final List<PhaseCheckpoint> phaseCheckpoints = new ArrayList<>();
    private boolean crossPhaseUnifiedFlow;
    private boolean phaseStarted;
    private boolean unifiedFlowPublished;
    private boolean completed;
    private final Set<AtfAssertion> failedUiAssertions = Collections.newSetFromMap(new IdentityHashMap<>());

    void retainUiFailures(Collection<AtfAssertion> assertions) {
        if (assertions != null) assertions.stream()
                .filter(assertion -> assertion.resultStatus() == AtfReportStatus.FAIL)
                .forEach(failedUiAssertions::add);
    }

    boolean reports(Throwable failure) {
        return UiCheckpointFailure.isReported(failure, failedUiAssertions);
    }

    UiScenarioLifecycle(AtfReportPorts.Scenario delegate, ReportEvidenceProfile evidenceProfile,
                        String scenarioTitle) {
        this.delegate = delegate;
        this.screenshotPolicy = evidenceProfile.screenshotPolicy();
        this.flowMode = evidenceProfile.flowMode();
        this.scenarioTitle = scenarioTitle == null ? "" : scenarioTitle.trim();
    }

    UiPhaseLifecycle startPhase(String name) {
        ensureOpen();
        phaseStarted = true;
        return new UiPhaseLifecycle(delegate.startPhase(name), screenshotPolicy,
                flowMode, scenarioTitle, name, this);
    }

    void unifyAcrossPhases() {
        ensureOpen();
        if (phaseStarted) throw new IllegalStateException("Select cross-phase flow before starting SETUP");
        if (flowMode != ReportEvidenceFlowMode.UNIFIED_FLOW) {
            throw new IllegalStateException("Cross-phase flow requires UNIFIED_FLOW evidence profile");
        }
        crossPhaseUnifiedFlow = true;
    }

    boolean crossPhaseUnifiedFlow() { return crossPhaseUnifiedFlow; }

    void retain(String phase, AtfReportStatus status, List<UiUnifiedFlowCheckpoint> checkpoints) {
        if (unifiedFlowPublished) throw new IllegalStateException("Unified flow was already published");
        for (var checkpoint : checkpoints) phaseCheckpoints.add(new PhaseCheckpoint(phase, status, checkpoint));
    }

    List<PhaseCheckpoint> publishableCheckpoints() {
        if (unifiedFlowPublished) throw new IllegalStateException("Unified flow was already published");
        unifiedFlowPublished = true;
        return List.copyOf(phaseCheckpoints);
    }

    record PhaseCheckpoint(String phase, AtfReportStatus phaseStatus,
                           UiUnifiedFlowCheckpoint checkpoint) { }

    void complete(AtfReportStatus status) {
        ensureOpen();
        if (crossPhaseUnifiedFlow && !unifiedFlowPublished && !phaseCheckpoints.isEmpty()) {
            throw new IllegalStateException("Complete the terminal phase before completing a cross-phase flow");
        }
        delegate.complete(status == null ? AtfReportStatus.INFO : status);
        completed = true;
    }

    @Override
    public void close() {
        if (!completed) complete(AtfReportStatus.FAIL);
    }

    private void ensureOpen() {
        if (completed) throw new IllegalStateException("UI scenario is already complete");
    }
}
