package atf.reporting.extent;
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
    private boolean closed;

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

    @Override public void detail(String name, String value) {
        ensurePhaseOpen();
        String normalizedName = name == null || name.isBlank() ? "DETAIL" : name;
        details.add(new ReportField(normalizedName, tree.redactor().redactField(name, value)));
        semantic.detail(name);
        tree.consoleDetail(AtfConsoleEvent.Scope.PHASE,
                name, tree.redactor().redactField(name, value));
    }

    @Override public void complete(AtfReportStatus status) {
        ensurePhaseOpen();
        var effective = AtfReportStatus.worst(statuses.aggregate(), status);
        scenario.renderDiagnostics(ReportPhaseDiagnostic.from(phaseName, details, effective));
        tree.complete(phase, effective);
        tree.markEnded(phase);
        scenario.record(effective);
        semantic.complete(effective);
        tree.consoleCompleted(AtfConsoleEvent.Scope.PHASE,
                phaseName, effective, phase);
        closed = true;
    }

    void record(AtfReportStatus status) { statuses.record(status); }
    ExtentTest node() { return phase; }
    @Override public void close() { closed = true; }

    private void ensurePhaseOpen() {
        tree.ensureOpen();
        if (closed) throw new IllegalStateException("Phase is closed");
    }
}
