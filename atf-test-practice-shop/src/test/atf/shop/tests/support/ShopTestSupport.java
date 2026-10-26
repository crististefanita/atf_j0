package atf.shop.tests.support;

import atf.core.config.ApiTransportMode;
import atf.testkit.scope.TestRunScope;
import atf.shop.config.model.ShopCredentials;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopRegistrationProfile;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExecutionCondition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Shared composition and selection boundary for Practice Shop tests. */
public final class ShopTestSupport {
    private static final String AUTHENTICATED_REASON =
            "authenticated Practice Shop fixture is not configured";
    private static final String LIVE_REASON =
            "authenticated live Practice Shop API fixture is not configured";

    private ShopTestSupport() { }

    public enum BrowserMode {
        HEADED,
        HEADLESS
    }

    public enum AccountSetupMode {
        REUSE_FIXTURE,
        REGISTER_PER_RUN
    }

    public enum RegistrationProfileMode {
        LOCAL,
        SYNTHETIC
    }

    /**
     * Declares a directly runnable authenticated E2E composition root.
     * Defaults intentionally point to the ignored local fixture and headed
     * browser so an IntelliJ run is useful without VM options.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @ExtendWith(AuthenticatedE2eCondition.class)
    public @interface AuthenticatedE2e {
        String accountFile() default
                "atf-test-practice-shop/src/test/resources/practice-software-testing-account.local.properties";

        BrowserMode browserMode() default BrowserMode.HEADED;

        int zoomPercent() default 70;

        AccountSetupMode accountSetup() default AccountSetupMode.REUSE_FIXTURE;

        RegistrationProfileMode registrationProfile() default RegistrationProfileMode.LOCAL;

        String registrationProfileFile() default
                "atf-test-practice-shop/src/test/resources/practice-software-testing-registration.local.properties";
    }

    /**
     * Explicitly allows the annotated E2E class to create a persistent external account per run.
     * Keeping this opt-in separate from {@link AuthenticatedE2e} makes the side effect visible
     * at the test declaration and avoids coupling policy to a class name.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AllowsRegistrationPerRun { }

    /** Selects only tests that have an external account fixture available. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @ExtendWith(AuthenticatedFixtureCondition.class)
    public @interface RequiresAuthenticatedFixture { }

    /** Selects only tests that can prove the authenticated API against HTTP. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @ExtendWith(LiveAuthenticatedApiCondition.class)
    public @interface RequiresLiveAuthenticatedApi { }

    public static final class AuthenticatedFixtureCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return ShopConfigurationLoader.load().authenticatedE2eAllowed()
                    ? ConditionEvaluationResult.enabled("authenticated fixture is configured")
                    : ConditionEvaluationResult.disabled(AUTHENTICATED_REASON);
        }
    }

    public static final class LiveAuthenticatedApiCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            ShopConfiguration configuration = ShopConfigurationLoader.load();
            boolean enabled = configuration.authenticatedE2eAllowed()
                    && configuration.api().mode() == ApiTransportMode.LIVE;
            return enabled
                    ? ConditionEvaluationResult.enabled("authenticated live API fixture is configured")
                    : ConditionEvaluationResult.disabled(LIVE_REASON);
        }
    }

    public static final class AuthenticatedE2eCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            TestRunScope scope = TestRunScope.selected(context);
            try {
                ShopConfiguration configuration = configurationFor(context.getRequiredTestClass());
                AuthenticatedE2e profile = context.getRequiredTestClass()
                        .getAnnotation(AuthenticatedE2e.class);
                if (scope == TestRunScope.SUITE && profile != null
                        && profile.accountSetup() == AccountSetupMode.REUSE_FIXTURE) {
                    var accounts = ShopE2eTestSupport.accounts(context);
                    if (accounts.hasFailed()) return ConditionEvaluationResult.disabled(
                            "suite account setup failed; inspect SETUP; automatic registration retry is forbidden");
                    if (accounts.current().isPresent()) return ConditionEvaluationResult.enabled(
                            "authenticated account was registered earlier in this Practice Shop suite");
                }
                String skipReason = authenticatedSetupSkipReason(context.getRequiredTestClass(), configuration);
                return skipReason == null
                        ? ConditionEvaluationResult.enabled("authenticated IntelliJ E2E profile is configured")
                        : ConditionEvaluationResult.disabled(skipReason);
            } catch (RuntimeException error) {
                return ConditionEvaluationResult.disabled(
                        AUTHENTICATED_REASON + "; check the ignored local account fixture and class profile");
            }
        }
    }

    /** Safe, actionable skip explanation; credential contents are never included. */
    static String authenticatedFixtureSkipReason(ShopConfiguration configuration) {
        if (configuration == null) return AUTHENTICATED_REASON;
        if (!configuration.authenticatedE2eEnabled()) {
            return "authenticated E2E profile is disabled; use the class-level @AuthenticatedE2e profile";
        }
        Path fixture = configuration.accountFile();
        if (!configuration.accountPolicy().accepts(fixture)) {
            return "account fixture must be stored under the module's src/test/resources directory";
        }
        if (!Files.isRegularFile(fixture)) {
            return "local account fixture is missing; create the ignored practice-software-testing-account.local.properties "
                    + "file with a registered account's email and password";
        }
        final var credentials = safeLoad(fixture);
        if (credentials == null) {
            return "local account fixture is unreadable or lacks required email/password fields; "
                    + "credential values are not shown";
        }
        if (!credentials.hasLiveAccountCandidate()) {
            return "fixture must use a registered, non-reserved account email; credential values are not shown";
        }
        return null;
    }

    /** Preflight follows the class annotation and never includes configured values in its message. */
    static String authenticatedSetupSkipReason(Class<?> testClass, ShopConfiguration configuration) {
        AuthenticatedE2e profile = testClass == null ? null : testClass.getAnnotation(AuthenticatedE2e.class);
        if (profile == null) return AUTHENTICATED_REASON;
        if (configuration == null || !configuration.authenticatedE2eEnabled()) {
            return "authenticated E2E is disabled; enable the class-level @AuthenticatedE2e profile";
        }
        if (profile.accountSetup() == AccountSetupMode.REUSE_FIXTURE) {
            return authenticatedFixtureSkipReason(configuration);
        }
        if (!testClass.isAnnotationPresent(AllowsRegistrationPerRun.class)) {
            return "REGISTER_PER_RUN requires the explicit @AllowsRegistrationPerRun marker; "
                    + "use REUSE_FIXTURE for this class";
        }
        if (profile.registrationProfile() == RegistrationProfileMode.SYNTHETIC) return null;
        try {
            Path registrationFile = ShopConfigurationLoader.resolveRegistrationProfileFile(
                    profile.registrationProfileFile());
            if (!Files.isRegularFile(registrationFile)) {
                return "local registration profile is missing; copy the example to "
                        + "practice-software-testing-registration.local.properties and configure it; "
                        + "values are never shown";
            }
            ShopRegistrationProfile.load(registrationFile)
                    .newRegistrationData("preflight000000000000000000000000");
            return null;
        } catch (RuntimeException invalidProfile) {
            return "local registration profile is invalid or uses a reserved email domain; "
                    + "check the required keys and controlled email domain; values are never shown";
        }
    }

    private static ShopCredentials safeLoad(Path fixture) {
        try {
            return ShopCredentials.load(fixture);
        } catch (RuntimeException invalidFixture) {
            return null;
        }
    }

    /** Applies the class-level launch profile without mutating global system properties. */
    public static ShopConfiguration configurationFor(Class<?> testClass) {
        ShopConfiguration base = ShopConfigurationLoader.load();
        AuthenticatedE2e profile = testClass.getAnnotation(AuthenticatedE2e.class);
        if (profile == null) return base;
        return base.forAuthenticatedE2e(
                ShopConfigurationLoader.resolveAccountFile(profile.accountFile()),
                profile.browserMode() == BrowserMode.HEADLESS,
                profile.zoomPercent());
    }
}
