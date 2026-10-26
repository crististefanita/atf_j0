package atf.shop.tests.support;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfGherkinKeyword;
import atf.api.reporting.contract.AtfReportStatus;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceProfile;
import atf.api.testkit.AtfComponentTest;
import atf.api.testkit.scope.ScopedTestValue;
import atf.api.testkit.scope.TestRunScope;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.model.ShopCredentials;
import atf.shop.tests.suites.ShopE2eSuite;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.suite.api.ConfigurationParameter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Exercises the actual Shop scope/condition wiring without a browser or external account mutation. */
@AtfComponentTest
class ShopAccountScopeComponentTest {
    private static final ThreadLocal<Observations> OBSERVED = new ThreadLocal<>();

    @Test
    void suiteParametersOrderTheProducerBeforeTheConsumerAndReleaseTheirValue() {
        var observed = execute(true, false, Consumer.class, Producer.class);
        assertEquals(0, observed.failed);
        assertEquals(0, observed.skipped);
        assertEquals(1, observed.created);
        assertEquals(List.of(Producer.class, Consumer.class), observed.classes);
        assertSame(observed.values.getFirst(), observed.values.getLast());
        assertTrue(observed.values.stream().allMatch(ScopedTestValue::isClosed));
        assertTrue(observed.values.stream().allMatch(value -> value.current().isEmpty()));
    }

    @Test
    void repeatedSuiteRunsDoNotShareCredentialsOrStaticEngineIdentifiers() {
        var first = execute(true, false, Consumer.class, Producer.class);
        var second = execute(true, false, Consumer.class, Producer.class);
        assertEquals(0, first.failed + second.failed);
        assertEquals(1, first.created);
        assertEquals(1, second.created);
        assertNotSame(first.values.getFirst(), second.values.getFirst());
        assertEquals(first.rootIds.getFirst(), second.rootIds.getFirst());
    }

    @Test
    void standaloneReuseWithoutAFixtureNeverConsumesAPreviousSuiteAccount() {
        var suite = execute(true, false, Producer.class, Consumer.class);
        assertEquals(0, suite.failed);
        var standalone = execute(false, false, Consumer.class);
        assertEquals(0, standalone.failed);
        assertEquals(1, standalone.skipped);
        assertEquals(0, standalone.created);
        assertTrue(standalone.values.isEmpty());
    }

    @Test
    void uncertainCreationIsNotReplayedAndDependentReuseDoesNotFallBack() {
        var observed = execute(true, true, Consumer.class, Producer.class);
        assertEquals(1, observed.failed);
        assertEquals(1, observed.skipped);
        assertEquals(1, observed.created);
        assertEquals(List.of(Producer.class), observed.classes);
        assertTrue(observed.skipReasons.stream().anyMatch(reason -> reason.contains(
                "automatic registration retry is forbidden")));
        assertTrue(observed.values.getFirst().isClosed());
        assertTrue(observed.values.getFirst().current().isEmpty());
    }

    @Test
    void reportPublicationFailureStillReleasesCredentialsAndIsNotRetried(@TempDir Path temporary)
            throws Exception {
        var accounts = new ScopedTestValue<ShopCredentials>();
        accounts.getOrCreate(ShopAccountScopeComponentTest::fixture);
        var resource = new ShopE2eTestSupport.SuiteReportResource(accounts);
        var configuration = configuration(temporary);
        var profile = ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.NONE);
        var report = resource.open(configuration, profile);
        var scenario = report.startScenario("ScopeFixture", "scope", "owned account", List.of("component"));
        var setup = scenario.startPhase("SETUP");
        setup.setup(AtfGherkinKeyword.GIVEN, "registered account belongs to this lifecycle", "fixture",
                List.of(new AtfAssertion("Account available", "true",
                        Boolean.toString(accounts.current().isPresent()), accounts.current().isPresent())),
                AtfReportStatus.PASS);
        setup.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
        Files.createDirectories(report.reportPath());

        assertThrows(RuntimeException.class, resource::close);

        assertTrue(accounts.isClosed(), "publication failure must not retain the account registry");
        assertTrue(accounts.current().isEmpty());
        assertDoesNotThrow(resource::close, "publication failure is not a permit to close the report twice");
    }

    @Test
    void invalidScopeIsAConfigurationFailureNotAnImplicitSkip() {
        var observed = executeConfigured("invalid", false, Producer.class);
        assertTrue(observed.failedContainers > 0);
        assertEquals(0, observed.skipped);
        assertEquals(0, observed.created);
        assertTrue(observed.values.isEmpty());
    }

    private static Observations execute(boolean suite, boolean failPreparation, Class<?>... fixtures) {
        return executeConfigured(suite ? "suite" : null, failPreparation, fixtures);
    }

    private static Observations executeConfigured(String scope, boolean failPreparation, Class<?>... fixtures) {
        var observed = new Observations(failPreparation);
        OBSERVED.set(observed);
        try {
            var request = LauncherDiscoveryRequestBuilder.request()
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
            if (scope != null) {
                for (var parameter : ShopE2eSuite.class.getAnnotationsByType(ConfigurationParameter.class)) {
                    request.configurationParameter(parameter.key(), parameter.value());
                }
                request.configurationParameter(TestRunScope.CONFIGURATION_PARAMETER, scope);
            }
            for (Class<?> fixture : fixtures) request.selectors(selectClass(fixture));
            var summary = new SummaryGeneratingListener();
            LauncherFactory.create().execute(request.build(), summary, new TestExecutionListener() {
                @Override public void executionSkipped(TestIdentifier test, String reason) {
                    observed.skipReasons.add(reason);
                }
            });
            observed.failed = summary.getSummary().getTestsFailedCount();
            observed.failedContainers = summary.getSummary().getContainersFailedCount();
            observed.skipped = summary.getSummary().getTestsSkippedCount();
            return observed;
        } finally {
            OBSERVED.remove();
        }
    }

    private static ShopConfiguration configuration(Path output) {
        var base = ShopConfigurationLoader.load().withScreenshotPolicy(ReportScreenshotPolicy.NONE);
        return new ShopConfiguration(base.baseUrl(), base.accountFile(), base.productName(), base.productPath(),
                base.checkout(), base.browser(), base.uiRetry(), base.validationProfile(), base.api(),
                output, output.resolve("api"), base.authenticatedE2eEnabled(), base.reporting());
    }

    private static ShopCredentials fixture() {
        return new ShopCredentials("synthetic@scope-fixture.invalid", "redacted-fixture", "Scope", "Fixture");
    }

    private static final class Observations {
        private final boolean failPreparation;
        private final List<ScopedTestValue<ShopCredentials>> values = new ArrayList<>();
        private final List<Class<?>> classes = new ArrayList<>();
        private final List<String> rootIds = new ArrayList<>();
        private final List<String> skipReasons = new ArrayList<>();
        private int created;
        private long failed;
        private long failedContainers;
        private long skipped;
        private Observations(boolean failPreparation) { this.failPreparation = failPreparation; }
    }

    public static final class AccountProbe implements BeforeEachCallback {
        @Override public void beforeEach(ExtensionContext context) {
            var observed = OBSERVED.get();
            var account = ShopE2eTestSupport.accounts(context);
            observed.values.add(account);
            observed.classes.add(context.getRequiredTestClass());
            observed.rootIds.add(context.getRoot().getUniqueId());
            if (context.getRequiredTestClass() == Producer.class) {
                account.getOrCreate(() -> {
                    observed.created++;
                    if (observed.failPreparation) throw new IllegalStateException("uncertain registration");
                    return fixture();
                });
            } else {
                assertTrue(account.current().isPresent());
            }
        }
    }

    @Order(1)
    @ShopTestSupport.AuthenticatedE2e(accountSetup = ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
            registrationProfile = ShopTestSupport.RegistrationProfileMode.SYNTHETIC)
    @ShopTestSupport.AllowsRegistrationPerRun
    @ExtendWith(AccountProbe.class)
    static class Producer {
        @Test void registeredAccount() { assertEquals(1, OBSERVED.get().created); }
    }

    @Order(2)
    @ShopTestSupport.AuthenticatedE2e(accountFile = "atf-test-practice-shop/src/test/resources/"
            + "missing-scope-probe.local.properties")
    @ExtendWith(AccountProbe.class)
    static class Consumer {
        @Test void reusedAccount() { assertFalse(OBSERVED.get().values.isEmpty()); }
    }
}
