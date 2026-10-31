package atf.reporting.html.scope;
import atf.reporting.duration.AtfDurationFormatter;
import atf.reporting.model.timing.ReportPhaseTiming;

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
    private ReportPhaseTiming timing;

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
        var handle = semantic.startRequest(title, method, path);
        var request = model.addRequest(title, method, path);
        String requestName = ExtentReportNodeSupport.renderRequestTitle(title, method, path);
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST, requestName));
        return new HtmlReportRequestScope(request, requestName,
                handle, console, childStatuses::record);
    }

    @Override public AtfReportPorts.AssertionStep startAssertionStep(String title) {
        ensureOpen();
        String stepTitle = title == null || title.isBlank() ? "Assertions" : title;
        var handle = semantic.startAssertionStep(stepTitle);
        return new HtmlReportAssertionStepScope(model.addAssertionStep(stepTitle), stepTitle,
                handle, console, childStatuses::record);
    }

    @Override
    public void detail(String name, String value) {
        ensureOpen();
        semantic.detail(name);
        model.addDetail(name, value);
    }

    @Override public boolean supportsObservations() { return true; }

    @Override public void observation(atf.reporting.model.ReportObservation value) {
        ensureOpen();
        java.util.Objects.requireNonNull(value, "observation");
        semantic.observation(value);
        model.addObservation(value);
    }

    @Override public void executionTiming(ReportPhaseTiming value) {
        ensureOpen();
        if (timing != null) throw new IllegalStateException("Phase timing is already supplied");
        semantic.executionTiming(value);
        model.executionTiming(value);
        timing = value;
    }

    @Override
    public void complete(AtfReportStatus status) {
        long completionStarted = System.nanoTime();
        ensureOpen();
        AtfReportStatus effective = childStatuses.resolve(status);
        semantic.complete(effective);
        model.complete(effective);
        if (timing != null) model.publicationMillis(timing.evidence().map(value -> AtfDurationFormatter.elapsedMillis(
                value.publication().plusNanos(System.nanoTime() - completionStarted)))
                .orElseGet(() -> HtmlReportScopeTiming.elapsedMillis(startedAt)));
        onComplete.accept(effective);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.PHASE,
                consoleName, effective, HtmlReportScopeTiming.elapsedMillis(startedAt)));
    }

    @Override public void close() { if (!semantic.isComplete()) complete(AtfReportStatus.FAIL); }

    private void ensureOpen() { semantic.ensureOpen(); }
}
