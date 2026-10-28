package atf.reporting.html.scope;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.reporting.semantic.AtfReportSemanticRecorder;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** Fallback lifecycle for a phase-owned assertion node. */
final class HtmlReportAssertionStepScope implements AtfReportPorts.AssertionStep {
    private final HtmlReportSemanticDocument.AssertionStepModel model;
    private final String title;
    private final AtfReportSemanticRecorder.AssertionStepHandle semantic;
    private final AtfConsoleSink console;
    private final Consumer<AtfReportStatus> onComplete;
    private final long startedAt = System.nanoTime();
    private List<AtfAssertion> rows = List.of();
    private boolean completed;

    HtmlReportAssertionStepScope(HtmlReportSemanticDocument.AssertionStepModel model, String title,
                                  AtfReportSemanticRecorder.AssertionStepHandle semantic,
                                  AtfConsoleSink console, Consumer<AtfReportStatus> onComplete) {
        this.model = model;
        this.title = title;
        this.semantic = semantic;
        this.console = console;
        this.onComplete = onComplete;
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.VALIDATION, title));
    }

    @Override public void assertions(Collection<AtfAssertion> assertions) {
        ensureOpen();
        rows = assertions == null ? List.of() : List.copyOf(assertions);
        model.assertions(rows);
        semantic.assertions(rows);
    }

    @Override public void complete(AtfReportStatus status) {
        ensureOpen();
        AtfReportStatus effective = status;
        for (AtfAssertion row : rows) effective = AtfReportStatus.worst(effective, row.resultStatus());
        model.complete(effective);
        semantic.complete(effective);
        onComplete.accept(effective);
        console.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.VALIDATION,
                title, effective, HtmlReportScopeTiming.elapsedMillis(startedAt)));
        completed = true;
    }

    @Override public void close() { completed = true; }
    private void ensureOpen() {
        if (completed) throw new IllegalStateException("Assertion step is already complete");
    }
}
