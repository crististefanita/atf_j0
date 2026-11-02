package atf.reporting.html;

import atf.reporting.semantic.tags.ReportTags;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.semantic.AtfReportSemanticRecorder;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.theme.AtfReportTheme;
import atf.reporting.html.script.HtmlReportNavigationScript;
import atf.reporting.html.script.HtmlReportScript;
import atf.reporting.html.scope.HtmlReportScenarioScope;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.ui.model.ReportDocumentModel;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.configuration.ReportEnvironment;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.security.SensitiveDataRedactor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/**
 * Renderer scoped to one report file. It deliberately owns no static mutable
 * state, so parallel executions cannot leak nodes or verdicts into each other.
 */
public final class HtmlReportSession implements AtfReportPorts.Session {
    public static final ReportDocumentModel DOCUMENT_MODEL = ReportDocumentModel.fallback();

    private final Path reportPath;
    private final ReportingSettings settings;
    private final HtmlReportSemanticDocument document = new HtmlReportSemanticDocument();
    private final AtfReportSemanticRecorder semantic = new AtfReportSemanticRecorder(this::ensureOpen);
    private final HtmlReportTestsView testsView;
    private final HtmlReportCategoryView categoryView = new HtmlReportCategoryView();
    private final HtmlReportFailsView failsView;
    private final HtmlReportOverviewView overviewView = new HtmlReportOverviewView();
    private final AtfConsoleSink console;
    private final HtmlReportScript navigationScript;
    private final SensitiveDataRedactor environmentRedactor = new SensitiveDataRedactor();
    private final Map<String, String> systemInfo = new LinkedHashMap<>(ReportEnvironment.defaults());
    private boolean closed;

    public HtmlReportSession(Path reportPath) {
        this(reportPath, ReportingSettings.defaults());
    }

    public HtmlReportSession(Path reportPath, ReportingSettings settings) {
        this(reportPath, settings, AtfConsoleSink.noOp());
    }

    public HtmlReportSession(Path reportPath, ReportingSettings settings, AtfConsoleSink console) {
        this(reportPath, settings, console, HtmlReportNavigationScript.standard());
    }

    /**
     * Creates a session with an explicit browser asset port.
     *
     * <p>The overload is public so an alternate report host can replace
     * navigation without subclassing or copying the lifecycle renderer.</p>
     */
    public HtmlReportSession(Path reportPath, ReportingSettings settings, AtfConsoleSink console,
                             HtmlReportScript navigationScript) {
        this.reportPath = Objects.requireNonNull(reportPath, "reportPath");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.testsView = new HtmlReportTestsView(settings, new SensitiveDataRedactor());
        this.failsView = new HtmlReportFailsView(settings);
        this.console = AtfConsoleSink.bestEffort(console);
        this.navigationScript = Objects.requireNonNull(navigationScript, "navigationScript");
    }

    @Override
    public Path reportPath() {
        return reportPath;
    }

    /** Merges allowlisted operational facts; renderer identity and the effective preview remain publisher-owned. */
    public void updateEnvironment(ReportEnvironmentSnapshot snapshot) {
        ensureOpen();
        var safe = new LinkedHashMap<String, String>();
        Objects.requireNonNull(snapshot, "snapshot").systemDisplayValues().forEach((name, value) ->
                safe.put(name, environmentRedactor.redactField(name, value)));
        systemInfo.putAll(safe);
    }

    /** Returns the same renderer-neutral semantic population used by Extent. */
    public AtfReportSemanticSnapshot semanticSnapshot() {
        return AtfReportSemanticSnapshot.from(document.snapshot());
    }

    /** Exposes the fallback execution tree for component-level parity checks. */
    public ReportExecutionModel executionModel() {
        return document.snapshot();
    }

    @Override
    public AtfReportPorts.Scenario startScenario(String testClass, String scenario, Collection<String> tags) {
        return startScenario(testClass, null, scenario, tags);
    }

    /** Package-aware overload kept in parity with the Extent projection. */
    @Override
    public AtfReportPorts.Scenario startScenario(String testClass, String testPackage,
                                                 String scenario, Collection<String> tags) {
        ensureOpen();
        var stableTags = ReportTags.normalize(tags);
        var handle = semantic.startScenario(testClass, testPackage, scenario, stableTags);
        var model = document.startScenario(testClass, testPackage, scenario, stableTags);
        String name = (testClass == null ? "API tests" : testClass) + " :: "
                + (scenario == null ? "Scenario" : scenario);
        console.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.SCENARIO, name));
        return new HtmlReportScenarioScope(model, name,
                handle, console);
    }

    @Override
    public void flush() {
        ensureOpen();
        try {
            if (reportPath.getParent() != null) Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, render(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot write report " + reportPath, error);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            try {
                flush();
            } finally {
                closed = true;
            }
        }
    }

    public String render() {
        assertSemanticPopulationAligned();
        var html = new StringBuilder(32_000);
        html.append(HtmlReportShell.open(AtfReportTheme.fallbackCss()));
        html.append("<nav ").append(AtfReportHtmlVocabulary.classAttribute(
                AtfReportHtmlVocabulary.CssClass.TABS)).append(">");
        for (var tabModel : DOCUMENT_MODEL.tabs()) {
            var tab = tabModel.tab();
            html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.BUTTON,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.TAB),
                            AtfReportHtmlVocabulary.attribute(
                                    AtfReportHtmlVocabulary.Attribute.FALLBACK_TAB, tab.value())))
                    .append(escape(tab.label()))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.BUTTON));
        }
        html.append(HtmlReportShell.mainAfterNavigation());
        var snapshot = document.snapshot();
        html.append(testsView.render(snapshot));
        html.append(categoryView.render(snapshot));
        html.append(failsView.render(snapshot));
        var environment = new LinkedHashMap<>(systemInfo);
        environment.put("Reporting", "Fallback · embedded offline HTML");
        environment.putAll(ReportEnvironment.reportingSettings(settings).systemDisplayValues());
        html.append(overviewView.render(snapshot, environment));
        html.append(HtmlReportShell.close(navigationScript.source()));
        return html.toString();
    }

    /** Fails before publication if the renderer and the shared semantic model diverge. */
    private void assertSemanticPopulationAligned() {
        var fallbackSnapshot = AtfReportSemanticSnapshot.from(document.snapshot());
        var recordedSnapshot = semantic.snapshot();
        if (!fallbackSnapshot.equals(recordedSnapshot)) {
            throw new IllegalStateException("Fallback semantic snapshot drift between execution model and recorder");
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Report session is closed");
    }

    private static long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
