package atf.reporting.ui;
import atf.reporting.model.ReportObservation;
import java.util.function.LongSupplier;
import atf.reporting.ui.timing.UiPhaseExecution;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.contract.AtfReportPorts;

import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ExtentReportConfiguration;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.network.ReportNetworkCaptureStatus;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * UI-test reporting facade. It deliberately creates a report separate from
 * the API artifact being inspected; the API artifact is an input, never the
 * owner of Playwright execution evidence.
 *
 * <p>Lifecycle state lives in package-private collaborators so this class
 * remains the stable public composition boundary. The nested types are kept
 * for source and binary compatibility with existing UI tests.</p>
 */
public final class UiExtentReportSession implements AutoCloseable {
    public static final String DEFAULT_FILE_NAME = "ui-atf-extent-report.html";

    private final ExtentReportSession delegate;
    private final ReportEvidenceProfile evidenceProfile;
    private final List<ReportEnvironmentSnapshot> scenarioEnvironments = new ArrayList<>();

    private UiExtentReportSession(ExtentReportSession delegate,
                                  ReportEvidenceProfile evidenceProfile) {
        this.delegate = delegate;
        this.evidenceProfile = Objects.requireNonNull(evidenceProfile, "evidenceProfile");
    }

    public static UiExtentReportSession openConfigured() {
        return openConfigured(AtfConsoleSink.noOp());
    }

    public static UiExtentReportSession openConfigured(AtfConsoleSink console) {
        var configuration = AtfReportConfigurationLoader.load();
        return open(configuration, UiExtentReportOptions.builder(configuration.outputDirectory())
                .fileName(DEFAULT_FILE_NAME)
                .console(console)
                .evidenceProfile(ReportEvidenceProfile.forScreenshotPolicy(
                        configuration.screenshotPolicy()))
                .build());
    }

    /** Opens the UI artifact in the directory selected by the UI YAML section. */
    public static UiExtentReportSession openConfigured(Path outputDirectory,
                                                       AtfConsoleSink console) {
        return openConfigured(UiExtentReportOptions.builder(outputDirectory)
                .fileName(DEFAULT_FILE_NAME)
                .console(console)
                .build());
    }

    /** Opens a separately named UI audit artifact without changing API input. */
    public static UiExtentReportSession openConfigured(Path outputDirectory,
                                                       String fileName,
                                                       AtfConsoleSink console) {
        return openConfigured(UiExtentReportOptions.builder(outputDirectory)
                .fileName(fileName)
                .console(console)
                .build());
    }

    /** Opens a UI report with an explicit, browser-neutral screenshot policy. */
    public static UiExtentReportSession openConfigured(Path outputDirectory,
                                                       String fileName,
                                                       AtfConsoleSink console,
                                                       ReportScreenshotPolicy screenshotPolicy) {
        return openConfigured(UiExtentReportOptions.builder(outputDirectory)
                .fileName(fileName)
                .console(console)
                .evidenceProfile(ReportEvidenceProfile.forScreenshotPolicy(screenshotPolicy))
                .build());
    }

    /** Opens a UI report with all evidence axes supplied as one typed profile. */
    public static UiExtentReportSession openConfigured(Path outputDirectory,
                                                       String fileName,
                                                       AtfConsoleSink console,
                                                       ReportEvidenceProfile evidenceProfile) {
        return openConfigured(UiExtentReportOptions.builder(outputDirectory)
                .fileName(fileName)
                .console(console)
                .evidenceProfile(evidenceProfile)
                .build());
    }

    /** Canonical typed configuration entry point; convenience factories delegate here. */
    public static UiExtentReportSession openConfigured(UiExtentReportOptions options) {
        var configuration = AtfReportConfigurationLoader.load();
        return open(configuration, options);
    }

    /** Opens a report whose collision-resistant name was already resolved. */
    public static UiExtentReportSession openConfiguredResolved(Path outputDirectory,
                                                                String resolvedFileName,
                                                                AtfConsoleSink console) {
        return openConfiguredResolved(outputDirectory, resolvedFileName, console, Map.of());
    }

    /** Opens a collision-resistant UI report with consumer-owned System metadata. */
    public static UiExtentReportSession openConfiguredResolved(Path outputDirectory,
                                                                String resolvedFileName,
                                                                AtfConsoleSink console,
                                                                Map<String, String> systemInfo) {
        var configuration = AtfReportConfigurationLoader.load();
        return open(configuration, UiExtentReportOptions.builder(outputDirectory)
                .fileName(resolvedFileName)
                .console(console)
                .evidenceProfile(ReportEvidenceProfile.forScreenshotPolicy(
                        configuration.screenshotPolicy()))
                .systemInfo(systemInfo)
                .timestampEnabled(false)
                .build());
    }

    /** Opens a resolved UI report with allowlisted semantic System metadata. */
    public static UiExtentReportSession openConfiguredResolved(Path outputDirectory,
                                                                String resolvedFileName,
                                                                AtfConsoleSink console,
                                                                ReportEnvironmentSnapshot systemInfo) {
        var configuration = AtfReportConfigurationLoader.load();
        return open(configuration, UiExtentReportOptions.builder(outputDirectory)
                .fileName(resolvedFileName)
                .console(console)
                .evidenceProfile(ReportEvidenceProfile.forScreenshotPolicy(
                        configuration.screenshotPolicy()))
                .environment(systemInfo)
                .timestampEnabled(false)
                .build());
    }

    /** Opens a resolved report with independent typed evidence and System contracts. */
    public static UiExtentReportSession openConfiguredResolved(Path outputDirectory,
                                                                String resolvedFileName,
                                                                AtfConsoleSink console,
                                                                ReportEvidenceProfile evidenceProfile,
                                                                ReportEnvironmentSnapshot systemInfo) {
        var configuration = AtfReportConfigurationLoader.load();
        return open(configuration, UiExtentReportOptions.builder(outputDirectory)
                .fileName(resolvedFileName)
                .console(console)
                .evidenceProfile(evidenceProfile)
                .environment(systemInfo)
                .timestampEnabled(false)
                .build());
    }

    private static UiExtentReportSession open(ExtentReportConfiguration configuration,
                                              UiExtentReportOptions options) {
        var uiConfiguration = new ExtentReportConfiguration(
                options.outputDirectory(), options.fileName(),
                options.timestampEnabled() && configuration.timestampEnabled(),
                configuration.timestampPattern(), configuration.timestampZone(),
                configuration.reportingSettings(), configuration.enabled(),
                options.evidenceProfile().screenshotPolicy());
        Path report = uiConfiguration.resolveReportPath(Clock.systemDefaultZone());
        // Keep test identity in the Tests tree; Overview/System receives only
        // operational metadata from the typed environment projection.
        var systemInfo = new LinkedHashMap<String, String>(options.environment().systemDisplayValues());
        systemInfo.putAll(options.systemInfo());
        return new UiExtentReportSession(new ExtentReportSession(report,
                uiConfiguration.reportingSettings(), new SensitiveDataRedactor(), options.console(),
                systemInfo, options.evidenceProfile().actionDetailsMode()),
                options.evidenceProfile());
    }

    /** Renderer-neutral checkpoint model for component audits and consumer diagnostics. */
    public AtfReportSemanticSnapshot semanticSnapshot() {
        return delegate.semanticSnapshot();
    }

    public Path reportPath() {
        return delegate.reportPath();
    }

    /** Returns the immutable evidence contract selected by the consumer. */
    public ReportEvidenceProfile evidenceProfile() {
        return evidenceProfile;
    }

    /** Includes the resolved inputs of a scenario; differing or absent sibling fields become mixed. */
    public void includeScenarioEnvironment(ReportEnvironmentSnapshot environment) {
        var next = new ArrayList<>(scenarioEnvironments);
        next.add(Objects.requireNonNull(environment, "environment"));
        delegate.updateEnvironment(ReportEnvironmentSnapshot.common(next));
        scenarioEnvironments.add(environment);
    }

    public UiScenario startScenario(String testClass, String testPackage,
                                    String scenario, Collection<String> tags) {
        return startScenario(testClass, testPackage, scenario, tags, evidenceProfile);
    }

    /** Starts one scenario with an explicit profile, independent of sibling scenarios in this report. */
    public UiScenario startScenario(String testClass, String testPackage,
                                    String scenario, Collection<String> tags,
                                    ReportEvidenceProfile scenarioProfile) {
        ReportEvidenceProfile requiredProfile = Objects.requireNonNull(
                scenarioProfile, "scenarioProfile");
        return adaptScenario(delegate.startScenario(testClass, testPackage, scenario, tags), requiredProfile, scenario);
    }

    /** Composes the same UI facade with an explicit renderer-neutral scenario, including a fallback host. */
    public static UiScenario adaptScenario(AtfReportPorts.Scenario scenario, ReportEvidenceProfile profile, String title) {
        return new UiScenario(new UiScenarioLifecycle(Objects.requireNonNull(scenario, "scenario"),
                Objects.requireNonNull(profile, "profile"), title));
    }

    @Override
    public void close() {
        delegate.close();
    }

    public static final class UiScenario implements AutoCloseable {
        private final UiScenarioLifecycle lifecycle;

        private UiScenario(UiScenarioLifecycle lifecycle) {
            this.lifecycle = lifecycle;
        }

        public UiPhase startPhase(String name) {
            return new UiPhase(lifecycle.startPhase(name));
        }

        /** Opts a UNIFIED_FLOW scenario into one chronological flow spanning its phases. */
        public void unifyAcrossPhases() {
            lifecycle.unifyAcrossPhases();
        }

        public void complete(AtfReportStatus status) {
            lifecycle.complete(status);
        }

        @Override
        public void close() {
            lifecycle.close();
        }
    }

    public static final class UiPhase implements AutoCloseable {
        private final UiPhaseLifecycle lifecycle;

        /** Publishes neutral captured facts under this exact phase, without inventing assertions. */
        public void observation(ReportObservation value) { lifecycle.observation(value); }
        public boolean supportsObservations() { return lifecycle.supportsObservations(); }

        public void executionTiming(ReportPhaseTiming timing) { lifecycle.executionTiming(timing); }
        public UiPhaseExecution measureExecution() { return lifecycle.measureExecution(System::nanoTime); }
        public UiPhaseExecution measureExecution(LongSupplier ticks) {
            return lifecycle.measureExecution(ticks);
        }

        private UiPhase(UiPhaseLifecycle lifecycle) {
            this.lifecycle = lifecycle;
        }

        public void step(String keyword, String title, String target,
                         String base64Png, AtfReportStatus status) {
            lifecycle.step(keyword, title, target, base64Png, status);
        }

        public void step(AtfGherkinKeyword keyword, String title, String target,
                         String base64Png, AtfReportStatus status) {
            lifecycle.step(keyword, title, target, base64Png, status);
        }

        public void step(String keyword, String title, String target,
                         String base64Png, Collection<AtfAssertion> assertions,
                         AtfReportStatus status) {
            lifecycle.step(keyword, title, target, base64Png, assertions, status);
        }

        public void step(AtfGherkinKeyword keyword, String title, String target,
                         String base64Png, Collection<AtfAssertion> assertions,
                         AtfReportStatus status) {
            lifecycle.step(keyword, title, target, base64Png, assertions, status);
        }

        public void step(AtfGherkinKeyword keyword, String title, String target,
                         String base64Png, Collection<AtfAssertion> assertions,
                         Collection<AtfUiActionRecord> actions,
                         AtfReportStatus status) {
            lifecycle.step(keyword, title, target, base64Png, assertions, actions, status);
        }

        /** Publishes a checkpoint with an explicit lifecycle point for policy selection. */
        public void step(AtfGherkinKeyword keyword, String title, String target,
                         String base64Png, Collection<AtfAssertion> assertions,
                         Collection<AtfUiActionRecord> actions,
                         ReportScreenshotPolicy.CapturePoint capturePoint,
                         AtfReportStatus status) {
            lifecycle.step(keyword, title, target, base64Png, assertions, actions,
                    capturePoint, status);
        }

        /** Publishes typed UI/API ownership; each API exchange keeps its own assertions and status. */
        public void step(AtfGherkinKeyword keyword, String title, String target,
                         String base64Png, Collection<AtfAssertion> uiAssertions,
                         Collection<AtfUiActionRecord> actions,
                         UiApiValidationEvidence apiValidation,
                         ReportScreenshotPolicy.CapturePoint capturePoint,
                         AtfReportStatus uiStatus) {
            lifecycle.step(keyword, title, target, base64Png, uiAssertions, actions,
                    apiValidation, capturePoint, uiStatus);
        }

        /**
         * Registers the phase's single journey summary. Reporting publishes it
         * after all other phase evidence, immediately before phase close; an
         * explicitly requested empty journey is retained as a zero-exchange summary.
         */
        public void networkInspector(String title,
                                     Collection<ReportExecutionEvidence> evidence) {
            lifecycle.networkInspector(title, evidence);
        }

        /** Registers ordered UI checkpoints with the phase's terminal network summary. */
        public void networkInspector(String title,
                                     Collection<ReportExecutionEvidence> evidence,
                                     Collection<ReportNetworkInspector.Step> steps) {
            lifecycle.networkInspector(title, evidence, steps);
        }

        /** Registers scope and completeness independently of the UI/API business verdict. */
        public void networkInspector(String title,
                                     Collection<ReportExecutionEvidence> evidence,
                                     Collection<ReportNetworkInspector.Step> steps,
                                     ReportNetworkCaptureStatus capture) {
            lifecycle.networkInspector(title, evidence, steps, capture);
        }

        public void setup(String keyword, String title, String target,
                          Collection<AtfAssertion> assertions, AtfReportStatus status) {
            lifecycle.setup(keyword, title, target, assertions, status);
        }

        public void setup(AtfGherkinKeyword keyword, String title, String target,
                          Collection<AtfAssertion> assertions, AtfReportStatus status) {
            lifecycle.setup(keyword, title, target, assertions, status);
        }

        /** Publishes structured setup checks together with sanitized UI/API evidence. */
        public void setup(AtfGherkinKeyword keyword, String title, String target,
                          Collection<AtfAssertion> assertions,
                          Collection<AtfUiActionRecord> actions,
                          UiApiValidationEvidence apiValidation,
                          AtfReportStatus status) {
            lifecycle.setup(keyword, title, target, assertions, actions, apiValidation, status);
        }

        /** Publishes optional sanitized screenshot evidence after the setup action table. */
        public void setup(AtfGherkinKeyword keyword, String title, String target,
                          Collection<AtfAssertion> assertions,
                          Collection<AtfUiActionRecord> actions,
                          UiApiValidationEvidence apiValidation,
                          ReportScreenshot screenshot,
                          AtfReportStatus status) {
            lifecycle.setup(keyword, title, target, assertions, actions, apiValidation, screenshot, status);
        }

        public void outcome(String keyword, String title, String target,
                            AtfReportStatus status) {
            lifecycle.outcome(keyword, title, target, status);
        }

        public void outcome(AtfGherkinKeyword keyword, String title, String target,
                            AtfReportStatus status) {
            lifecycle.outcome(keyword, title, target, status);
        }

        /** Publishes API evidence as a causal sibling immediately after its UI checkpoint. */
        public void apiTrace(AtfGherkinKeyword keyword, String title, String target,
                             UiApiValidationEvidence apiValidation,
                             AtfReportStatus status) {
            lifecycle.apiTrace(keyword, title, target, apiValidation, status);
        }

        public void uiValidation(String title, String target, String base64Png,
                                     Collection<AtfAssertion> assertions,
                                     AtfReportStatus status) {
            lifecycle.uiValidation(title, target, base64Png, assertions, status);
        }

        public void uiValidation(String title, String target, String base64Png,
                                     Collection<AtfAssertion> assertions,
                                     Collection<AtfUiActionRecord> actions,
                                     AtfReportStatus status) {
            lifecycle.uiValidation(title, target, base64Png, assertions, actions, status);
        }

        /** Publishes visual UI evidence at an explicit checkpoint/final point. */
        public void uiValidation(String title, String target, String base64Png,
                                     Collection<AtfAssertion> assertions,
                                     Collection<AtfUiActionRecord> actions,
                                     ReportScreenshotPolicy.CapturePoint capturePoint,
                                     AtfReportStatus status) {
            lifecycle.uiValidation(title, target, base64Png, assertions, actions,
                    capturePoint, status);
        }

        public void uiValidationAsMedia(String title, String target, String base64Png,
                                            Collection<AtfAssertion> assertions,
                                            Collection<AtfUiActionRecord> actions,
                                            AtfReportStatus status) {
            lifecycle.uiValidationAsMedia(title, target, base64Png, assertions, actions, status);
        }

        /** Keeps terminal document/form exchanges alongside the screen that caused them. */
        public void uiValidation(String title, String target, String base64Png,
                                     Collection<AtfAssertion> assertions,
                                     Collection<AtfUiActionRecord> actions,
                                     UiApiValidationEvidence apiValidation,
                                     ReportScreenshotPolicy.CapturePoint capturePoint,
                                     AtfReportStatus status) {
            lifecycle.uiValidation(title, target, base64Png, assertions, actions, apiValidation,
                    capturePoint, status);
        }

        public void failureEvidence(String title, String target,
                                    Collection<AtfUiActionRecord> actions,
                                    Throwable failure) {
            lifecycle.failureEvidence(title, target, actions, failure, null);
        }

        public void failureEvidence(String title, String target,
                                    Collection<AtfUiActionRecord> actions,
                                    Throwable failure, String base64Png) {
            lifecycle.failureEvidence(title, target, actions, failure, base64Png);
        }

        /** Completes the last phase and publishes one flow with retained phase checkpoints. */
        public void completeFinal(AtfReportStatus status) {
            lifecycle.completeFinal(status);
        }

        public void complete(AtfReportStatus status) {
            lifecycle.complete(status);
        }

        @Override
        public void close() {
            lifecycle.close();
        }
    }
}
