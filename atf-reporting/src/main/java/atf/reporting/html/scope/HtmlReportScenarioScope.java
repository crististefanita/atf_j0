package atf.reporting.html.scope;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.reporting.semantic.AtfReportSemanticRecorder;

/** Coordinates one scenario without coupling the lifecycle to HTML rendering. */
public final class HtmlReportScenarioScope implements AtfReportPorts.Scenario {
    private final HtmlReportSemanticDocument.ScenarioModel model;
    private final String consoleName;
    private final AtfReportSemanticRecorder.ScenarioHandle semantic;
    private final AtfConsoleSink console;
    private final long startedAt = System.nanoTime();
    private boolean closed;

    public HtmlReportScenarioScope(HtmlReportSemanticDocument.ScenarioModel model,
                                   String consoleName,
                                   AtfReportSemanticRecorder.ScenarioHandle semantic,
                                   AtfConsoleSink console) {
        this.model = model;
        this.consoleName = consoleName;
        this.semantic = semantic;
        this.console = console;
    }

    @Override
    public String id() {
        return model.id();
    }

    @Override
    public AtfReportPorts.Phase startPhase(String name) {
        ensureOpen();
        var phase = model.addPhase(name);
        String phaseName = name == null ? "" : name;
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.PHASE, phaseName));
        return new HtmlReportPhaseScope(phase, phaseName,
                semantic.startPhase(phaseName), console);
    }

    @Override
    public void complete(AtfReportStatus status) {
        model.complete(status);
        semantic.complete(status);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.SCENARIO,
                consoleName, status, HtmlReportScopeTiming.elapsedMillis(startedAt)));
    }

    @Override
    public boolean hasWarnings() {
        return model.hasWarnings();
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scenario is closed");
        }
    }
}
