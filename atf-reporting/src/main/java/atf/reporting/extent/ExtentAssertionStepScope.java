package atf.reporting.extent;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportTree;
import atf.reporting.semantic.AtfReportSemanticRecorder;
import com.aventstack.extentreports.ExtentTest;

import java.util.Collection;
import java.util.List;

/** One phase-owned, non-transport assertion node. */
final class ExtentAssertionStepScope implements AtfReportPorts.AssertionStep {
    private final ExtentReportTree tree;
    private final ExtentPhaseScope phase;
    private final ExtentTest node;
    private final String title;
    private final AtfReportSemanticRecorder.AssertionStepHandle semantic;
    private List<AtfAssertion> rows = List.of();
    private boolean completed;

    ExtentAssertionStepScope(ExtentReportTree tree, ExtentPhaseScope phase, ExtentTest node,
                             String title, AtfReportSemanticRecorder.AssertionStepHandle semantic) {
        this.tree = tree;
        this.phase = phase;
        this.node = node;
        this.title = title;
        this.semantic = semantic;
        tree.markStarted(node);
        tree.consoleStarted(AtfConsoleEvent.Scope.VALIDATION, title);
    }

    @Override public void assertions(Collection<AtfAssertion> assertions) {
        ensureOpen();
        rows = assertions == null ? List.of() : List.copyOf(assertions);
        semantic.assertions(rows);
    }

    @Override public void complete(AtfReportStatus status) {
        ensureOpen();
        AtfReportStatus effective = status;
        for (AtfAssertion row : rows) effective = AtfReportStatus.worst(effective, row.resultStatus());
        tree.renderAssertions(node, rows);
        tree.complete(node, effective);
        tree.markEnded(node);
        phase.record(effective);
        semantic.complete(effective);
        tree.consoleCompleted(AtfConsoleEvent.Scope.VALIDATION, title, effective, node);
        completed = true;
    }

    @Override public void close() { completed = true; }

    private void ensureOpen() {
        tree.ensureOpen();
        if (completed) throw new IllegalStateException("Assertion step is already complete");
    }
}
