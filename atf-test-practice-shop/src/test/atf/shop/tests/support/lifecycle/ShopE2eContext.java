package atf.shop.tests.support.lifecycle;
import atf.reporting.ui.timing.UiPhaseExecution;

import atf.reporting.ui.validation.UiApiValidationEvidence;

import atf.console.AtfConsoleReporter;
import atf.console.junit.AtfConsoleTestContext;
import atf.core.diagnostics.ThrowableDiagnostics;
import atf.core.ui.AtfUiActionRecord;
import atf.testkit.lifecycle.FailureAwareCleanup;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.playwright.UiScreenValidation;
import atf.shop.config.ShopConfiguration;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.ShopAuditObservations;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.evidence.ShopBrowserCaptureProvider;
import atf.shop.tests.support.reporting.ShopReportSystemMetadata;
import atf.reporting.api.ReportModelAdapters;
import atf.ui.audit.UiScreenAudit;
import atf.shop.reporting.ShopScreenshotPolicy;
import com.microsoft.playwright.Page;

import java.nio.file.Path;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Owns the browser/report lifecycle for one external Practice Shop suite. */
public final class ShopE2eContext implements AutoCloseable {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private static final String NETWORK_INSPECTOR_SUMMARY_TITLE =
            "API network inspector — journey summary";
    private final ShopConfiguration configuration;
    private final UiExtentReportSession report;
    private final ReportEvidenceProfile evidenceProfile;
    private final boolean ownsReport;
    private final PlaywrightUiSession browser;
    private final ShopBrowserCaptureProvider browserCaptureProvider;
    private final ShopApiNetworkJournal apiNetwork;
    private final ReportEnvironmentSnapshot environment;
    private atf.ui.playwright.recovery.UiBackendRecovery backendRecovery;
    private PlaywrightActionSequence recoveryActions;
    private boolean closed;

    private ShopE2eContext(ShopConfiguration configuration,
                                   ReportEvidenceProfile evidenceProfile,
                                   UiExtentReportSession report, PlaywrightUiSession browser,
                                   ShopBrowserCaptureProvider browserCaptureProvider,
                                   boolean ownsReport,
                                   ShopApiNetworkJournal apiNetwork,
                                   ReportEnvironmentSnapshot environment) {
        this.configuration = configuration; this.evidenceProfile = evidenceProfile;
        this.report = report; this.browser = browser;
        this.browserCaptureProvider = browserCaptureProvider;
        this.ownsReport = ownsReport;
        this.apiNetwork = apiNetwork;
        this.environment = environment;
    }

    public static ShopE2eContext open(ShopConfiguration configuration) {
        return open(configuration, ShopScreenshotPolicy.profile(configuration.reporting()));
    }

    public static ShopE2eContext open(ShopConfiguration configuration,
                                               ReportEvidenceProfile evidenceProfile) {
        return open(configuration, evidenceProfile, null);
    }

    /** Opens a browser harness that publishes into a suite-owned Extent session. */
    public static ShopE2eContext open(ShopConfiguration configuration,
                                               ReportEvidenceProfile evidenceProfile,
                                               UiExtentReportSession sharedReport) {
        return open(configuration, evidenceProfile, sharedReport,
                atf.reporting.console.AtfConsoleSink.noOp());
    }

    /** Opens a browser harness with an explicit sink for a report it owns. */
    public static ShopE2eContext open(ShopConfiguration configuration,
                                     ReportEvidenceProfile evidenceProfile,
                                     UiExtentReportSession sharedReport,
                                     atf.reporting.console.AtfConsoleSink reportConsole) {
        ReportEvidenceProfile selectedProfile = java.util.Objects.requireNonNull(
                evidenceProfile, "evidenceProfile");
        var environment = ShopReportSystemMetadata.snapshot(configuration, selectedProfile,
                System.getProperty("atf.environment", ShopReportSystemMetadata.DEFAULT_ENVIRONMENT));
        boolean ownsReport = sharedReport == null;
        UiExtentReportSession report = ownsReport
                ? openReport(configuration, selectedProfile, reportConsole) : sharedReport;
        try {
            return openBrowser(configuration, selectedProfile, report, ownsReport, environment);
        } catch (RuntimeException | Error error) {
            if (ownsReport) FailureAwareCleanup.closeAfterFailure(error, report);
            throw error;
        }
    }

    /** Creates the single report resource owned by an opt-in Practice Shop suite run. */
    public static UiExtentReportSession openReport(ShopConfiguration configuration,
                                                   ReportEvidenceProfile evidenceProfile) {
        return openReport(configuration, evidenceProfile, atf.reporting.console.AtfConsoleSink.noOp());
    }

    /** Creates the suite-owned report with an explicit terminal notification sink. */
    public static UiExtentReportSession openReport(ShopConfiguration configuration,
                                                   ReportEvidenceProfile evidenceProfile,
                                                   atf.reporting.console.AtfConsoleSink console) {
        ReportEvidenceProfile selectedProfile = java.util.Objects.requireNonNull(
                evidenceProfile, "evidenceProfile");
        String file = "ui-atf-practice-shop-e2e_"
                + STAMP.withZone(Clock.systemDefaultZone().getZone()).format(Clock.systemDefaultZone().instant())
                + ".html";
        return UiExtentReportSession.openConfiguredResolved(
                configuration.uiReportDirectory(), file,
                java.util.Objects.requireNonNull(console, "console"),
                selectedProfile,
                ShopReportSystemMetadata.snapshot(configuration, selectedProfile,
                        System.getProperty("atf.environment", ShopReportSystemMetadata.DEFAULT_ENVIRONMENT)));
    }

    private static ShopE2eContext openBrowser(ShopConfiguration configuration,
                                                       ReportEvidenceProfile evidenceProfile,
                                                       UiExtentReportSession report,
                                                       boolean ownsReport,
                                                       ReportEnvironmentSnapshot environment) {
        AtfConsoleSink console = AtfConsoleTestContext.current() == null
                ? AtfConsoleReporter.configuredStdout()
                : AtfConsoleTestContext.configuredSink();
        PlaywrightUiSession browser = null;
        try {
            browser = PlaywrightUiSession.open(configuration.browser(), console);
            ShopBrowserCaptureProvider browserCaptureProvider =
                    new ShopBrowserCaptureProvider(browser.page(),
                            UiScreenValidationConfiguration.defaults());
            ShopApiNetworkJournal apiNetwork =
                    new ShopApiNetworkJournal(configuration.validationProfile(),
                            configuration.api().evidenceBinding());
            // Keep the Shop journal on its typed Page response adapter. The
            // generic inspector remains available for reusable consumers, but
            // registration navigation can replace the execution context while
            // its response callback is still settling.
            apiNetwork.attach(browser.page());
            return new ShopE2eContext(configuration, evidenceProfile, report, browser, browserCaptureProvider,
                    ownsReport, apiNetwork, environment);
        } catch (RuntimeException | Error error) {
            FailureAwareCleanup.closeAfterFailure(error, browser);
            throw error;
        }
    }

    private static Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            if (java.nio.file.Files.isRegularFile(current.resolve("atf-reporting/pom.xml"))) return current;
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    /** Shared component seam for setup failures; keeps the real cause on the setup phase. */
    static void publishSetupFailureEvidence(UiExtentReportSession.UiPhase phase, String target,
                                            Collection<atf.reporting.ui.model.AtfUiActionRecord> actions,
                                            Throwable failure, String screenshot) {
        phase.failureEvidence("authenticated account setup failure", target,
                actions, failure, screenshot);
    }

    static List<AtfUiActionRecord> terminalContractEvidence(
            PlaywrightActionSequence actions, Page page, String title,
            Collection<AtfAssertion> assertions, UiScreenValidation.Evidence screenshot) {
        return UiScreenAudit.drain(actions, title, page.url(), assertions.size(),
                screenshot, true, false);
    }

    public Page page() { return browser.page(); }
    public ShopConfiguration configuration() { return configuration; }
    public PlaywrightActionSequence actions() { return browser.newActionSequence(); }
    public atf.ui.playwright.recovery.UiBackendRecovery backendRecovery(PlaywrightActionSequence actions) {
        if (closed) throw new IllegalStateException("Shop invocation is closed");
        java.util.Objects.requireNonNull(actions, "actions");
        if (backendRecovery == null) {
            backendRecovery = atf.shop.support.recovery.ShopBackendRecovery.open(page(), configuration, actions);
            recoveryActions = actions;
        } else if (recoveryActions != actions) {
            throw new IllegalStateException("One Shop invocation must retain its recovery recorder and deadline owner");
        }
        return backendRecovery;
    }
    public Path reportPath() { return report.reportPath(); }
    public ShopApiNetworkJournal apiNetwork() { return apiNetwork; }
    public ReportScreenshot captureCheckpointScreenshot(String title,
            ReportScreenshotPolicy.CapturePoint capturePoint) {
        return evidenceProfile.capture(capturePoint, title, page().url(), browserCaptureProvider)
                .orElse(null);
    }

    public Scenario scenario(String title, String overview) {
        return scenario("ShopE2eTest", title, overview);
    }

    /** Starts a report scenario with the concrete test identity of the caller. */
    public Scenario scenario(String reportClass, String title, String overview) {
        UiExtentReportSession.UiScenario scenario = startReportScenario(reportClass, overview);
        var setup = scenario.startPhase("SETUP");
        setup.measureExecution();
        List<AtfAssertion> setupAssertions = setupAssertions(configuration);
        AtfReportStatus setupStatus = setupAssertions.stream().allMatch(AtfAssertion::passed)
                ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        boolean authenticated = configuration.authenticatedE2eAllowed();
        ShopPurchaseTraceStep initialSetup = authenticated
                ? ShopPurchaseTraceStep.AUTHENTICATED_FIXTURE_READY
                : ShopPurchaseTraceStep.GUEST_CONFIGURATION_READY;
        setup.setup(initialSetup.keyword(), initialSetup.title(),
                authenticated ? configuration.accountPolicy().evidenceTarget() : "YAML checkout inputs",
                setupAssertions, setupStatus);
        setup.complete(setupStatus);
        return new Scenario(scenario, title, null);
    }

    /** Opens SETUP and defers TEST BODY until the caller proves account setup succeeded. */
    public Scenario scenarioWithExplicitSetup(String reportClass, String title, String overview) {
        UiExtentReportSession.UiScenario scenario = startReportScenario(reportClass, overview);
        return new Scenario(scenario, title, scenario.startPhase("SETUP"));
    }

    private UiExtentReportSession.UiScenario startReportScenario(String reportClass, String overview) {
        report.includeScenarioEnvironment(environment);
        return startReportScenario(report, evidenceProfile, reportClass, overview);
    }

    static UiExtentReportSession.UiScenario startReportScenario(
            UiExtentReportSession report, ReportEvidenceProfile evidenceProfile,
            String reportClass, String overview) {
        UiExtentReportSession.UiScenario scenario = report.startScenario(reportClass, "atf.shop.tests", overview,
                List.of("ui", "e2e", "external", "practice-shop"), evidenceProfile);
        if (evidenceProfile.flowMode() == ReportEvidenceFlowMode.UNIFIED_FLOW) {
            scenario.unifyAcrossPhases();
        }
        return scenario;
    }

    static List<AtfAssertion> setupAssertions(ShopConfiguration configuration) {
        boolean accountFixtureRequired = configuration.authenticatedE2eEnabled();
        boolean accountConfigured = accountFixtureRequired && configuration.accountConfigured();
        boolean authenticated = configuration.authenticatedE2eAllowed();
        String selectedMode = authenticated ? "authenticated" : "guest";
        String expectedMode = configuration.authenticatedE2eEnabled() && accountConfigured
                ? "authenticated" : "guest";
        String profile = configuration.validationProfile().name().toLowerCase().replace('_', '-');
        var accountPolicy = configuration.accountPolicy();
        boolean policyAccepted = !accountFixtureRequired || accountPolicy.accepts(configuration.accountFile());
        String observedPolicy = !accountFixtureRequired
                ? "not required for guest journey"
                : policyAccepted ? accountPolicy.displayName() + "; credentials redacted"
                : "fixture path outside module-owned src/test/resources";
        String modeEvidence = authenticated ? "authenticated checkout; account fixture available"
                : "guest checkout; account fixture not required";
        return List.of(
                new AtfAssertion("Checkout mode", expectedMode + " checkout is selected",
                        modeEvidence,
                        expectedMode.equals(selectedMode)),
                new AtfAssertion("Account setup",
                        authenticated ? "pre-registered account file is available" : "not required for guest journey",
                        authenticated
                                ? (accountConfigured ? "account file available; credentials redacted" : "account file missing")
                                : "not required for guest journey",
                        !authenticated || accountConfigured),
                new AtfAssertion("Account fixture policy",
                        accountFixtureRequired ? accountPolicy.expectedEvidence() : "not required for guest journey",
                        observedPolicy,
                        policyAccepted),
                new AtfAssertion("API evidence profile", "ui-api or ui-only", profile,
                        "ui-api".equals(profile) || "ui-only".equals(profile)));
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        Throwable failure = null;
        if (backendRecovery != null) failure = FailureAwareCleanup.runAll(failure, backendRecovery::close);
        try {
            browser.close();
        } catch (Throwable error) {
            if (failure == null) failure = error; else failure.addSuppressed(error);
        } finally {
            if (ownsReport) try {
                report.close();
            } catch (Throwable error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new IllegalStateException("Practice Shop E2E teardown failed", failure);
    }

    public final class Scenario implements AutoCloseable {
        private final UiExtentReportSession.UiScenario scenario;
        private final UiExtentReportSession.UiPhase setupPhase;
        private UiExtentReportSession.UiPhase body;
        private final UiPhaseExecution setupExecution;
        private UiPhaseExecution bodyExecution;
        private final String title;
        private final List<ReportNetworkInspector.Step> networkSteps = new ArrayList<>();
        private boolean complete;
        private boolean networkInspectorPublished;
        private boolean setupStepPublished;
        private boolean setupFinished;

        private Scenario(UiExtentReportSession.UiScenario scenario, String title,
                         UiExtentReportSession.UiPhase setupPhase) {
            this.scenario = scenario;
            this.setupPhase = setupPhase;
            this.setupExecution = setupPhase == null ? null : setupPhase.measureExecution();
            this.body = setupPhase == null ? scenario.startPhase("TEST BODY") : null;
            this.bodyExecution = body == null ? null : body.measureExecution();
            this.title = title == null || title.isBlank() ? "completed Practice Shop scenario" : title;
        }

        /** Publishes setup evidence and captures after every non-empty Playwright action table. */
        public void setupCheckpoint(Page page, PlaywrightActionSequence actions, AtfGherkinKeyword keyword,
                                    String stepTitle,
                                    Collection<AtfAssertion> assertions,
                                    Collection<ReportExecutionEvidence> apiEvidence,
                                    AtfReportStatus status) {
            if (setupPhase == null || setupFinished) {
                throw new IllegalStateException("Explicit authenticated setup is not open");
            }
            ReportScreenshotPolicy.CapturePoint point = status == AtfReportStatus.PASS
                    ? ReportScreenshotPolicy.CapturePoint.CHECKPOINT
                    : ReportScreenshotPolicy.CapturePoint.FAILURE;
            ReportScreenshot screenshot = actions.snapshot().isEmpty()
                    ? null : setupExecution.capture(() -> captureCheckpointScreenshot(stepTitle, point));
            setupPhase.setup(keyword, stepTitle, page.url(), assertions,
                    ReportModelAdapters.uiActions(actions.drain()), new UiApiValidationEvidence(List.of(), (apiEvidence).stream()
                            .map(exchange -> new UiApiValidationEvidence.Exchange(exchange, List.of())).toList()), screenshot, status);
            setupStepPublished = true;
        }

        /** The business body does not exist in the report until setup has passed. */
        public void finishSetup(AtfReportStatus status) {
            if (setupPhase == null || setupFinished) {
                throw new IllegalStateException("Explicit authenticated setup is not open");
            }
            if (!setupStepPublished) {
                throw new IllegalStateException("SETUP must publish at least one account step before it can finish");
            }
            if (status == AtfReportStatus.PASS) setupPhase.complete(status);
            else setupPhase.completeFinal(status);
            setupFinished = true;
            if (status == AtfReportStatus.PASS) {
                body = scenario.startPhase("TEST BODY");
                bodyExecution = body.measureExecution();
            } else {
                scenario.complete(status);
                complete = true;
            }
        }

        /**
         * Keeps failure publication and scenario closing out of the test body.
         * Tests remain a readable business narrative; this boundary owns the
         * exceptional lifecycle and preserves the original failure.
         */
        public void execute(Page page, PlaywrightActionSequence actions, Runnable testBody) {
            executeWithFailureCapture(testBody, failure -> fail(page, actions, failure), this::close);
        }

        static void executeWithFailureCapture(Runnable testBody, Consumer<Throwable> failureHandler) {
            executeWithFailureCapture(testBody, failureHandler, () -> { });
        }

        static void executeWithFailureCapture(Runnable testBody, Consumer<Throwable> failureHandler,
                                              Runnable closeAction) {
            Throwable primaryFailure = null;
            try {
                testBody.run();
            } catch (RuntimeException | AssertionError | LinkageError failure) {
                primaryFailure = failure;
                try {
                    failureHandler.accept(failure);
                } catch (RuntimeException | Error publicationFailure) {
                    suppress(failure, publicationFailure);
                }
                throw failure;
            } finally {
                try {
                    closeAction.run();
                } catch (RuntimeException | Error closeFailure) {
                    if (primaryFailure == null) throw closeFailure;
                    suppress(primaryFailure, closeFailure);
                }
            }
        }

        private static void suppress(Throwable primary, Throwable secondary) {
            if (primary != secondary) primary.addSuppressed(secondary);
        }

        public void checkpoint(Page page, PlaywrightActionSequence actions,
                               ShopPurchaseCheckpoint checkpoint) {
            if (body == null) throw new IllegalStateException("TEST BODY cannot start before setup passes");
            var capturePoint = checkpoint.passed()
                    ? ReportScreenshotPolicy.CapturePoint.CHECKPOINT
                    : ReportScreenshotPolicy.CapturePoint.FAILURE;
            String payload = !shouldCaptureCheckpointScreenshot(
                    evidenceProfile, !actions.snapshot().isEmpty(), checkpoint.passed())
                    ? "" : capture(page, checkpoint.title(), capturePoint);
            UiScreenValidation.Evidence evidence = new UiScreenValidation.Evidence(
                    checkpoint.title(), payload, !payload.isBlank());
            ShopAuditObservations.recordCheckpoint(actions, page, checkpoint, evidence);
            networkSteps.add(ReportNetworkInspector.Step.from(checkpoint.keyword().text(), checkpoint.title(),
                    checkpoint.assertions(), checkpoint.apiEvidence()));
            AtfReportStatus checkpointStatus = checkpoint.passed()
                    ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            AtfReportStatus uiStatus = checkpoint.assertions().stream().allMatch(AtfAssertion::passed)
                    ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            body.step(checkpoint.keyword(), checkpoint.title(), page.url(), evidence.base64Png(),
                    checkpoint.assertions(), ReportModelAdapters.uiActions(actions.drain()),
                    checkpoint.apiValidation(), capturePoint, uiStatus);
        }

        /** Unified-flow reports reserve their single screenshot for the terminal screen. */
        public static boolean shouldCaptureCheckpointScreenshot(ReportEvidenceProfile profile,
                                                                boolean hasActions,
                                                                boolean checkpointPassed) {
            if (!hasActions) return false;
            return profile.flowMode() != ReportEvidenceFlowMode.UNIFIED_FLOW || !checkpointPassed;
        }

        /** Final screen contract for the optional authenticated catalog scenarios. */
        public void pass(Page page, PlaywrightActionSequence actions,
                         String evidenceTitle,
                         Collection<AtfAssertion> assertions) {
            if (body == null) throw new IllegalStateException("TEST BODY cannot start before setup passes");
            bodyExecution.finishExecution();
            String payload = capture(page, evidenceTitle, ReportScreenshotPolicy.CapturePoint.FINAL);
            UiScreenValidation.Evidence evidence = new UiScreenValidation.Evidence(
                    evidenceTitle, payload, !payload.isBlank());
            var actionEvidence = ReportModelAdapters.uiActions(
                    terminalContractEvidence(actions, page, title, assertions, evidence));
            if (evidence.hasScreenshotPayload()) {
                body.uiValidation(title, page.url(), evidence.base64Png(),
                        assertions, actionEvidence,
                        ReportScreenshotPolicy.CapturePoint.FINAL, AtfReportStatus.PASS);
            } else {
                body.step(AtfGherkinKeyword.THEN, title, page.url(), "", assertions,
                        actionEvidence, UiApiValidationEvidence.empty(), ReportScreenshotPolicy.CapturePoint.FINAL,
                        AtfReportStatus.PASS);
            }
            publishNetworkInspector();
            body.completeFinal(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            complete = true;
        }

        public void complete() {
            complete(AtfReportStatus.PASS);
        }

        public void complete(AtfReportStatus status) {
            if (body == null) throw new IllegalStateException("TEST BODY cannot start before setup passes");
            bodyExecution.finishExecution();
            publishNetworkInspector();
            body.completeFinal(status);
            scenario.complete(status);
            complete = true;
        }

        public void fail(Page page, PlaywrightActionSequence actions, Throwable failure) {
            if (complete) return;
            (bodyExecution == null ? setupExecution : bodyExecution).finishExecution();
            if (body == null && setupPhase != null) {
                if (setupFinished) {
                    // A setup owner may already have published and closed a failed checkpoint.
                    // Never dereference the not-yet-created TEST BODY or mask the original cause.
                    complete = true;
                    return;
                }
                try {
                    if (!setupStepPublished) {
                        AtfAssertion failed = new AtfAssertion("Authenticated account setup",
                                "registration/login setup passes before TEST BODY",
                                ThrowableDiagnostics.unavailable(failure), false);
                        setupPhase.setup(ShopPurchaseTraceStep.ACCOUNT_SETUP_FAILED.keyword(),
                                ShopPurchaseTraceStep.ACCOUNT_SETUP_FAILED.title(),
                                page.url(), List.of(failed),
                                List.of(), new UiApiValidationEvidence(List.of(), (setupEvidence()).stream()
                            .map(exchange -> new UiApiValidationEvidence.Exchange(exchange, List.of())).toList()), null, AtfReportStatus.FAIL);
                        setupStepPublished = true;
                    }
                    publishSetupFailureEvidence(page, actions, failure);
                    setupPhase.completeFinal(AtfReportStatus.FAIL);
                    setupFinished = true;
                    scenario.complete(AtfReportStatus.FAIL);
                } catch (IllegalStateException alreadyClosed) {
                    // Preserve the original setup failure if report publication already closed the phase.
                    failure.addSuppressed(alreadyClosed);
                } catch (RuntimeException reportFailure) {
                    failure.addSuppressed(reportFailure);
                } finally {
                    complete = true;
                }
                return;
            }
            try {
                String screenshot = "";
                try {
                    screenshot = capture(page, "external E2E failure",
                            ReportScreenshotPolicy.CapturePoint.FAILURE);
                } catch (RuntimeException captureFailure) {
                    failure.addSuppressed(captureFailure);
                }
                body.failureEvidence("external E2E failure", page.url(),
                        ReportModelAdapters.uiActions(actions.drain()), failure, screenshot);
                publishNetworkInspector();
                body.completeFinal(AtfReportStatus.FAIL);
                scenario.complete(AtfReportStatus.FAIL);
            } catch (IllegalStateException alreadyClosed) {
                // A renderer may close the scenario while publishing a partial failure.
                // Preserve the browser/test exception rather than masking it.
                failure.addSuppressed(alreadyClosed);
            } catch (RuntimeException reportFailure) {
                failure.addSuppressed(reportFailure);
            } finally {
                complete = true;
            }
        }

        /** Retains the real setup exception and action trace even though TEST BODY never started. */
        void publishSetupFailureEvidence(Page page, PlaywrightActionSequence actions, Throwable failure) {
            String screenshot = "";
            try {
                screenshot = capture(page, "authenticated account setup failure",
                        ReportScreenshotPolicy.CapturePoint.FAILURE);
            } catch (RuntimeException captureFailure) {
                failure.addSuppressed(captureFailure);
            }
            ShopE2eContext.publishSetupFailureEvidence(setupPhase, page.url(),
                    ReportModelAdapters.uiActions(actions.drain()), failure, screenshot);
        }

        private String capture(Page page, String title,
                               ReportScreenshotPolicy.CapturePoint point) {
            return (bodyExecution == null ? setupExecution : bodyExecution).capture(() ->
                    evidenceProfile.capture(point, title, page.url(), browserCaptureProvider)
                            .map(ReportScreenshot::base64Png).orElse(""));
        }

        @Override public void close() {
            if (!complete) {
                if (body == null) {
                    fail(page(), actions(), new IllegalStateException("authenticated setup did not complete"));
                } else {
                    try { complete(AtfReportStatus.FAIL); }
                    finally { complete = true; }
                }
            }
        }

        private List<ReportExecutionEvidence> setupEvidence() {
            List<ReportExecutionEvidence> evidence = new ArrayList<>();
            evidence.addAll(apiNetwork.evidence(
                    atf.shop.config.api.ShopApiCheckpoint.REGISTRATION));
            evidence.addAll(apiNetwork.evidence(
                    atf.shop.config.api.ShopApiCheckpoint.LOGIN));
            return List.copyOf(evidence);
        }

        private void publishNetworkInspector() {
            if (networkInspectorPublished || !configuration.validationProfile().apiEvidenceRequired()) return;
            body.networkInspector(NETWORK_INSPECTOR_SUMMARY_TITLE, apiNetwork.inspectorEvidence(),
                    networkSteps);
            networkInspectorPublished = true;
        }

    }
}


