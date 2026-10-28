package atf.reporting.html.scope;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.semantic.AtfReportSemanticRecorder;

import java.util.function.Consumer;

/** Coordinates one scenario phase and exposes request-level reporting ports. */
public final class HtmlReportPhaseScope implements AtfReportPorts.Phase {
    private final HtmlReportSemanticDocument.PhaseModel model;
    private final String consoleName;
    private final AtfReportSemanticRecorder.PhaseHandle semantic;
    private final AtfConsoleSink console;
    private final Consumer<AtfReportStatus> onComplete;
    private final long startedAt = System.nanoTime();
    private final ReportStatusAggregator childStatuses = new ReportStatusAggregator();
    private boolean closed;
    private atf.reporting.model.timing.ReportPhaseTiming timing;

    public HtmlReportPhaseScope(HtmlReportSemanticDocument.PhaseModel model,
                                String consoleName,
                                AtfReportSemanticRecorder.PhaseHandle semantic,
                                AtfConsoleSink console,
                                Consumer<AtfReportStatus> onComplete) {
        this.model = model;
        this.consoleName = consoleName;
        this.semantic = semantic;
        this.console = console;
        this.onComplete = onComplete;
    }

    @Override
    public AtfReportPorts.Request startRequest(String title, String method, String path) {
        ensureOpen();
        var request = model.addRequest(title, method, path);
        String requestName = ExtentReportNodeSupport.renderRequestTitle(title, method, path);
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST, requestName));
        return new HtmlReportRequestScope(request, requestName,
                semantic.startRequest(title, method, path), console);
    }

    @Override public AtfReportPorts.AssertionStep startAssertionStep(String title) {
        ensureOpen();
        String stepTitle = title == null || title.isBlank() ? "Assertions" : title;
        return new HtmlReportAssertionStepScope(model.addAssertionStep(stepTitle), stepTitle,
                semantic.startAssertionStep(stepTitle), console, childStatuses::record);
    }

    @Override
    public void detail(String name, String value) {
        ensureOpen();
        model.addDetail(name, value);
        semantic.detail(name);
    }

    @Override public void executionTiming(atf.reporting.model.timing.ReportPhaseTiming value) {
        ensureOpen();
        if (timing != null) throw new IllegalStateException("Phase timing is already supplied");
        timing = java.util.Objects.requireNonNull(value, "timing");
        model.executionTiming(value);
        semantic.executionTiming(value);
    }

    @Override
    public void complete(AtfReportStatus status) {
        ensureOpen();
        if (timing != null) model.publicationMillis(HtmlReportScopeTiming.elapsedMillis(startedAt));
        AtfReportStatus effective = AtfReportStatus.worst(childStatuses.aggregate(), status);
        model.complete(effective);
        semantic.complete(effective);
        onComplete.accept(effective);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.PHASE,
                consoleName, effective, HtmlReportScopeTiming.elapsedMillis(startedAt)));
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Phase is closed");
        }
    }
}
