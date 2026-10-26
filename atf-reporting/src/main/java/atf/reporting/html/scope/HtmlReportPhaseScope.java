package atf.reporting.html.scope;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.semantic.AtfReportSemanticRecorder;

/** Coordinates one scenario phase and exposes request-level reporting ports. */
public final class HtmlReportPhaseScope implements AtfReportPorts.Phase {
    private final HtmlReportSemanticDocument.PhaseModel model;
    private final String consoleName;
    private final AtfReportSemanticRecorder.PhaseHandle semantic;
    private final AtfConsoleSink console;
    private final long startedAt = System.nanoTime();
    private boolean closed;

    public HtmlReportPhaseScope(HtmlReportSemanticDocument.PhaseModel model,
                                String consoleName,
                                AtfReportSemanticRecorder.PhaseHandle semantic,
                                AtfConsoleSink console) {
        this.model = model;
        this.consoleName = consoleName;
        this.semantic = semantic;
        this.console = console;
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

    @Override
    public void detail(String name, String value) {
        ensureOpen();
        model.addDetail(name, value);
        semantic.detail(name);
    }

    @Override
    public void complete(AtfReportStatus status) {
        model.complete(status);
        semantic.complete(status);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.PHASE,
                consoleName, status, HtmlReportScopeTiming.elapsedMillis(startedAt)));
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
