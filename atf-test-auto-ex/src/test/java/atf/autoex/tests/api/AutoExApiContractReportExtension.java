package atf.autoex.tests.api;

import atf.console.junit.AtfConsoleTestExtension;
import atf.console.junit.AtfReportPathOptIn;
import atf.core.http.response.ApiExecutionResult;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.lifecycle.FailureAwareCleanup;
import atf.testkit.output.ReportOutputDirectory;
import atf.testkit.scope.TestRunScope;
import atf.autoex.api.AutoExApiJsonField;
import atf.autoex.runtime.config.AutoExProjectRoot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static atf.reporting.console.AtfConsoleEvent.Type.REPORT_PUBLISHED;
import static atf.reporting.ui.contract.ReportUiContract.Phase.TEST_BODY;

/** Publishes direct Automation Exercise API contract runs as one auditable Extent artifact. */
public final class AutoExApiContractReportExtension implements BeforeAllCallback, BeforeEachCallback,
        AfterEachCallback {
    public static final String REPORT_DIRECTORY_PROPERTY = "atf.autoex.api.report-directory";
    private static final String DEFAULT_REPORT_DIRECTORY = "artifacts/reports/api/auto-ex";
    private static final String REPORT_FILE_PREFIX = "api-atf-auto-ex-contract_";
    private static final int MAX_DIAGNOSTIC_FAILURES = 4;
    private static final int MAX_DIAGNOSTIC_LENGTH = 1_000;
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(AutoExApiContractReportExtension.class);
    private static final String SESSION = "session";
    private static final String RUN = "run";
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private static final ObjectMapper JSON = new ObjectMapper();
    private final java.util.Optional<ReportEnvironmentSnapshot> environmentOverride;
    private final List<String> tags;

    public AutoExApiContractReportExtension() {
        environmentOverride = java.util.Optional.empty();
        tags = List.of("api", "auto-ex", "contract", "live");
    }

    /** Explicit metadata for controlled lifecycle fixtures; no external transport is implied. */
    public AutoExApiContractReportExtension(ReportEnvironmentSnapshot environment, List<String> tags) {
        environmentOverride = java.util.Optional.of(environment);
        this.tags = List.copyOf(tags);
    }

    private record Run(AtfReportPorts.Scenario scenario, AtfReportPorts.Phase phase) { }
    private record OwnedReport(ExtentReportSession session, AtfReportPathOptIn paths)
            implements ExtensionContext.Store.CloseableResource, AutoCloseable {
        @Override public void close() { session.close(); }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var scope = TestRunScope.selected(context);
        var report = scope.value(context, getClass(), SESSION, OwnedReport.class).getOrCreate(() -> {
            var opened = openReport(context);
            try {
                (scope == TestRunScope.SUITE ? context.getRoot() : context).getStore(NAMESPACE).put(SESSION, opened);
                return opened;
            } catch (RuntimeException | Error failure) {
                FailureAwareCleanup.closeAfterFailure(failure, opened);
                throw failure;
            }
        }).value();
        report.paths().include(context.getRequiredTestClass());
    }

    private OwnedReport openReport(ExtensionContext context) {
        Path root = AutoExProjectRoot.discover();
        Path directory = resolveDirectory(root,
                System.getProperty(REPORT_DIRECTORY_PROPERTY, DEFAULT_REPORT_DIRECTORY));
        try { Files.createDirectories(directory); }
        catch (IOException failure) { throw new UncheckedIOException("Cannot prepare API report directory", failure); }
        String timestamp = Instant.now().toString().replace(":", "-");
        Path report = directory.resolve(REPORT_FILE_PREFIX + timestamp + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".html");
        var environment = environmentOverride.orElseGet(() -> ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "LIVE OPT-IN")
                .put(ReportEnvironmentField.TEST_PACKAGE, context.getRequiredTestClass().getPackageName())
                .put(ReportEnvironmentField.SUITE, "Automation Exercise API contract")
                .put(ReportEnvironmentField.PROFILE, "auto-ex-api")
                .put(ReportEnvironmentField.TRANSPORT_MODE, "LIVE HTTP")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Direct API contract evidence")
                .build());
        // Keep the API artifact's consumer-owned path, but use the same ATF
        // reporting configuration as the UI session (assertion/body limits).
        var paths = new AtfReportPathOptIn(AtfConsoleTestExtension.reportPathPublisher());
        var session = new ExtentReportSession(report,
                AtfReportConfigurationLoader.load().reportingSettings(),
                new SensitiveDataRedactor(),
                reportPathSink(paths), environment);
        return new OwnedReport(session, paths);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var session = TestRunScope.selected(context).value(context, getClass(), SESSION, OwnedReport.class)
                .current().orElseThrow(() -> new IllegalStateException("API contract report session was not opened")).session();
        Class<?> owner = context.getRequiredTestClass();
        var scenario = session.startScenario(owner.getSimpleName(), owner.getPackageName(),
                context.getDisplayName(), tags);
        AtfReportPorts.Phase phase = null;
        try {
            phase = scenario.startPhase(TEST_BODY.displayName());
            var run = new Run(scenario, phase);
            context.getStore(NAMESPACE).put(RUN, run);
            CURRENT.set(run);
        } catch (RuntimeException | Error setupFailure) {
            AtfReportPorts.Phase acquiredPhase = phase;
            FailureAwareCleanup.runAll(setupFailure,
                    () -> { if (acquiredPhase != null) acquiredPhase.close(); },
                    () -> scenario.complete(AtfReportStatus.FAIL), scenario::close);
            throw setupFailure;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Run run = context.getStore(NAMESPACE).remove(RUN, Run.class);
        CURRENT.remove();
        if (run == null) return;
        Throwable failure = context.getExecutionException().orElse(null);
        finishRun(run.scenario(), run.phase(), failure);
    }

    /** App report adapter delegates cleanup ordering and failure aggregation to the existing Test Kit. */
    public static void finishRun(AtfReportPorts.Scenario scenario, AtfReportPorts.Phase phase,
                                 Throwable primary) throws Exception {
        AtfReportStatus status = primary == null ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        Throwable failure = FailureAwareCleanup.runAll(primary,
                () -> { if (primary != null) recordExecutionFailure(phase, primary); },
                () -> phase.complete(status), phase::close);
        AtfReportStatus finalStatus = failure == null ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        failure = FailureAwareCleanup.runAll(failure,
                () -> scenario.complete(finalStatus), scenario::close);
        if (primary != null || failure == null) return;
        if (failure instanceof Exception exception) throw exception;
        throw (Error) failure;
    }

    public static void publish(String title, ApiExecutionResult execution, int expectedResponseCode,
                               String expectedMessage) {
        Run run = CURRENT.get();
        if (run == null) throw new IllegalStateException("API exchange was recorded outside its JUnit report scope");
        publish(run.phase(), title, execution, expectedResponseCode, expectedMessage);
    }

    public static void publish(String title, ApiExecutionResult execution, int expectedResponseCode,
                               String expectedMessage, Collection<AtfAssertion> additionalAssertions) {
        Run run = CURRENT.get();
        if (run == null) throw new IllegalStateException("API exchange was recorded outside its JUnit report scope");
        publish(run.phase(), title, execution, expectedResponseCode, expectedMessage, additionalAssertions);
    }

    public static void publish(AtfReportPorts.Phase phase, String title, ApiExecutionResult execution,
                               int expectedResponseCode, String expectedMessage) {
        publish(phase, title, execution, expectedResponseCode, expectedMessage, List.of());
    }

    public static void publish(AtfReportPorts.Phase phase, String title, ApiExecutionResult execution,
                               int expectedResponseCode, String expectedMessage,
                               Collection<AtfAssertion> additionalAssertions) {
        AtfReportPorts.Request node = phase.startRequest(title,
                execution.request().method(), execution.request().path());
        ApiExchangeEvidence.publish(node, ReportModelAdapters.execution(execution));

        String actualCode = jsonField(execution.response().body(), AutoExApiJsonField.RESPONSE_CODE.path());
        String actualMessage = jsonField(execution.response().body(), AutoExApiJsonField.MESSAGE.path());
        boolean codeMatches = Integer.toString(expectedResponseCode).equals(actualCode);
        boolean messageMatches = expectedMessage == null || expectedMessage.equals(actualMessage);
        var validation = node.startValidation("Validate documented JSON response contract");
        var assertions = new java.util.ArrayList<AtfAssertion>();
        assertions.add(new AtfAssertion("JSON responseCode", Integer.toString(expectedResponseCode),
                actualCode, codeMatches));
        if (expectedMessage != null) {
            assertions.add(new AtfAssertion("JSON message", expectedMessage, actualMessage, messageMatches));
        }
        assertions.addAll(additionalAssertions);
        AtfReportStatus status = assertions.stream().allMatch(AtfAssertion::passed)
                ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        validation.assertions(assertions);
        validation.complete(status);
        node.complete(status);
    }

    public static void recordExecutionFailure(AtfReportPorts.Phase phase, Throwable failure) {
        if (phase == null || failure == null) return;
        String diagnostic = describeFailure(failure);
        diagnostic = atf.autoex.runtime.api.AutoExApiFailureEvidence.publish(phase, failure, diagnostic).orElse(diagnostic);
        phase.diagnostic(ReportPhaseDiagnostic.Kind.JUNIT_FAILURE, diagnostic);
    }

    private static String describeFailure(Throwable failure) {
        var description = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var pending = new ArrayDeque<Throwable>();
        pending.add(failure);
        int relatedFailures = 0;
        while (!pending.isEmpty() && relatedFailures < MAX_DIAGNOSTIC_FAILURES) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) continue;
            relatedFailures++;
            if (!description.isEmpty()) description.append(" <- ");
            description.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (message != null && !message.isBlank()) description.append(": ").append(message.trim());
            if (current.getCause() != null) pending.addLast(current.getCause());
            for (Throwable suppressed : current.getSuppressed()) pending.addLast(suppressed);
        }
        String safe = new SensitiveDataRedactor().redactText(description.toString());
        return safe.length() <= MAX_DIAGNOSTIC_LENGTH ? safe : safe.substring(0, MAX_DIAGNOSTIC_LENGTH);
    }

    public static Path resolveDirectory(Path projectRoot, String configured) {
        return ReportOutputDirectory.resolveProjectRelative(projectRoot, configured, DEFAULT_REPORT_DIRECTORY);
    }

    private static String jsonField(String json, String field) {
        try {
            var node = JSON.readTree(json);
            if (node == null || !node.has(field)) return "<missing>";
            return node.path(field).asText();
        } catch (java.io.IOException invalidJson) {
            return "<invalid JSON>";
        }
    }

    private static AtfConsoleSink reportPathSink(AtfReportPathOptIn coreSink) {
        return event -> {
            if (event.type() == REPORT_PUBLISHED) {
                coreSink.publish(AtfConsoleEvent.reportPublished(Path.of(event.detailValue())));
            }
        };
    }
}
