package atf.reporting.extent;
import atf.reporting.duration.AtfDurationFormatter;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.extent.session.ExtentReportTree;
import atf.reporting.model.ReportField;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.semantic.AtfReportSemanticRecorder;

import com.aventstack.extentreports.ExtentTest;

import java.util.ArrayList;
import java.util.List;

final class ExtentPhaseScope implements AtfReportPorts.Phase {
    private final ExtentReportTree tree;
    private final ExtentScenarioScope scenario;
    private final ExtentTest phase;
    private final String phaseName;
    private final ReportStatusAggregator statuses = new ReportStatusAggregator();
    private final List<ReportField> details = new ArrayList<>();
    private final AtfReportSemanticRecorder.PhaseHandle semantic;
    private ReportPhaseTiming timing;
    private final long publicationStarted = System.nanoTime();

    ExtentPhaseScope(ExtentReportTree tree, ExtentScenarioScope scenario, ExtentTest phase,
                     String phaseName, AtfReportSemanticRecorder.PhaseHandle semantic) {
        this.tree = tree;
        this.scenario = scenario;
        this.phase = phase;
        this.phaseName = phaseName;
        this.semantic = semantic;
        tree.markStarted(phase);
    }

    @Override public AtfReportPorts.Request startRequest(String title, String method, String path) {
        ensurePhaseOpen();
        String requestTitle = tree.renderRequestTitle(title, method, path);
        tree.consoleStarted(AtfConsoleEvent.Scope.REQUEST, requestTitle);
        return new ExtentRequestScope(tree, this, phase.createNode(requestTitle), requestTitle,
                semantic.startRequest(title, method, path));
    }

    @Override public AtfReportPorts.AssertionStep startAssertionStep(String title) {
        ensurePhaseOpen();
        String stepTitle = title == null || title.isBlank() ? "Assertions" : title;
        return new ExtentAssertionStepScope(tree, this, phase.createNode(stepTitle),
                stepTitle, semantic.startAssertionStep(stepTitle));
    }

    @Override public boolean supportsObservations() { return true; }

    @Override public void observation(atf.reporting.model.ReportObservation value) {
        ensurePhaseOpen();
        java.util.Objects.requireNonNull(value, "observation");
        String markup = atf.reporting.evidence.markup.ReportObservationMarkup.render(value, tree.settings(), tree.redactor());
        String title = atf.reporting.markup.ExtentReportNodeSupport.display(tree.redactor().redactText(value.title()), tree.settings());
        var node = phase.createNode(title);
        tree.markStarted(node);
        semantic.observation(value);
        node.info(markup);
        tree.complete(node, AtfReportStatus.INFO);
        tree.markEnded(node);
    }

    @Override public void executionTiming(ReportPhaseTiming value) {
        ensurePhaseOpen();
        if (timing != null) throw new IllegalStateException("Phase timing is already supplied");
        timing = java.util.Objects.requireNonNull(value, "timing");
        semantic.executionTiming(value);
    }

    @Override public void detail(String name, String value) {
        ensurePhaseOpen();
        String normalizedName = name == null || name.isBlank() ? "DETAIL" : name;
        details.add(new ReportField(normalizedName, tree.redactor().redactField(name, value)));
        semantic.detail(name);
        tree.consoleDetail(AtfConsoleEvent.Scope.PHASE,
                name, tree.redactor().redactField(name, value));
    }

    @Override public void complete(AtfReportStatus status) {
        long completionStarted = System.nanoTime();
        ensurePhaseOpen();
        var effective = statuses.resolve(status);
        semantic.complete(effective);
        scenario.renderDiagnostics(ReportPhaseDiagnostic.from(phaseName, details, effective));
        if (timing != null) {
            long publicationMillis = timing.evidence().map(value -> AtfDurationFormatter.elapsedMillis(
                    value.publication().plusNanos(System.nanoTime() - completionStarted)))
                    .orElseGet(() -> Math.max(0L, (System.nanoTime() - publicationStarted) / 1_000_000L));
            phase.info(atf.reporting.evidence.timing.ReportPhaseTimingMarkup.metadata(timing, publicationMillis));
        }
        tree.complete(phase, effective);
        tree.markEnded(phase);
        scenario.record(effective);
        tree.consoleCompleted(AtfConsoleEvent.Scope.PHASE,
                phaseName, effective, phase);
    }

    void record(AtfReportStatus status) { statuses.record(status); }
    ExtentTest node() { return phase; }
    @Override public void close() { if (!semantic.isComplete()) complete(AtfReportStatus.FAIL); }

    private void ensurePhaseOpen() {
        tree.ensureOpen();
        semantic.ensureOpen();
    }
}
