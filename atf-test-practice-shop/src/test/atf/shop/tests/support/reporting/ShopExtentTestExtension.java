package atf.shop.tests.support.reporting;

import atf.testkit.assertions.RecordedAssertions;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import atf.shop.config.ShopConfigurationLoader;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Per-test generic Extent lifecycle; it has no browser or screenshot policy. */
public final class ShopExtentTestExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NS =
            ExtensionContext.Namespace.create(ShopExtentTestExtension.class);
    private static final String STATE = "extent-state";
    private record State(ExtentReportSession session, AtfReportPorts.Scenario scenario,
                         AtfReportPorts.Phase phase, RecordedAssertions assertions) { }

    @Override public void beforeEach(ExtensionContext context) throws Exception {
        Path output = ShopConfigurationLoader.load().apiReportDirectory()
                .resolve("component").toAbsolutePath().normalize();
        Files.createDirectories(output);
        String name = context.getRequiredTestClass().getSimpleName() + "_"
                + context.getRequiredTestMethod().getName() + "_"
                + Instant.now().toString().replace(":", "-") + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".html";
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "COMPONENT")
                .put(ReportEnvironmentField.SUITE, "Practice Shop component")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Per-test result")
                .build();
        var session = new ExtentReportSession(output.resolve(name),
                AtfReportConfigurationLoader.load().reportingSettings(), new SensitiveDataRedactor(),
                atf.reporting.console.AtfConsoleSink.noOp(), environment);
        var type = context.getRequiredTestClass();
        var scenario = session.startScenario(type.getSimpleName(), type.getPackageName(),
                context.getDisplayName(), List.of("component", "practice-shop"));
        var phase = scenario.startPhase("TEST BODY");
        phase.detail("Test method", context.getRequiredTestMethod().getName());
        context.getStore(NS).put(STATE, new State(session, scenario, phase,
                new RecordedAssertions()));
    }

    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == RecordedAssertions.class;
    }

    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        State state = context.getStore(NS).get(STATE, State.class);
        if (state == null) throw new IllegalStateException("Component report is not open");
        return state.assertions();
    }

    @Override public void afterEach(ExtensionContext context) {
        State state = context.getStore(NS).remove(STATE, State.class);
        if (state == null) return;
        AtfReportStatus status = context.getExecutionException().isEmpty()
                ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        try {
            var assertions = state.assertions().checks().stream()
                    .map(check -> new AtfAssertion(check.name(), check.expected(), check.actual(), check.passed()))
                    .toList();
            if (!assertions.isEmpty()) {
                var assertionStep = state.phase().startAssertionStep("Component assertions");
                assertionStep.assertions(assertions);
                var assertionStatus = assertions.stream().allMatch(AtfAssertion::passed)
                        ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                assertionStep.complete(assertionStatus);
                assertionStep.close();
            }
            if (status == AtfReportStatus.FAIL) {
                state.phase().diagnostic(atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE,
                        context.getExecutionException().orElseThrow().toString());
            }
            state.phase().complete(status);
            state.scenario().complete(status);
        } finally {
            state.phase().close();
            state.scenario().close();
            state.session().close();
        }
    }
}
