package atf.reporting.html.scope;
import atf.reporting.model.ReportScreenshot;
import java.util.ArrayList;
import atf.reporting.evidence.policy.UiActionScreenshotPolicy;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;

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
import java.util.List;
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
        ensureOpen();
        semantic.detail(name);
        model.addDetail(name, value);
    }

    @Override
    public void json(String label, String json) {
        ensureOpen();
        var body = new ReportJsonBody(label, json);
        semantic.json(body.label());
        model.addBody(body);
    }

    @Override
    public void body(ReportJsonBody body) {
        ensureOpen();
        Objects.requireNonNull(body, "body");
        semantic.json(body.label());
        model.addBody(body);
    }

    @Override
    public void execution(ReportExecutionEvidence result) {
        ensureOpen();
        if (result != null) { semantic.execution(); model.execution(result); }
    }

    @Override
    public void networkAggregate(ReportNetworkInspector.JourneyAggregate aggregate) {
        ensureOpen();
        networkAggregate = aggregate;
        if (aggregate != null) semantic.detail("network.aggregate");
        model.networkAggregate(aggregate);
    }

    @Override
    public void screenshot(String title, String base64Png) {
        ensureOpen();
        if (base64Png == null || base64Png.isBlank()) throw new IllegalArgumentException("base64Png must not be blank");
        var current = model.uiEvidence();
        var candidate = new ArrayList<>(current.screenshots()); candidate.add(new ReportScreenshot(title, base64Png));
        UiActionScreenshotPolicy.validate(current.actions(), candidate);
        semantic.screenshot();
        model.addScreenshot(title, base64Png);
    }

    @Override
    public void uiActions(Collection<AtfUiActionRecord> actions) {
        ensureOpen();
        var stable = actions == null ? List.<AtfUiActionRecord>of() : List.copyOf(actions);
        var current = model.uiEvidence(); var candidate = new ArrayList<>(current.actions()); candidate.addAll(stable);
        UiActionScreenshotPolicy.validate(candidate, current.screenshots());
        semantic.uiActions(stable);
        model.addUiActions(stable);
    }

    @Override public void uiActionDetailsMode(ReportUiActionDetailsMode mode) {
        ensureOpen(); model.uiActionDetailsMode(mode);
    }

    @Override
    public AtfReportPorts.Validation startValidation(String title) {
        ensureOpen();
        String validationName = title == null ? "Validation" : title;
        var handle = semantic.startValidation(validationName);
        var validation = model.addValidation(validationName);
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.VALIDATION, validationName));
        return new HtmlReportValidationScope(validation, validationName,
                handle, console);
    }

    @Override
    public void complete(AtfReportStatus status) {
        ensureOpen();
        var current = model.uiEvidence();
        UiActionScreenshotPolicy.validate(current.actions(), current.screenshots());
        semantic.complete(status);
        model.complete(status);
        AtfReportStatus effective = model.status();
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                consoleName, effective, HtmlReportScopeTiming.elapsedMillis(startedAt)));
        onComplete.accept(effective);
    }

    @Override public void close() { if (!semantic.isComplete()) complete(AtfReportStatus.FAIL); }

    private void ensureOpen() { semantic.ensureOpen(); }
}
