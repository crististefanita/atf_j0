package atf.reporting.extent.session;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.evidence.ExtentEvidenceRenderer;
import atf.reporting.extent.ExtentScenarioScope;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.model.ReportField;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.model.ReportTagNormalizer;
import atf.reporting.semantic.AtfReportSemanticRecorder;
import atf.reporting.semantic.AtfReportSemanticSnapshot;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.ui.model.AtfUiActionRecord;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns the Extent tree and creates the lifecycle scopes behind the session facade. */
public final class ExtentReportTree {
    private final ExtentReports reports;
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor;
    private final ExtentEvidenceRenderer evidence;
    private final ReportUiActionDetailsMode defaultDetailsMode;
    private final Runnable ensureSessionOpen;
    private final AtfConsoleSink console;
    private final LifecycleStartProbe startProbe;
    private final AtfReportSemanticRecorder semantic = new AtfReportSemanticRecorder();
    private final Map<String, ExtentTest> classRoots = new LinkedHashMap<>();
    private final Map<String, ReportStatusAggregator> classStatuses = new LinkedHashMap<>();
    private final Map<String, String> classPackages = new LinkedHashMap<>();

    public ExtentReportTree(ExtentReports reports, ReportingSettings settings,
                            SensitiveDataRedactor redactor, Runnable ensureSessionOpen) {
        this(reports, settings, redactor, ensureSessionOpen, AtfConsoleSink.noOp());
    }

    public ExtentReportTree(ExtentReports reports, ReportingSettings settings,
                            SensitiveDataRedactor redactor, Runnable ensureSessionOpen,
                            AtfConsoleSink console) {
        this(reports, settings, redactor, ensureSessionOpen, console,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    public ExtentReportTree(ExtentReports reports, ReportingSettings settings,
                            SensitiveDataRedactor redactor, Runnable ensureSessionOpen,
                            AtfConsoleSink console,
                            ReportUiActionDetailsMode detailsMode) {
        this(reports, settings, redactor, ensureSessionOpen, console, detailsMode,
                LifecycleStartProbe.noOp());
    }

    ExtentReportTree(ExtentReports reports, ReportingSettings settings,
                     SensitiveDataRedactor redactor, Runnable ensureSessionOpen,
                     AtfConsoleSink console, ReportUiActionDetailsMode detailsMode,
                     LifecycleStartProbe startProbe) {
        this.reports = reports;
        this.settings = settings;
        this.redactor = redactor;
        this.evidence = new ExtentEvidenceRenderer(settings, redactor, detailsMode);
        this.defaultDetailsMode = detailsMode;
        this.ensureSessionOpen = ensureSessionOpen;
        this.console = AtfConsoleSink.bestEffort(console);
        this.startProbe = startProbe;
    }

    public AtfReportPorts.Scenario startScenario(String testClass, String scenario,
                                                  Collection<String> tags) {
        return startScenario(testClass, null, scenario, tags);
    }

    public AtfReportPorts.Scenario startScenario(String testClass, String testPackage,
                                                  String scenario, Collection<String> tags) {
        ensureOpen();
        // Normalize caller-owned input before creating either Extent or semantic nodes.
        // A faulty Collection implementation must not leave a half-started scenario.
        List<String> normalizedTags = ReportTagNormalizer.normalize(tags);
        String className = testClass == null || testClass.isBlank() ? "API tests" : testClass;
        String packageName = testPackage == null ? "" : testPackage.trim();
        boolean newRoot = !classRoots.containsKey(className);
        ExtentTest classRoot = null;
        ExtentTest test = null;
        try {
            classRoot = classRoots.get(className);
            if (classRoot == null) {
                classRoot = reports.createTest(className);
                if (!packageName.isBlank()) classRoot.getModel().setDescription(packageName);
                ExtentNodeLifecycle.start(classRoot);
                classRoots.put(className, classRoot);
            }
            test = classRoot.createNode(scenario == null ? "Scenario" : scenario);
            ExtentNodeLifecycle.start(test);
            if (!normalizedTags.isEmpty()) test.assignCategory(normalizedTags.toArray(String[]::new));
            if (!packageName.isBlank()) classPackages.putIfAbsent(className, packageName);
            classStatuses.computeIfAbsent(className, ignored -> new ReportStatusAggregator());
            startProbe.beforeScenarioSemantic();
            var semanticScenario = semantic.startScenario(className, packageName, scenario, normalizedTags);
            consoleStarted(AtfConsoleEvent.Scope.SCENARIO,
                    className + " :: " + (scenario == null ? "Scenario" : scenario));
            return new ExtentScenarioScope(this, test, className, semanticScenario);
        } catch (RuntimeException | Error failure) {
            rollbackScenarioStart(className, newRoot, classRoot, test, failure);
            throw failure;
        }
    }

    private void rollbackScenarioStart(String className, boolean newRoot,
                                       ExtentTest classRoot, ExtentTest test, Throwable failure) {
        if (classRoot != null && test != null) {
            try {
                if (!classRoot.getModel().getChildren().remove(test.getModel())) {
                    throw new IllegalStateException("Extent scenario was not attached to its class root");
                }
            } catch (RuntimeException | Error rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
        }
        if (newRoot && classRoot != null) {
            try {
                if (classRoot.getModel().getChildren().isEmpty()) {
                    reports.removeTest(classRoot);
                    classRoots.remove(className);
                    classStatuses.remove(className);
                    classPackages.remove(className);
                }
            } catch (RuntimeException | Error rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
        }
    }

    /** Invoked by the phase scope immediately before recording its semantic peer. */
    public void beforePhaseSemantic(ExtentTest phase) { startProbe.beforePhaseSemantic(phase); }

    /** Removes a phase whose start did not return a usable handle. */
    public void rollbackPhaseStart(ExtentTest scenario, ExtentTest phase, Throwable failure) {
        if (phase == null) return;
        try {
            if (!scenario.getModel().getChildren().remove(phase.getModel())) {
                throw new IllegalStateException("Extent phase was not attached to its scenario");
            }
        } catch (RuntimeException | Error rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    public void completeClassRoots() {
        classStatuses.forEach((name, status) -> {
            var root = classRoots.get(name);
            if (root != null) {
                ExtentNodeLifecycle.complete(root, status.aggregate());
                ExtentNodeLifecycle.end(root);
            }
        });
    }

    public void ensureOpen() { ensureSessionOpen.run(); }

    public ReportingSettings settings() { return settings; }
    public SensitiveDataRedactor redactor() { return redactor; }
    public ReportUiActionDetailsMode defaultDetailsMode() { return defaultDetailsMode; }
    public AtfReportSemanticSnapshot semanticSnapshot() { return semantic.snapshot(); }
    public ExtentTest classRoot(String className) { return classRoots.get(className); }

    public String renderRequestTitle(String title, String method, String path) {
        return ExtentReportNodeSupport.renderRequestTitle(title, method, path);
    }

    public String normalizeDetailName(String name) {
        return ExtentReportNodeSupport.normalizeDetailName(name);
    }

    public void recordClassStatus(String className, AtfReportStatus status) {
        classStatuses.get(className).record(status);
    }

    public void complete(ExtentTest node, AtfReportStatus status) {
        ExtentNodeLifecycle.complete(node, status);
    }

    public void markStarted(ExtentTest node) { ExtentNodeLifecycle.start(node); }
    public void markEnded(ExtentTest node) { ExtentNodeLifecycle.end(node); }

    public void renderRequestDetails(ExtentTest node, List<ReportField> details,
                                     List<ReportJsonBody> json) {
        evidence.renderRequestDetails(node, details, json);
    }

    public void renderPhaseDiagnostics(ExtentTest scenario, List<ReportPhaseDiagnostic> diagnostics) {
        evidence.renderPhaseDiagnostics(scenario, diagnostics);
    }

    public void renderResponseDetails(ExtentTest node, List<ReportField> details,
                                      List<ReportJsonBody> json) {
        evidence.renderResponseDetails(node, details, json);
    }

    public void renderUiFlowProvenance(ExtentTest node, List<ReportField> details) {
        evidence.renderUiFlowProvenance(node, details);
    }

    public void renderSetupEvidence(ExtentTest node, List<ReportField> details) {
        evidence.renderSetupEvidence(node, details);
    }

    public void renderNetworkAggregate(ExtentTest node,
                                       ReportNetworkInspector.JourneyAggregate aggregate) {
        evidence.renderNetworkAggregate(node, aggregate);
    }

    public void renderNetworkAggregateSubstep(ExtentTest node,
                                              ReportNetworkInspector.JourneyAggregate aggregate) {
        evidence.renderNetworkAggregateSubstep(node, aggregate);
    }

    /** Publishes one renderer-neutral transport evidence block at the request boundary. */
    public void renderExecution(ExtentTest node, ReportExecutionEvidence execution) {
        evidence.renderExecution(node, execution);
    }

    public void renderScreenshots(ExtentTest node, List<ReportScreenshot> screenshots) {
        evidence.renderScreenshots(node, screenshots);
    }

    public void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions) {
        evidence.renderUiActions(node, actions, List.of());
    }

    public void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions,
                                List<ReportScreenshot> screenshots) {
        evidence.renderUiActions(node, actions, screenshots);
    }

    public void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions,
                                List<ReportScreenshot> screenshots, ReportUiActionDetailsMode mode) {
        evidence.renderUiActions(node, actions, screenshots, mode);
    }

    /** Renders a Playwright action table without embedding its screenshot. */
    public void renderUiActionsAsMedia(ExtentTest node, List<AtfUiActionRecord> actions) {
        evidence.renderUiActionsAsMedia(node, actions);
    }

    public void renderUiActionsAsMedia(ExtentTest node, List<AtfUiActionRecord> actions,
                                       ReportUiActionDetailsMode mode) {
        evidence.renderUiActionsAsMedia(node, actions, mode);
    }

    public void renderAssertions(ExtentTest node, Collection<AtfAssertion> assertions) {
        renderAssertions(node, assertions, ReportEvidenceOwner.UNSPECIFIED);
    }

    public void renderAssertions(ExtentTest node, Collection<AtfAssertion> assertions, ReportEvidenceOwner owner) {
        evidence.renderAssertions(node, assertions, owner);
    }

    public void consoleStarted(AtfConsoleEvent.Scope scope, String name) {
        publishConsoleSafely(AtfConsoleEvent.started(scope, name));
    }

    public void consoleDetail(AtfConsoleEvent.Scope scope, String name, String value) {
        publishConsoleSafely(AtfConsoleEvent.detail(scope, name, value));
    }

    public void consoleCode(AtfConsoleEvent.Scope scope, String name, String value) {
        publishConsoleSafely(AtfConsoleEvent.code(scope, name, value));
    }

    public void consoleCompleted(AtfConsoleEvent.Scope scope, String name,
                                 AtfReportStatus status, ExtentTest node) {
        publishConsoleSafely(AtfConsoleEvent.completed(scope, name, status,
                ExtentNodeLifecycle.durationMillis(node)));
    }

    private void publishConsoleSafely(AtfConsoleEvent event) {
        console.publish(event);
    }

}
