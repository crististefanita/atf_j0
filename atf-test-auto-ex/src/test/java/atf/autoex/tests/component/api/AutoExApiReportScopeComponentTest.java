package atf.autoex.tests.component.api;

import atf.autoex.api.AutoExApiRequestFactory;
import atf.autoex.tests.api.AutoExApiContractReportExtension;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransportProvenance;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.scope.TestRunScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static atf.testkit.assertions.AuditedAssertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Actual Jupiter/suite lifecycle; requests are deterministic component evidence, never remote mutations. */
@AutoExComponentTest
@ResourceLock("java.lang.System.properties")
class AutoExApiReportScopeComponentTest {
    private static final String READ_ONLY = "atf.autoex.scope-fixture.readonly";
    private static final String MUTATIVE = "atf.autoex.scope-fixture.mutative";
    private static final String FAIL = "atf.autoex.scope-fixture.fail";

    @Test void explicitSuiteScopeCombinesBothClassesAndPreservesAnExactFailure() throws Exception {
        for (var selection : List.of(List.of(true, false), List.of(false, true), List.of(true, true))) {
            var output = output();
            var result = launch(output, selection.get(0), selection.get(1), false, CombinedFixture.class);
            assertEquals(0, result.getTestsFailedCount());
            assertEquals(selection.stream().filter(Boolean::booleanValue).count(), result.getTestsSucceededCount());
            var files = reports(output);
            assertEquals(1, files.size(), "the explicit suite owns one artifact");
            String html = Files.readString(files.getFirst());
            assertEquals(selection.get(0), html.contains("ReadOnlyFixture"));
            assertEquals(selection.get(1), html.contains("MutativeFixture"));
            assertTrue(html.contains("Controlled lifecycle fixture"));
            assertFalse(html.contains(">LIVE HTTP<"));
        }
        var output = output();
        var failed = launch(output, true, true, true, CombinedFixture.class);
        assertEquals(1, failed.getTestsSucceededCount());
        assertEquals(1, failed.getTestsFailedCount());
        assertEquals("controlled API scope failure", failed.getFailures().getFirst().getException().getMessage());
        var files = reports(output);
        assertEquals(1, files.size());
        String html = Files.readString(files.getFirst());
        assertTrue(html.contains("ReadOnlyFixture") && html.contains("MutativeFixture"));
        assertTrue(html.contains("controlled API scope failure"));
        assertTrue(html.contains("Fixture read-only") && html.contains("Fixture mutation"));
    }

    @Test void individualClassesAndRepeatedLaunchesKeepIndependentArtifacts() throws Exception {
        var output = output();
        var result = launch(output, true, true, false, ReadOnlyFixture.class, MutativeFixture.class);
        assertEquals(2, result.getTestsSucceededCount());
        assertEquals(2, reports(output).size(), "class scope remains the default outside an explicit suite");
        output = output();
        for (int invocation = 0; invocation < 2; invocation++) {
            result = launch(output, true, false, false, CombinedFixture.class);
            assertEquals(1, result.getTestsSucceededCount());
        }
        assertEquals(2, reports(output).size(), "a new execution cannot reuse a closed suite session");
    }

    private static org.junit.platform.launcher.listeners.TestExecutionSummary launch(Path output,
            boolean readOnly, boolean mutative, boolean fail, Class<?>... classes) {
        var relative = atf.autoex.runtime.config.AutoExProjectRoot.discover().relativize(output);
        var properties = Map.of(AutoExApiContractReportExtension.REPORT_DIRECTORY_PROPERTY, relative.toString(),
                READ_ONLY, Boolean.toString(readOnly), MUTATIVE, Boolean.toString(mutative), FAIL, Boolean.toString(fail));
        var previous = new java.util.HashMap<String, String>();
        properties.forEach((key, value) -> previous.put(key, System.getProperty(key)));
        try {
            properties.forEach(System::setProperty);
            var listener = new SummaryGeneratingListener();
            var request = LauncherDiscoveryRequestBuilder.request();
            for (var type : classes) request.selectors(selectClass(type));
            LauncherFactory.create().execute(request.build(), listener);
            assertEquals(0, listener.getSummary().getContainersFailedCount(), listener.getSummary().getFailures()
                    .stream().map(failure -> failure.getException().toString()).toList().toString());
            return listener.getSummary();
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
    }
    private static Path output() throws Exception {
        return Files.createDirectories(Path.of(System.getProperty("atf.test.reportDirectory", "target/scope-reports"))
                .toAbsolutePath().resolve("api-scope").resolve(java.util.UUID.randomUUID().toString()));
    }
    private static List<Path> reports(Path directory) throws Exception {
        try (var files = Files.list(directory)) { return files.filter(file -> file.toString().endsWith(".html")).toList(); }
    }
    private static void publish(boolean mutation) {
        var requests = new AutoExApiRequestFactory();
        var request = mutation ? requests.deleteAccount("scope@example.test", "synthetic-password") : requests.productsList();
        var response = new ApiResponse(200, "{\"responseCode\":200}", Map.of("Content-Type", "application/json"),
                Duration.ofMillis(2), "https://fixture.test" + request.path());
        boolean fail = mutation && Boolean.getBoolean(FAIL);
        AutoExApiContractReportExtension.publish(mutation ? "Fixture mutation" : "Fixture read-only",
                new ApiExecutionResult(response, 1, Duration.ofMillis(2), List.of(), List.of(), request,
                        ApiTransportProvenance.ACCEPTANCE_MOCK), fail ? 201 : 200, null);
        if (fail) throw new AssertionError("controlled API scope failure");
    }

    private static AutoExApiContractReportExtension fixtureReport() {
        var environment = atf.reporting.configuration.ReportEnvironmentSnapshot.builder()
                .put(atf.reporting.configuration.ReportEnvironmentField.ENVIRONMENT, "COMPONENT")
                .put(atf.reporting.configuration.ReportEnvironmentField.TRANSPORT_MODE, "ACCEPTANCE MOCK")
                .put(atf.reporting.configuration.ReportEnvironmentField.REPORT_ARTIFACT, "Controlled lifecycle fixture")
                .build();
        return new AutoExApiContractReportExtension(environment, List.of("api", "auto-ex", "component"));
    }

    @Suite
    @AtfIntegrationTest
    @SelectClasses({ReadOnlyFixture.class, MutativeFixture.class})
    @ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
    public static class CombinedFixture { }

    @AtfIntegrationTest
    @EnabledIfSystemProperty(named = READ_ONLY, matches = "true")
    public static class ReadOnlyFixture {
        @RegisterExtension static final AutoExApiContractReportExtension REPORT = fixtureReport();
        @Test void observedReadOnly() { publish(false); }
    }

    @AtfIntegrationTest
    @EnabledIfSystemProperty(named = MUTATIVE, matches = "true")
    public static class MutativeFixture {
        @RegisterExtension static final AutoExApiContractReportExtension REPORT = fixtureReport();
        @Test void observedMutation() { publish(true); }
    }
}
