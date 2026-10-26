package atf.shop.tests.support;

import atf.api.core.reporting.console.AtfConsoleEvent;
import atf.api.core.reporting.console.AtfConsoleSink;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfReportStatus;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.ui.UiExtentReportSession;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.api.reporting.evidence.profile.ReportEvidenceProfile;
import atf.api.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.api.testkit.evidence.AtfUiEvidence;
import atf.api.testkit.evidence.AtfUiEvidenceFlow;
import atf.api.testkit.evidence.AtfUiScreenshotPolicy;
import atf.api.console.junit.AtfConsoleTestExtension;
import atf.api.console.junit.AtfReportPathOptIn;
import atf.api.testkit.AtfConsoleMessages;
import atf.api.testkit.reporting.AtfReportPath;
import atf.api.testkit.lifecycle.CloseOnceResource;
import atf.api.testkit.scope.ScopedTestValue;
import atf.api.testkit.scope.TestRunScope;
import atf.api.testkit.channel.AtfTestChannelResolver;
import atf.api.testkit.channel.TestChannel;
import atf.api.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.model.ShopCredentials;
import atf.shop.config.ShopValidationProfile;
import atf.shop.flow.ShopPurchaseFlow;
import atf.shop.support.validation.ShopPurchaseConfirmation;
import atf.shop.locators.ShopLocators;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.tests.support.lifecycle.ShopE2eContext;
import atf.shop.tests.support.lifecycle.ShopE2eActivationExtension;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.reporting.ShopScreenshotPolicy;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.nio.file.Path;

/** Per-method browser lifecycle and the small composition API used by Practice Shop E2E tests. */
public final class ShopE2eTestSupport {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ShopE2eTestSupport.class);
    private static final String HARNESS_KEY = "e2e-harness";
    private static final String SUITE_REPORT_KEY = "practice-shop-suite-report";
    private static final String ACCOUNT_KEY = "registered-account";

    private ShopE2eTestSupport() { }

    static ScopedTestValue<ShopCredentials> accounts(ExtensionContext context) {
        return TestRunScope.selected(context).value(
                context, ShopE2eTestSupport.class, ACCOUNT_KEY, ShopCredentials.class);
    }

    public enum HarnessProfile {
        CONFIGURED,
        UI_ONLY,
        UI_API
    }

    public enum ActionDetailsMode {
        CONFIGURED,
        INCLUDED_COLLAPSED,
        SUMMARY_ONLY
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
    @AtfReportPath
    @ExtendWith({AtfConsoleTestExtension.class, ShopE2eActivationExtension.class, E2eExtension.class})
    public @interface WithE2eHarness {
        HarnessProfile profile() default HarnessProfile.CONFIGURED;
        ActionDetailsMode actionDetails() default ActionDetailsMode.CONFIGURED;
    }

    /** JUnit opens and closes one harness per test method, including failed methods. */
    public static final class E2eExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
        @Override
        public void beforeEach(ExtensionContext context) {
            AtfTestChannelResolver.resolve(context.getRequiredTestClass())
                    .ifPresent(plan -> plan.requireSupported(Set.of(TestChannel.UI, TestChannel.UI_API)));
            ShopConfiguration configuration = ShopTestSupport.configurationFor(
                    context.getRequiredTestClass());
            WithE2eHarness harness = context.getRequiredTestClass().getAnnotation(WithE2eHarness.class);
            configuration = configurationForChannel(configuration, harness, context.getRequiredTestClass());
            ShopTestSupport.AuthenticatedE2e authenticated =
                    context.getRequiredTestClass().getAnnotation(ShopTestSupport.AuthenticatedE2e.class);
            ShopTestSupport.AccountSetupMode accountSetup = authenticated == null
                    ? ShopTestSupport.AccountSetupMode.REUSE_FIXTURE : authenticated.accountSetup();
            Path registrationProfile = authenticated != null
                    && accountSetup == ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN
                    && authenticated.registrationProfile() == ShopTestSupport.RegistrationProfileMode.LOCAL
                    ? ShopConfigurationLoader.resolveRegistrationProfileFile(
                            authenticated.registrationProfileFile()) : null;
            AtfUiEvidence uiEvidence = context.getRequiredTestClass().getAnnotation(AtfUiEvidence.class);
            ReportEvidenceProfile presentation = reportProfile(configuration, harness, uiEvidence);
            atf.api.reporting.console.AtfConsoleSink reportConsole;
            UiExtentReportSession sharedReport = null;
            TestRunScope scope = TestRunScope.selected(context);
            ScopedTestValue<ShopCredentials> accounts = accounts(context);
            if (scope == TestRunScope.SUITE) {
                SuiteReportResource suiteReport = context.getRoot().getStore(NAMESPACE)
                        .getOrComputeIfAbsent(SUITE_REPORT_KEY,
                                ignored -> new SuiteReportResource(accounts),
                                SuiteReportResource.class);
                suiteReport.includeReportPath(context.getRequiredTestClass());
                sharedReport = suiteReport.open(configuration, presentation);
                reportConsole = suiteReport.reportConsole();
            } else {
                reportConsole = reportingConsole(
                        AtfConsoleTestExtension.reportPathSink(context.getRequiredTestClass()));
            }
            ShopE2eContext browser = ShopE2eContext.open(
                    configuration, presentation, sharedReport, reportConsole);
            context.getStore(NAMESPACE).put(HARNESS_KEY, new CloseOnceResource<>(
                    new E2eHarness(configuration, browser, new ShopLocators(),
                            context.getRequiredTestClass().getSimpleName(), accountSetup, registrationProfile,
                            scope, accounts)));
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            CloseOnceResource<?> resource = context.getStore(NAMESPACE).remove(HARNESS_KEY, CloseOnceResource.class);
            if (resource != null) resource.closeAfter(context.getExecutionException().orElse(null));
        }

        @Override
        public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
            return parameter.getParameter().getType() == E2eHarness.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
            CloseOnceResource<?> resource = context.getStore(NAMESPACE).get(HARNESS_KEY, CloseOnceResource.class);
            if (resource == null) throw new ParameterResolutionException("Practice Shop E2E harness was not opened");
            return (E2eHarness) resource.value();
        }
    }

    private static atf.api.reporting.console.AtfConsoleSink reportingConsole(AtfConsoleSink console) {
        Objects.requireNonNull(console, "console");
        return event -> console.publish(AtfConsoleEvent.fromExternalEnums(
                event.type(), event.scope(), event.name(), event.status(),
                event.durationMillis(), event.detailName(), event.detailValue()));
    }

    /** Root-store owner: all suite scenarios share one reporter, flushed after the last child test. */
    static final class SuiteReportResource implements ExtensionContext.Store.CloseableResource {
        private final ScopedTestValue<ShopCredentials> accounts;
        private final AtfReportPathOptIn reportPathOptIn = new AtfReportPathOptIn(
                AtfConsoleTestExtension.reportPathPublisher());
        private UiExtentReportSession report;

        SuiteReportResource(ScopedTestValue<ShopCredentials> accounts) {
            this.accounts = Objects.requireNonNull(accounts, "accounts");
        }

        synchronized void includeReportPath(Class<?> owner) {
            reportPathOptIn.include(owner);
        }

        atf.api.reporting.console.AtfConsoleSink reportConsole() {
            return reportingConsole(reportPathOptIn);
        }

        synchronized UiExtentReportSession open(ShopConfiguration configuration,
                                                ReportEvidenceProfile profile) {
            if (report == null) report = ShopE2eContext.openReport(
                    configuration, profile, reportConsole());
            return report;
        }

        @Override
        public synchronized void close() {
            UiExtentReportSession owned = report;
            report = null;
            try {
                if (owned != null) owned.close();
            } finally {
                accounts.close();
            }
        }
    }

    static ShopConfiguration configurationForHarnessProfile(
            ShopConfiguration configuration, HarnessProfile profile) {
        return switch (Objects.requireNonNull(profile, "profile")) {
            case CONFIGURED -> Objects.requireNonNull(configuration, "configuration");
            case UI_ONLY -> configuration.withValidationProfile(ShopValidationProfile.UI_ONLY);
            case UI_API -> configuration.withValidationProfile(ShopValidationProfile.UI_API);
        };
    }

    /** Preflight is complete before a report, page, network listener or browser is opened. */
    static ShopConfiguration configurationForChannel(ShopConfiguration configuration,
                                                    WithE2eHarness harness, Class<?> owner) {
        var selected = AtfTestChannelResolver.resolve(owner);
        if (selected.isEmpty()) {
            return harness == null ? configuration
                    : configurationForHarnessProfile(configuration, harness.profile());
        }
        var plan = selected.orElseThrow();
        plan.requireSupported(Set.of(TestChannel.UI, TestChannel.UI_API));
        TestChannel channel = plan.channel();
        if (harness != null && harness.profile() != HarnessProfile.CONFIGURED
                && ((channel == TestChannel.UI && harness.profile() != HarnessProfile.UI_ONLY)
                    || (channel == TestChannel.UI_API && harness.profile() != HarnessProfile.UI_API))) {
            throw new IllegalStateException(owner.getName()
                    + " declares a channel that conflicts with WithE2eHarness.profile");
        }
        if (channel == TestChannel.UI) {
            return configuration.withValidationProfile(ShopValidationProfile.UI_ONLY);
        }
        if (configuration.api().evidenceBinding().checkpoints().isEmpty()
                || !configuration.browser().networkInspection().enabled()
                || configuration.browser().networkInspection().allowedHosts().isEmpty()) {
            throw new IllegalStateException(owner.getName()
                    + " requires selected API checkpoints and an enabled, allowlisted network inspector");
        }
        return configuration.withValidationProfile(ShopValidationProfile.UI_API);
    }

    static ReportEvidenceProfile reportProfile(ShopConfiguration configuration,
                                               WithE2eHarness harness) {
        return reportProfile(configuration, harness, null);
    }

    static ReportEvidenceProfile reportProfile(ShopConfiguration configuration,
                                               WithE2eHarness harness,
                                               AtfUiEvidence evidence) {
        ReportEvidenceProfile configured = ShopScreenshotPolicy.profile(
                Objects.requireNonNull(configuration, "configuration")
                        .reporting().screenshotPolicy());
        AtfUiEvidenceFlow flow = evidence == null
                ? AtfUiEvidenceFlow.CONFIGURED : evidence.flow();
        AtfUiScreenshotPolicy screenshots = evidence == null
                ? AtfUiScreenshotPolicy.CONFIGURED : evidence.screenshots();
        ReportEvidenceProfile.Builder selected = configured.toBuilder()
                .flowMode(resolve(flow, configured.flowMode()))
                .screenshotPolicy(resolve(screenshots, configured.screenshotPolicy()));
        if (harness != null) {
            selected.actionDetailsMode(resolve(harness.actionDetails(), configured.actionDetailsMode()));
        }
        return selected.build();
    }

    private static ReportUiActionDetailsMode resolve(ActionDetailsMode selected,
                                                      ReportUiActionDetailsMode configured) {
        return switch (Objects.requireNonNull(selected, "actionDetails")) {
            case CONFIGURED -> Objects.requireNonNull(configured, "configured action details mode");
            case INCLUDED_COLLAPSED -> ReportUiActionDetailsMode.INCLUDED_COLLAPSED;
            case SUMMARY_ONLY -> ReportUiActionDetailsMode.SUMMARY_ONLY;
        };
    }

    private static ReportEvidenceFlowMode resolve(AtfUiEvidenceFlow selected,
                                                  ReportEvidenceFlowMode configured) {
        return switch (Objects.requireNonNull(selected, "reportFlow")) {
            case CONFIGURED -> Objects.requireNonNull(configured, "configured flow");
            case SCREEN_CHECKPOINTS -> ReportEvidenceFlowMode.SCREEN_CHECKPOINTS;
            case UNIFIED_FLOW -> ReportEvidenceFlowMode.UNIFIED_FLOW;
        };
    }

    private static ReportScreenshotPolicy resolve(AtfUiScreenshotPolicy selected,
                                                  ReportScreenshotPolicy configured) {
        return switch (Objects.requireNonNull(selected, "screenshots")) {
            case CONFIGURED -> Objects.requireNonNull(configured, "configured screenshot policy");
            case EACH_CHECKPOINT -> ReportScreenshotPolicy.EACH_CHECKPOINT;
            case FINAL_ONLY -> ReportScreenshotPolicy.FINAL_ONLY;
            case FAILURE_ONLY -> ReportScreenshotPolicy.FAILURE_ONLY;
            case FINAL_AND_FAILURE -> ReportScreenshotPolicy.FINAL_AND_FAILURE;
            case NONE -> ReportScreenshotPolicy.NONE;
        };
    }

    public record E2eHarness(ShopConfiguration configuration,
                             ShopE2eContext context,
                             ShopLocators locators,
                             String reportClass,
                             ShopTestSupport.AccountSetupMode accountSetupMode,
                             Path registrationProfileFile,
                             TestRunScope runScope,
                             ScopedTestValue<ShopCredentials> accounts) implements AutoCloseable {
        public E2eHarness {
            Objects.requireNonNull(configuration, "configuration");
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(locators, "locators");
            if (reportClass == null || reportClass.isBlank()) {
                throw new IllegalArgumentException("reportClass is required");
            }
            Objects.requireNonNull(accountSetupMode, "accountSetupMode");
            Objects.requireNonNull(runScope, "runScope");
            Objects.requireNonNull(accounts, "accounts");
        }

        public void run(String title, String overview, Consumer<E2eJourney> businessFlow) {
            Objects.requireNonNull(businessFlow, "businessFlow");
            PlaywrightActionSequence actions = context.actions();
            ShopE2eContext.Scenario scenario = context.scenario(reportClass, title, overview);
            E2eJourney journey = new E2eJourney(this, actions, scenario);
            scenario.execute(context.page(), actions, () -> businessFlow.accept(journey));
        }

        /** Runs account acquisition in SETUP and opens TEST BODY only after it has passed. */
        public void run(String title, String overview,
                        Function<E2eSetup, ShopCredentials> accountSetup,
                        BiConsumer<E2eJourney, ShopCredentials> businessFlow) {
            Objects.requireNonNull(accountSetup, "accountSetup");
            Objects.requireNonNull(businessFlow, "businessFlow");
            PlaywrightActionSequence actions = context.actions();
            ShopE2eContext.Scenario scenario = context.scenarioWithExplicitSetup(reportClass, title, overview);
            scenario.execute(context.page(), actions, () -> {
                E2eSetup setup = new E2eSetup(this, actions, scenario);
                ShopCredentials credentials = Objects.requireNonNull(
                        accountSetup.apply(setup), "accountSetup credentials");
                if (!setup.completed()) {
                    throw new IllegalStateException("account setup must publish a passing SETUP step before TEST BODY");
                }
                businessFlow.accept(new E2eJourney(this, actions, scenario), credentials);
            });
        }

        @Override
        public void close() {
            context.close();
        }
    }

    /** Setup-only view; raw values stay excluded and each action table follows the YAML capture policy. */
    public static final class E2eSetup {
        private final E2eHarness harness;
        private final PlaywrightActionSequence actions;
        private final ShopE2eContext.Scenario scenario;
        private boolean completed;
        private boolean failed;

        private E2eSetup(E2eHarness harness, PlaywrightActionSequence actions,
                         ShopE2eContext.Scenario scenario) {
            this.harness = harness;
            this.actions = actions;
            this.scenario = scenario;
        }

        public ShopConfiguration configuration() { return harness.configuration(); }
        public ShopLocators locators() { return harness.locators(); }
        public Page page() { return harness.context().page(); }
        public PlaywrightActionSequence actions() { return actions; }
        public ShopApiNetworkJournal apiNetwork() { return harness.context().apiNetwork(); }
        public ShopTestSupport.AccountSetupMode accountSetupMode() { return harness.accountSetupMode(); }
        public Path registrationProfileFile() { return harness.registrationProfileFile(); }
        public TestRunScope runScope() { return harness.runScope(); }
        public ScopedTestValue<ShopCredentials> accounts() { return harness.accounts(); }
        public void checkpoint(ShopPurchaseTraceStep step, Collection<AtfAssertion> assertions,
                               Collection<atf.api.reporting.model.ReportExecutionEvidence> apiEvidence) {
            if (completed || failed) throw new IllegalStateException("SETUP is already finished");
            AtfReportStatus status = assertions.stream().allMatch(AtfAssertion::passed)
                    ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
            scenario.setupCheckpoint(page(), actions, step.keyword(), step.title(), assertions, apiEvidence, status);
            if (status == AtfReportStatus.FAIL) {
                failed = true;
                scenario.finishSetup(AtfReportStatus.FAIL);
                throw new AssertionError("Authenticated account setup failed; TEST BODY was not started");
            }
        }

        public void complete() {
            if (completed || failed) throw new IllegalStateException("SETUP is already finished");
            scenario.finishSetup(AtfReportStatus.PASS);
            completed = true;
        }

        public void fail(ShopPurchaseTraceStep step, Collection<AtfAssertion> assertions,
                         Collection<atf.api.reporting.model.ReportExecutionEvidence> apiEvidence,
                         Throwable failure) {
            if (completed || failed) throw new IllegalStateException("SETUP is already finished");
            scenario.setupCheckpoint(page(), actions, step.keyword(), step.title(),
                    assertions, apiEvidence, AtfReportStatus.FAIL);
            scenario.fail(page(), actions, failure);
            failed = true;
        }

        private boolean completed() { return completed; }
    }

    /** Test-facing handles; observed assertions and business flow stay in the E2E test. */
    public record E2eJourney(E2eHarness harness, PlaywrightActionSequence actions,
                             ShopE2eContext.Scenario scenario) {
        public ShopConfiguration configuration() { return harness.configuration(); }
        public ShopE2eContext context() { return harness.context(); }
        public ShopLocators locators() { return harness.locators(); }
        public Page page() { return context().page(); }
        public ShopApiNetworkJournal apiNetwork() { return context().apiNetwork(); }

        public void checkpoint(ShopPurchaseCheckpoint checkpoint) {
            scenario.checkpoint(page(), actions, checkpoint);
        }

        public void pass(ShopPurchaseTraceStep step, Collection<AtfAssertion> assertions) {
            scenario.pass(page(), actions, step.title(), assertions);
        }

        public void completePurchase() {
            scenario.complete(AtfReportStatus.PASS);
        }

        /** Publishes one terminal screen validation with the final browser capture. */
        public void completePurchase(ShopPurchaseFlow.PurchaseResult result) {
            pass(ShopPurchaseTraceStep.ORDER_CONFIRMATION, ShopPurchaseConfirmation.assertions(result));
        }


    }
}
