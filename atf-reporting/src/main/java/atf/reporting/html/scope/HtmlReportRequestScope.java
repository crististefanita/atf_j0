package atf.reporting.html.scope;

import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.reporting.semantic.AtfReportSemanticRecorder;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/** Coordinates request evidence and its nested validation port. */
public final class HtmlReportRequestScope implements AtfReportPorts.Request {
    private final HtmlReportSemanticDocument.RequestModel model;
    private final String consoleName;
    private final AtfReportSemanticRecorder.RequestHandle semantic;
    private final AtfConsoleSink console;
    private final Consumer<AtfReportStatus> onComplete;
    private ReportNetworkInspector.JourneyAggregate networkAggregate;
    private final long startedAt = System.nanoTime();
    private boolean closed;

    public HtmlReportRequestScope(HtmlReportSemanticDocument.RequestModel model,
                                  String consoleName,
                                  AtfReportSemanticRecorder.RequestHandle semantic,
                                  AtfConsoleSink console) {
        this(model, consoleName, semantic, console, status -> { });
    }

    HtmlReportRequestScope(HtmlReportSemanticDocument.RequestModel model,
                           String consoleName,
                           AtfReportSemanticRecorder.RequestHandle semantic,
                           AtfConsoleSink console,
                           Consumer<AtfReportStatus> onComplete) {
        this.model = model;
        this.consoleName = consoleName;
        this.semantic = Objects.requireNonNull(semantic, "semantic");
        this.console = Objects.requireNonNull(console, "console");
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete");
    }

    @Override
    public void detail(String name, String value) {
        model.addDetail(name, value);
        semantic.detail(name);
    }

    @Override
    public void json(String label, String json) {
        model.addJson(label, json);
        semantic.json(label);
    }

    @Override
    public void body(ReportJsonBody body) {
        model.addBody(body);
        semantic.json(body.label());
    }

    @Override
    public void execution(ReportExecutionEvidence result) {
        model.execution(result);
        semantic.execution();
    }

    @Override
    public void networkAggregate(ReportNetworkInspector.JourneyAggregate aggregate) {
        networkAggregate = aggregate;
        if (aggregate != null) semantic.detail("network.aggregate");
        model.networkAggregate(aggregate);
    }

    @Override
    public void screenshot(String title, String base64Png) {
        model.addScreenshot();
        semantic.screenshot();
    }

    @Override
    public void uiActions(Collection<AtfUiActionRecord> actions) {
        model.addUiActions(actions);
        semantic.uiActions(actions);
    }

    @Override
    public AtfReportPorts.Validation startValidation(String title) {
        ensureOpen();
        var validation = model.addValidation(title);
        String validationName = title == null ? "Validation" : title;
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.VALIDATION, validationName));
        return new HtmlReportValidationScope(validation, validationName,
                semantic.startValidation(validationName), console);
    }

    @Override
    public void complete(AtfReportStatus status) {
        model.complete(status);
        AtfReportStatus effective = model.status();
        semantic.complete(effective);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                consoleName, effective, HtmlReportScopeTiming.elapsedMillis(startedAt)));
        onComplete.accept(effective);
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Request is closed");
        }
    }
}
