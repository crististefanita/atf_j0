package atf.ui.tests.extent.support.api;
import atf.console.AtfConsoleReporter;
import atf.core.config.ApiTransportMode;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.ApiReportEnvironmentSnapshotFactory;
import atf.core.config.source.ApiBootstrapConfigurationLoader;
import atf.core.testing.api.ApiScenarioSelection;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.reporting.api.tests.support.setup.ApiScenarioSetupCatalog;
import atf.ui.tests.extent.support.data.UiTestData;

import atf.reporting.api.execution.ApiModule;
import atf.reporting.api.execution.ApiModuleFactory;
import atf.core.lifecycle.ApiScenarioLifecycleRunner;
import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockTransport;

import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.support.CoreReportPortsAdapter;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.config.UiAuditSuiteConfiguration;
import atf.ui.tests.extent.component.plan.ApiScenarioManifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Composition root for a read-only API artifact consumed by a UI test.
 *
 * <p>The setup source is deliberately small: the test declares either API
 * families or exact {@link ApiScenarioSetup#tests()} references and the
 * catalog publishes them through the project mock. No browser selector or UI
 * assertion is allowed to leak into this layer.</p>
 */
public class ApiReportAuditSetup {
    public Result generate(Class<?> setupOwner, UiConfiguration configuration) {
        Objects.requireNonNull(setupOwner, "setupOwner");
        Objects.requireNonNull(configuration, "configuration");
        return generate(setupOwner, configuration, selection(setupOwner, configuration));
    }

    /** Explicit family input is authoritative and does not re-read class annotations. */
    public Result generate(Class<?> setupOwner, UiConfiguration configuration,
                           Collection<String> selectedFamilies) {
        Objects.requireNonNull(setupOwner, "setupOwner");
        Objects.requireNonNull(configuration, "configuration");
        return generate(setupOwner, configuration,
                new Selection(parseFamilies(String.join(",", selectedFamilies),
                        "UiReportAuditSpec.apiFamilies"), List.of()));
    }

    /** Generates exactly one previously resolved family or exact-test selection. */
    public Result generate(Class<?> setupOwner, UiConfiguration configuration, Selection selection) {
        Objects.requireNonNull(setupOwner, "setupOwner");
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(selection, "selection");
        if (!selection.selectedTests().isEmpty()) {
            return generateSelected(setupOwner, selection.families(), selection.selectedTests());
        }
        List<String> families = selection.families();
        AtfMockTransport transport = ReportingTestResources.mockTransport();
        ApiModule api = acceptanceApi(transport);
        Path report;
        var reportingConfiguration = AtfReportConfigurationLoader.load();
        try (ExtentReportSession session = ExtentReportSession.openConfigured(
                reportingConfiguration,
                ApiReportEnvironmentSnapshotFactory.precondition(setupOwner, families))) {
            report = session.reportPath();
            ApiScenarioSetupCatalog.publish(CoreReportPortsAdapter.wrap(session), api,
                    new ApiScenarioLifecycleRunner(), families);
        }
        if (!Files.isRegularFile(report)) {
            throw new IllegalStateException("API setup did not publish an Extent report: " + report);
        }
        return new Result(report, transport.configuration(), families,
                ApiScenarioManifest.from(families, transport.configuration()), List.of());
    }

    /** Resolves one reusable API selection through the documented precedence. */
    public static List<String> families(Class<?> setupOwner, UiConfiguration configuration) {
        return selection(setupOwner, configuration).families();
    }

    /** Resolves one collection with explicit JVM overrides before exact tests and YAML. */
    public static Selection selection(Class<?> setupOwner, UiConfiguration configuration) {
        return selection(setupOwner, configuration,
                System.getProperty(UiTestData.API_FAMILIES_PROPERTY),
                System.getProperty(UiTestData.AUDIT_SUITE_PROPERTY));
    }

    /** Resolves a selection with explicit overrides; {@code null} means no override. */
    public static Selection selection(Class<?> setupOwner, UiConfiguration configuration,
                                      String familyOverride, String suiteOverride) {
        Objects.requireNonNull(setupOwner, "setupOwner");
        Objects.requireNonNull(configuration, "configuration");
        if (familyOverride != null) {
            return new Selection(parseFamilies(familyOverride, UiTestData.API_FAMILIES_PROPERTY), List.of());
        }
        if (suiteOverride != null && !suiteOverride.isBlank()) {
            return new Selection(parseFamilies(suiteFamilies(suiteOverride, configuration),
                    UiTestData.AUDIT_SUITE_PROPERTY), List.of());
        }
        List<ApiScenarioSelection> selectedTests = declaredTests(setupOwner);
        if (!selectedTests.isEmpty()) {
            return new Selection(ApiScenarioSetupCatalog.familiesFor(selectedTests), selectedTests);
        }
        ApiScenarioSetup declaration = setupOwner.getAnnotation(ApiScenarioSetup.class);
        if (declaration == null || declaration.families().length == 0) {
            return new Selection(configuration.apiScenarioFamilies(), List.of());
        }
        return new Selection(parseFamilies(String.join(",", declaration.families()),
                "@ApiScenarioSetup.families"), List.of());
    }

    /** Returns exact annotation tests only when no higher-precedence JVM override exists. */
    public static List<ApiScenarioSelection> selectedTests(Class<?> setupOwner) {
        Objects.requireNonNull(setupOwner, "setupOwner");
        if (System.getProperty(UiTestData.API_FAMILIES_PROPERTY) != null) return List.of();
        String suite = System.getProperty(UiTestData.AUDIT_SUITE_PROPERTY);
        if (suite != null && !suite.isBlank()) return List.of();
        return declaredTests(setupOwner);
    }

    private static List<ApiScenarioSelection> declaredTests(Class<?> setupOwner) {
        ApiScenarioSetup declaration = setupOwner.getAnnotation(ApiScenarioSetup.class);
        if (declaration == null || declaration.tests().length == 0) return List.of();
        return ApiScenarioSetupCatalog.resolve(declaration.tests());
    }

    private Result generateSelected(Class<?> setupOwner, List<String> families,
                                    List<ApiScenarioSelection> selectedTests) {
        AtfMockTransport transport = ReportingTestResources.mockTransport();
        ApiModule api = acceptanceApi(transport);
        Path report;
        var reportingConfiguration = AtfReportConfigurationLoader.load();
        try (ExtentReportSession session = ExtentReportSession.openConfigured(
                reportingConfiguration,
                ApiReportEnvironmentSnapshotFactory.precondition(setupOwner, families))) {
            report = session.reportPath();
            ApiScenarioSetupCatalog.publishSelected(CoreReportPortsAdapter.wrap(session), api,
                    new ApiScenarioLifecycleRunner(), selectedTests);
        }
        if (!Files.isRegularFile(report)) {
            throw new IllegalStateException("Selected API scenarios did not publish an Extent report: "
                    + report);
        }
        return new Result(report, transport.configuration(), families,
                ApiScenarioManifest.from(families, transport.configuration(), selectedTests),
                selectedTests);
    }

    private static String suiteFamilies(String suite, UiConfiguration configuration) {
        UiAuditSuiteConfiguration configured = configuration.auditSuite(suite).orElse(null);
        if (configured != null) return String.join(",", configured.scenarioFamilies());
        return switch (suite.trim().toLowerCase(Locale.ROOT)) {
            case "users-posts", "users_posts" -> "users,posts";
            case "requests-headers", "requests_headers" -> "requests,headers";
            case "all", "all-configured", "all_configured" ->
                    String.join(",", configuration.apiScenarioFamilies());
            default -> suite;
        };
    }

    private static ApiModule acceptanceApi(AtfMockTransport transport) {
        var bootstrap = ApiBootstrapConfigurationLoader.load();
        if (bootstrap.transportPolicy().mode() != ApiTransportMode.ACCEPTANCE) {
            throw new IllegalStateException("UI API setup requires the acceptance transport; live is never implicit");
        }
        return ApiModuleFactory.bootstrap(bootstrap, transport, null, AtfConsoleReporter.configuredStdout());
    }

    private static List<String> parseFamilies(String raw, String source) {
        List<String> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException(source + " must select at least one API family");
        }
        return parsed;
    }

    public record Result(Path report, AtfMockConfiguration mock, List<String> families,
                         ApiScenarioManifest manifest,
                         List<ApiScenarioSelection> selectedTests) {
        public Result(Path report, AtfMockConfiguration mock, List<String> families) {
            this(report, mock, families, ApiScenarioManifest.from(families, mock), List.of());
        }

        public Result(Path report, AtfMockConfiguration mock, List<String> families,
                      ApiScenarioManifest manifest) {
            this(report, mock, families, manifest, List.of());
        }

        public Result {
            report = Objects.requireNonNull(report, "report").toAbsolutePath().normalize();
            mock = Objects.requireNonNull(mock, "mock");
            families = List.copyOf(Objects.requireNonNull(families, "families"));
            manifest = Objects.requireNonNull(manifest, "manifest");
            selectedTests = List.copyOf(Objects.requireNonNull(selectedTests, "selectedTests"));
        }
    }

    public record Selection(List<String> families, List<ApiScenarioSelection> selectedTests) {
        public Selection {
            families = List.copyOf(Objects.requireNonNull(families, "families"));
            selectedTests = List.copyOf(Objects.requireNonNull(selectedTests, "selectedTests"));
            if (families.isEmpty()) {
                throw new IllegalArgumentException("A resolved API selection must contain a family");
            }
        }
    }
}
