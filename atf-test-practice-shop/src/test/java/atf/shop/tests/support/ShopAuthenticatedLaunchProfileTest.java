package atf.shop.tests.support;

import atf.api.core.config.ApiTransportMode;
import atf.api.testkit.AtfComponentTest;
import atf.api.testkit.scope.ScopedTestValue;
import atf.shop.config.api.ShopApiPrecondition;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.model.ShopCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.suite.api.SelectClasses;

import atf.shop.tests.suites.ShopE2eSuite;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopAuthenticatedLaunchProfileTest {
    private static final String LOCAL_FIXTURE =
            "atf-test-practice-shop/src/test/resources/practice-software-testing-account.local.properties";

    @Test
    void eachAuthenticatedClassResolvesItsOwnIntelliJProfileWithoutVmOverrides() throws Exception {
        assertAuthenticatedProfile("atf.shop.tests.e2e.ShopNewAccountPurchaseE2eTest");
        assertAuthenticatedProfile("atf.shop.tests.e2e.ShopAuthenticatedPurchaseE2eTest");
    }

    @Test
    void missingFixtureExplainsHowToPrepareTheReusableLocalAccount(@TempDir Path temp) {
        var configuration = authenticatedConfiguration(temp.resolve(
                "src/test/resources/practice-software-testing-account.local.properties"));

        String reason = ShopTestSupport.authenticatedFixtureSkipReason(configuration);

        assertNotNull(reason);
        assertTrue(reason.contains("fixture is missing"));
        assertTrue(reason.contains("registered account's email and password"));
    }

    @Test
    void malformedOrReservedFixtureSkipsWithoutLeakingCredentialValues(@TempDir Path temp) throws Exception {
        Path fixture = temp.resolve("src/test/resources/account.local.properties");
        Files.createDirectories(fixture.getParent());
        Files.writeString(fixture,
                "email=customer@example.test\npassword=never-print-this\n");

        String reason = ShopTestSupport.authenticatedFixtureSkipReason(
                authenticatedConfiguration(fixture));

        assertAll(
                () -> assertNotNull(reason),
                () -> assertTrue(reason.contains("registered, non-reserved account email")),
                () -> assertFalse(reason.contains("never-print-this")));
    }

    @Test
    void incompleteFixtureExplainsRequiredKeysWithoutPrintingValues(@TempDir Path temp) throws Exception {
        Path fixture = temp.resolve("src/test/resources/account.local.properties");
        Files.createDirectories(fixture.getParent());
        Files.writeString(fixture, "email=customer@practicesoftwaretesting.com\n");

        String reason = ShopTestSupport.authenticatedFixtureSkipReason(
                authenticatedConfiguration(fixture));

        assertNotNull(reason);
        assertTrue(reason.contains("lacks required email/password fields"));
        assertFalse(reason.contains("customer@practicesoftwaretesting.com"));
    }

    @Test
    void fixtureOutsideTestResourcesIsRejectedWithoutShowingItsPath(@TempDir Path temp) {
        Path fixture = temp.resolve("secrets/account.local.properties");

        String reason = ShopTestSupport.authenticatedFixtureSkipReason(
                authenticatedConfiguration(fixture));

        assertNotNull(reason);
        assertTrue(reason.contains("src/test/resources"));
        assertFalse(reason.contains(fixture.toString()));
    }

    @Test
    void registerPerRunCannotSilentlyActivateOnAnotherAuthenticatedClass() {
        var configuration = ShopTestSupport.configurationFor(NonPurchaseAuthenticatedTest.class);

        String reason = ShopTestSupport.authenticatedSetupSkipReason(
                NonPurchaseAuthenticatedTest.class, configuration);

        assertNotNull(reason);
        assertTrue(reason.contains("@AllowsRegistrationPerRun"));
    }

    @Test
    void authenticatedPurchaseProfilesKeepFixtureReuseSeparateFromRegistration() throws ClassNotFoundException {
        Class<?> purchase = Class.forName(
                "atf.shop.tests.e2e.ShopAuthenticatedPurchaseE2eTest");
        var purchaseProfile = purchase.getAnnotation(ShopTestSupport.AuthenticatedE2e.class);

        assertFalse(purchase.isAnnotationPresent(ShopTestSupport.AllowsRegistrationPerRun.class));
        assertEquals(ShopTestSupport.AccountSetupMode.REUSE_FIXTURE,
                purchaseProfile.accountSetup());
        assertEquals(LOCAL_FIXTURE, purchaseProfile.accountFile());
        Class<?> newAccountPurchase = Class.forName(
                "atf.shop.tests.e2e.ShopNewAccountPurchaseE2eTest");
        var accountProfile = newAccountPurchase.getAnnotation(ShopTestSupport.AuthenticatedE2e.class);
        assertTrue(newAccountPurchase.isAnnotationPresent(
                ShopTestSupport.AllowsRegistrationPerRun.class));
        assertEquals(ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN,
                accountProfile.accountSetup());
        assertEquals(ShopTestSupport.RegistrationProfileMode.SYNTHETIC,
                accountProfile.registrationProfile());
        assertNull(ShopTestSupport.authenticatedSetupSkipReason(newAccountPurchase,
                ShopTestSupport.configurationFor(newAccountPurchase)));
        assertFalse(NonPurchaseAuthenticatedTest.class.isAnnotationPresent(
                ShopTestSupport.AllowsRegistrationPerRun.class));
    }

    @Test
    void reusableFixturePreflightAcceptsARegisteredTestAccountWithoutExposingValues(@TempDir Path temp)
            throws Exception {
        Path fixture = temp.resolve("module/src/test/resources/account.local.properties");
        Files.createDirectories(fixture.getParent());
        Files.writeString(fixture,
                "email=registered.customer@practice-shop.dev\npassword=secret-value\n");

        String reason = ShopTestSupport.authenticatedSetupSkipReason(
                Class.forName("atf.shop.tests.e2e.ShopAuthenticatedPurchaseE2eTest"),
                authenticatedConfiguration(fixture));

        assertNull(reason);
    }

    @Test
    void bothAuthenticatedPurchaseClassesExposeOneBusinessScenario() throws ClassNotFoundException {
        for (String className : new String[]{
                "atf.shop.tests.e2e.ShopNewAccountPurchaseE2eTest",
                "atf.shop.tests.e2e.ShopAuthenticatedPurchaseE2eTest"}) {
            Class<?> testClass = Class.forName(className);
            long scenarios = java.util.Arrays.stream(testClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(org.junit.jupiter.api.Test.class))
                    .count();

            assertEquals(1, scenarios, testClass.getSimpleName()
                    + " must report one complete purchase scenario after setup");
            assertTrue(testClass.getSimpleName().contains("Purchase"),
                    "E2E class names must identify the purchase business outcome");
        }
    }

    @Test
    void shopSuiteSelectsEveryPurchaseE2eInExplicitBusinessOrder() throws ClassNotFoundException {
        SelectClasses selected = ShopE2eSuite.class
                .getAnnotation(SelectClasses.class);

        assertArrayEquals(new Class<?>[]{
                        Class.forName("atf.shop.tests.e2e.ShopNewAccountPurchaseE2eTest"),
                        Class.forName("atf.shop.tests.e2e.ShopAuthenticatedPurchaseE2eTest"),
                        Class.forName("atf.shop.tests.e2e.ShopPurchaseE2eTest"),
                        Class.forName("atf.shop.tests.e2e.ShopUiOnlyPurchaseE2eTest"),
                        Class.forName("atf.shop.tests.e2e.ShopUiOnlySingleFlowPurchaseE2eTest")},
                selected.value());
        var account = new ScopedTestValue<ShopCredentials>();
        var generatedAccount = new ShopCredentials(
                "customer@practice-shop.dev", "secret", "Test", "Customer");
        account.getOrCreate(() -> generatedAccount);
        assertSame(generatedAccount, account.current().orElseThrow());
        account.close();
        assertTrue(account.current().isEmpty());
    }

    @Test
    void registrationProfileMustUseAnIgnoredLocalPropertiesFile(@TempDir Path temp) {
        Path valid = temp.resolve(
                "project/atf-test-practice-shop/src/test/resources/registration.local.properties");
        Path wrongModule = temp.resolve(
                "project/atf-test-kit/src/test/resources/registration.local.properties");
        Path wrongExtension = temp.resolve(
                "project/atf-test-practice-shop/src/test/resources/registration.properties");

        var moduleFailure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ShopConfigurationLoader.resolveRegistrationProfileFile(
                        wrongModule.toString()));
        var extensionFailure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ShopConfigurationLoader.resolveRegistrationProfileFile(
                        wrongExtension.toString()));

        assertAll(
                () -> assertEquals(valid.toAbsolutePath().normalize(),
                        ShopConfigurationLoader.resolveRegistrationProfileFile(valid.toString())),
                () -> assertTrue(moduleFailure.getMessage().contains("atf-test-practice-shop/src/test/resources")),
                () -> assertTrue(extensionFailure.getMessage().contains(".local.properties")));
    }

    private static void assertAuthenticatedProfile(String testClassName) throws ClassNotFoundException {
        Class<?> testClass = Class.forName(testClassName);
        var configuration = ShopTestSupport.configurationFor(testClass);

        assertAll(testClass.getSimpleName(),
                () -> assertTrue(configuration.authenticatedE2eEnabled()),
                () -> assertTrue(configuration.accountFile().endsWith(Path.of(LOCAL_FIXTURE))),
                () -> assertEquals(ApiTransportMode.LIVE, configuration.api().mode()),
                () -> assertEquals(ShopApiPrecondition.AUTHENTICATED,
                        configuration.api().precondition()),
                () -> assertFalse(configuration.browser().headless()),
                () -> assertEquals(70, configuration.browser().zoomPercent()));
    }

    private static ShopConfiguration authenticatedConfiguration(Path fixture) {
        return ShopConfigurationLoader.load().forAuthenticatedE2e(fixture, false, 70);
    }

    @ShopTestSupport.AuthenticatedE2e(
            accountSetup = ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN)
    static class NonPurchaseAuthenticatedTest { }
}
