package atf.reporting.html.scope;
import atf.reporting.contract.evidence.ReportEvidenceOwner;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.reporting.semantic.AtfReportSemanticRecorder;

import java.util.Collection;

/** Coordinates assertions and UI evidence for one validation leaf. */
public final class HtmlReportValidationScope implements AtfReportPorts.Validation {
    private final HtmlReportSemanticDocument.ValidationModel model;
    private final String consoleName;
    private final AtfReportSemanticRecorder.ValidationHandle semantic;
    private final AtfConsoleSink console;
    private final ReportStatusAggregator childStatuses = new ReportStatusAggregator();
    private final long startedAt = System.nanoTime();
    private int openRequests;
    private boolean completed;

    public HtmlReportValidationScope(HtmlReportSemanticDocument.ValidationModel model,
                                     String consoleName,
                                     AtfReportSemanticRecorder.ValidationHandle semantic,
                                     AtfConsoleSink console) {
        this.model = model;
        this.consoleName = consoleName;
        this.semantic = semantic;
        this.console = console;
    }

    @Override
    public void evidenceOwner(ReportEvidenceOwner owner) {
        ensureOpen();
        model.evidenceOwner(owner);
        semantic.evidenceOwner(owner);
    }

    @Override
    public void assertions(Collection<AtfAssertion> assertions) {
        ensureOpen();
        model.assertions(assertions);
        semantic.assertions(assertions);
    }

    @Override
    public void uiActions(Collection<AtfUiActionRecord> actions) {
        ensureOpen();
        model.addUiActions(actions);
        semantic.uiActions(actions);
    }

    @Override
    public void screenshot(String title, String base64Png) {
        ensureOpen();
        model.addScreenshot(title, base64Png);
        semantic.screenshot();
    }

    @Override
    public void mediaScreenshot(String title, String base64Png) {
        ensureOpen();
        model.addScreenshot(title, base64Png);
        semantic.screenshot();
    }

    @Override
    public void complete(AtfReportStatus status) {
        ensureOpen();
        if (openRequests > 0) {
            throw new IllegalStateException("Validation cannot complete while a nested request is open");
        }
        AtfReportStatus effective = AtfReportStatus.worst(childStatuses.aggregate(), status);
        model.complete(effective);
        semantic.complete(effective);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.VALIDATION,
                consoleName, effective, HtmlReportScopeTiming.elapsedMillis(startedAt)));
        completed = true;
    }

    @Override
    public AtfReportPorts.Request startRequest(String title, String method, String path) {
        ensureOpen();
        openRequests++;
        var request = model.addRequest(title, method, path);
        String requestName = ExtentReportNodeSupport
                .renderRequestTitle(title, method, path);
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST, requestName));
        return new HtmlReportRequestScope(request, requestName,
                semantic.startRequest(title, method, path), console,
                this::recordChildStatus);
    }

    private void recordChildStatus(AtfReportStatus status) {
        childStatuses.record(status);
        openRequests--;
    }

    @Override
    public void close() {
    }

    private void ensureOpen() {
        if (completed) {
            throw new IllegalStateException("Validation is already complete");
        }
    }
}
