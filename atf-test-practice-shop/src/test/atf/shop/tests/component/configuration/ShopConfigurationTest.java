package atf.shop.tests.component.configuration;

import atf.core.config.ApiTransportMode;
import atf.shop.tests.support.ShopComponentTest;
import atf.testkit.assertions.RecordedAssertions;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.shop.config.model.ShopAccountFixturePolicy;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.api.ShopApiPrecondition;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopReportConfiguration;
import atf.shop.config.ShopValidationProfile;
import atf.shop.routes.ShopUiRoutes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ShopComponentTest
class ShopConfigurationTest {
    @Test
    void malformedEvidenceAndBillingPoliciesCannotSilentlyUseDefaults(@TempDir Path temp) throws Exception {
        Path yaml = temp.resolve("invalid-policy.yaml");
        for (String policy : List.of("flowMode: 7", "flowMode: unknown", "actionDetailsMode: false",
                "screenshotPolicy: ''", "flowMod: unified-flow")) {
            Files.writeString(yaml, "practiceShop:\n  baseUrl: https://fixture.test\n  reporting:\n    " + policy + "\n");
            assertThrows(IllegalStateException.class, () -> ShopConfigurationLoader.load(yaml), policy);
        }
        Files.writeString(yaml, "practiceShop:\n  baseUrl: https://fixture.test\n  checkout:\n    billingStrategy: 7\n");
        assertThrows(IllegalStateException.class, () -> ShopConfigurationLoader.load(yaml));
    }
    @Test
    @ResourceLock(ShopConfigurationLoader.CONFIG_PROPERTY)
    void explicitLaunchConfigurationIsUsedWithoutEditingThePackagedProfile(@TempDir Path temp) throws Exception {
        String previous = System.getProperty(ShopConfigurationLoader.CONFIG_PROPERTY);
        Path yaml = temp.resolve("controlled-launch.yaml");
        Files.writeString(yaml, """
                practiceShop:
                  baseUrl: https://controlled.fixture.test
                  headless: true
                  reporting:
                    flowMode: unified-flow
                """);
        try {
            System.setProperty(ShopConfigurationLoader.CONFIG_PROPERTY, yaml.toString());
            var launch = ShopConfigurationLoader.load();
            assertEquals("controlled.fixture.test", launch.baseUrl().getHost());
            assertTrue(launch.browser().headless());
            assertEquals("unified-flow", launch.reporting().flowMode().configValue());
            System.setProperty(ShopConfigurationLoader.CONFIG_PROPERTY, temp.resolve("missing.yaml").toString());
            assertThrows(IllegalStateException.class, ShopConfigurationLoader::load);
        } finally {
            if (previous == null) System.clearProperty(ShopConfigurationLoader.CONFIG_PROPERTY);
            else System.setProperty(ShopConfigurationLoader.CONFIG_PROPERTY, previous);
        }
    }
    @Test
    void yamlDefinesOwnedJourneyAndSyntheticFixtureInSourceResources(RecordedAssertions assertions) {
        var configuration = ShopConfigurationLoader.load();

        assertions.isTrue("Base URL is absolute", configuration.baseUrl().isAbsolute());
        assertions.isTrue("Practice Shop host", configuration.baseUrl().getHost()
                .endsWith("practicesoftwaretesting.com"));
        assertions.equals("Product name", "Pliers", configuration.productName());
        assertions.isTrue("Dynamic product path", configuration.usesDynamicProductPath());
        assertions.isTrue("Configured product route matches", configuration.matchesProductPath(
                ShopUiRoutes.product("current-external-fixture")));
        assertions.isFalse("Category route is not a product", configuration.matchesProductPath(
                ShopUiRoutes.category("pliers")));
        assertions.equals("Checkout quantity", 2, configuration.checkout().quantity());
        var catalog = configuration.catalogSelection();
        assertions.equals("Catalog home URL", configuration.uri(ShopUiRoutes.HOME), catalog.homeUrl());
        assertions.equals("Catalog product", configuration.productName(), catalog.productName());
        assertions.equals("Catalog product path", configuration.productPath(), catalog.productPath());
        assertions.equals("Catalog quantity", configuration.checkout().quantity(), catalog.quantity());
        assertions.isTrue("Catalog matches product route", catalog.matchesProductPath(
                ShopUiRoutes.product("current-external-fixture")));
        assertions.isFalse("Catalog rejects category route", catalog.matchesProductPath(
                ShopUiRoutes.category("pliers")));
        assertions.equals("Payment method", "Cash on Delivery", configuration.checkout().paymentMethod());
        assertions.equals("Billing country", "United States of America (the)",
                configuration.checkout().billing().country());
        assertions.equals("Billing postal code", "53703", configuration.checkout().billing().postalCode());
        assertions.equals("Billing street", "Wayne Lock", configuration.checkout().billing().street());
        assertions.equals("Billing city", "Cartwrightmouth", configuration.checkout().billing().city());
        assertions.equals("Billing state", "Alaska", configuration.checkout().billing().state());
        assertions.equals("Browser zoom", 70, configuration.browser().zoomPercent());
        assertions.isTrue("Screenshot on failure", configuration.browser().screenshotOnFailure());
        assertions.equals("UI retry attempts", 1, configuration.uiRetry().maxAttempts());
        assertions.isFalse("UI retry disabled", configuration.uiRetry().enabled());
        assertions.isFalse("Browser retry disabled", configuration.browser().retryPolicy().enabled());
        assertions.equals("Validation profile", ShopValidationProfile.UI_API, configuration.validationProfile());
        assertions.isTrue("Authenticated API precondition", configuration.api().authenticatedPrecondition());
        assertions.equals("API checkpoint count", 6, configuration.api().evidenceBinding().checkpoints().size());
        assertions.isTrue("Product API checkpoint", configuration.api().evidenceBinding()
                .includes(ShopApiCheckpoint.PRODUCT));
        assertions.isTrue("Payment API checkpoint", configuration.api().evidenceBinding()
                .includes(ShopApiCheckpoint.PAYMENT));
        assertions.isTrue("Order API checkpoint", configuration.api().evidenceBinding()
                .includes(ShopApiCheckpoint.ORDER));
        assertions.isTrue("UI report directory", configuration.uiReportDirectory().toString()
                .endsWith("artifacts\\reports\\ui"));
        assertions.isTrue("API report directory", configuration.apiReportDirectory().toString()
                .endsWith("artifacts\\reports\\api"));
        assertions.isFalse("Distinct report directories", configuration.uiReportDirectory()
                .equals(configuration.apiReportDirectory()));
        assertions.isFalse("Authenticated E2E disabled", configuration.authenticatedE2eEnabled());
        assertions.isFalse("Authenticated E2E not allowed", configuration.authenticatedE2eAllowed());
        assertions.equals("Account fixture policy", ShopAccountFixturePolicy.SOURCE_TEST_RESOURCE,
                configuration.accountPolicy());
        assertions.isTrue("Local account evidence policy", configuration.accountPolicy()
                .expectedEvidence().contains("registered local account"));
        assertions.isTrue("Account fixture configured", configuration.accountConfigured());
        assertions.equals("System preview limit", ShopReportConfiguration.DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS,
                configuration.reporting().systemBodyPreviewCharacters());
        assertions.equals("Screenshot policy", ReportScreenshotPolicy.EACH_CHECKPOINT,
                configuration.reporting().screenshotPolicy());
        assertions.equals("Fixture parent is test resources", "resources",
                configuration.accountFile().getParent().getFileName().toString());
        assertions.equals("Fixture filename", "practice-software-testing-account.properties",
                configuration.accountFile().getFileName().toString());
    }

    @Test
    void resolvesRelativeUiRoutesFromTheYamlBaseUrl() {
        var configuration = ShopConfigurationLoader.load();

        assertEquals(configuration.baseUrl().resolve("product/1"),
                configuration.uri(ShopUiRoutes.product("1")));
        assertEquals(configuration.baseUrl().resolve("checkout"),
                configuration.uri(ShopUiRoutes.CHECKOUT));
    }

    @Test
    void explicitYamlRetainsTheExactProductNameInsteadOfReplacingItWithTheDefault(@TempDir Path temp)
            throws Exception {
        Path yaml = temp.resolve("practice-shop.yaml");
        for (String product : List.of("Pliers", "Combination Pliers")) {
            Files.writeString(yaml, """
                    practiceShop:
                      baseUrl: https://practicesoftwaretesting.com
                      productName: "%s"
                    """.formatted(product));
            assertEquals(product, ShopConfigurationLoader.load(yaml).productName());
        }
    }

    @Test
    void yamlDistinguishesMissingApiEvidenceFromAnExplicitlyEmptyList(@TempDir Path temp) throws Exception {
        Path missingSelection = temp.resolve("missing-selection.yaml");
        Path emptySelection = temp.resolve("empty-selection.yaml");
        Files.writeString(missingSelection, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  validation:
                    profile: ui-api
                """);
        Files.writeString(emptySelection, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  validation:
                    profile: ui-api
                    apiEvidence: []
                """);

        var inherited = ShopConfigurationLoader.load(missingSelection);
        var explicitlyEmpty = ShopConfigurationLoader.load(emptySelection);

        assertEquals(6, inherited.api().evidenceBinding().checkpoints().size());
        assertTrue(explicitlyEmpty.api().evidenceBinding().checkpoints().isEmpty());
    }

    @Test
    @ResourceLock("atf.practice.api-evidence-checkpoints")
    void explicitlyBlankCheckpointOverrideClearsYamlSelectionButAbsentOverrideInheritsIt(
            @TempDir Path temp) throws Exception {
        String property = "atf.practice.api-evidence-checkpoints";
        String previous = System.getProperty(property);
        Path yaml = temp.resolve("checkpoint-override.yaml");
        Files.writeString(yaml, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  validation:
                    profile: ui-api
                    apiEvidence: [product]
                """);
        try {
            System.clearProperty(property);
            assertEquals(1, ShopConfigurationLoader.load(yaml)
                    .api().evidenceBinding().checkpoints().size());

            System.setProperty(property, "");
            assertTrue(ShopConfigurationLoader.load(yaml)
                    .api().evidenceBinding().checkpoints().isEmpty());

            System.setProperty(property, "payment, order");
            var overridden = ShopConfigurationLoader.load(yaml).api().evidenceBinding();
            assertEquals(2, overridden.checkpoints().size());
            assertTrue(overridden.includes(ShopApiCheckpoint.PAYMENT));
            assertTrue(overridden.includes(ShopApiCheckpoint.ORDER));
            assertFalse(overridden.includes(ShopApiCheckpoint.PRODUCT));
        } finally {
            if (previous == null) System.clearProperty(property);
            else System.setProperty(property, previous);
        }
    }

    @Test
    void uiOnlyHarnessProfileKeepsRuntimeConfigurationButDisablesApiEvidence() {
        var configured = ShopConfigurationLoader.load();
        var uiOnly = configured.withValidationProfile(ShopValidationProfile.UI_ONLY);

        assertEquals(ShopValidationProfile.UI_ONLY, uiOnly.validationProfile());
        assertTrue(uiOnly.api().evidenceBinding().checkpoints().isEmpty());
        assertEquals(configured.baseUrl(), uiOnly.baseUrl());
        assertEquals(configured.productName(), uiOnly.productName());
        assertEquals(configured.checkout(), uiOnly.checkout());
        assertEquals(configured.browser(), uiOnly.browser());
        assertEquals(configured.uiReportDirectory(), uiOnly.uiReportDirectory());
    }

    @Test
    void authenticatedRunCannotDeclareAnAnonymousApiPrecondition() {
        assertThrows(IllegalArgumentException.class, () -> new atf.shop.config.ShopConfiguration(
                configuration().baseUrl(), configuration().accountFile(), configuration().productName(),
                configuration().productPath(), configuration().checkout(), configuration().browser(),
                configuration().validationProfile(),
                new ShopApiConfiguration(configuration().api().endpoint(),
                        configuration().api().mode(), ShopApiPrecondition.ANONYMOUS,
                        configuration().api().timeout()),
                configuration().uiReportDirectory(), configuration().apiReportDirectory(), true));
    }

    @Test
    void annotationStyleAuthProfilePromotesYamlToHeadedLiveE2e(@TempDir Path temp) throws Exception {
        var base = configuration();
        Path sourceFixture = temp.resolve("src/test/resources/account.properties");
        Files.createDirectories(sourceFixture.getParent());
        Files.writeString(sourceFixture,
                "email=customer@practicesoftwaretesting.com\npassword=secret\n");

        var configured = base.forAuthenticatedE2e(sourceFixture, false, 70);

        assertTrue(configured.authenticatedE2eEnabled());
        assertTrue(configured.authenticatedE2eAllowed());
        assertEquals(ApiTransportMode.LIVE, configured.api().mode());
        assertEquals(ShopApiPrecondition.AUTHENTICATED, configured.api().precondition());
        assertFalse(configured.browser().headless());
        assertEquals(70, configured.browser().zoomPercent());
    }

    @Test
    void authenticatedGateRequiresReadableSourceFixture(@TempDir Path temp) throws Exception {
        var base = configuration();
        Path nonFixture = temp.resolve("external.properties");
        Files.writeString(nonFixture,
                "email=customer@practicesoftwaretesting.com\npassword=secret\n");
        Path sourceFixture = temp.resolve("src/test/resources/account.properties");
        Files.createDirectories(sourceFixture.getParent());
        Files.writeString(sourceFixture,
                "email=customer@practicesoftwaretesting.com\npassword=secret\n");

        assertFalse(withAccountFile(base, nonFixture).authenticatedE2eAllowed());
        assertTrue(withAccountFile(base, sourceFixture).authenticatedE2eAllowed());
    }

    @Test
    void rejectsAnExternalAccountOverride(@TempDir Path temp) {
        String property = "atf.practice.account-file";
        String previous = System.getProperty(property);
        System.setProperty(property, temp.resolve("external.properties").toString());
        try {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    ShopConfigurationLoader::load);
            assertTrue(failure.getCause() instanceof IllegalArgumentException);
            assertTrue(failure.getCause().getMessage().contains("src/test/resources"));
        } finally {
            if (previous == null) System.clearProperty(property);
            else System.setProperty(property, previous);
        }
    }

    @Test
    void directConfigurationAcceptsTheOwnedSourceResourceFixture(@TempDir Path temp) throws Exception {
        var base = configuration();
        Path sourceFixture = temp.resolve("src/test/resources/account.properties");
        Files.createDirectories(sourceFixture.getParent());
        Files.writeString(sourceFixture,
                "email=customer@practicesoftwaretesting.com\npassword=secret\n");

        var configured = withAccountFile(base, sourceFixture);

        assertTrue(configured.accountConfigured(),
                "the typed direct configuration must honor the same source fixture policy");
        assertTrue(configured.authenticatedE2eAllowed());
    }

    @Test
    void rejectsAnUnboundedSystemPreviewPolicy() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShopReportConfiguration(0));
    }

    private static atf.shop.config.ShopConfiguration configuration() {
        return ShopConfigurationLoader.load();
    }

    private static atf.shop.config.ShopConfiguration withAccountFile(
            atf.shop.config.ShopConfiguration base, Path accountFile) {
        return new atf.shop.config.ShopConfiguration(base.baseUrl(), accountFile,
                base.productName(), base.productPath(), base.checkout(), base.browser(), base.uiRetry(),
                base.validationProfile(), base.api(), base.uiReportDirectory(), base.apiReportDirectory(), true);
    }
}
