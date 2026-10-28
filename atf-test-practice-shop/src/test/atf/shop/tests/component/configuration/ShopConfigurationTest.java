package atf.shop.tests.component.configuration;

import atf.core.config.ApiTransportMode;
import atf.shop.tests.support.ShopComponentTest;
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
    void yamlDefinesOwnedJourneyAndSyntheticFixtureInSourceResources() {
        var configuration = ShopConfigurationLoader.load();

        assertTrue(configuration.baseUrl().isAbsolute());
        assertTrue(configuration.baseUrl().getHost().endsWith("practicesoftwaretesting.com"));
        assertEquals("Pliers", configuration.productName());
        assertTrue(configuration.usesDynamicProductPath());
        assertTrue(configuration.matchesProductPath(ShopUiRoutes.product("current-external-fixture")));
        assertFalse(configuration.matchesProductPath(ShopUiRoutes.category("pliers")));
        assertEquals(2, configuration.checkout().quantity());
        var catalog = configuration.catalogSelection();
        assertEquals(configuration.uri(ShopUiRoutes.HOME), catalog.homeUrl());
        assertEquals(configuration.productName(), catalog.productName());
        assertEquals(configuration.productPath(), catalog.productPath());
        assertEquals(configuration.checkout().quantity(), catalog.quantity());
        assertTrue(catalog.matchesProductPath(ShopUiRoutes.product("current-external-fixture")));
        assertFalse(catalog.matchesProductPath(ShopUiRoutes.category("pliers")));
        assertEquals("Cash on Delivery", configuration.checkout().paymentMethod());
        assertEquals("United States of America (the)", configuration.checkout().billing().country());
        assertEquals("53703", configuration.checkout().billing().postalCode());
        assertEquals("Wayne Lock", configuration.checkout().billing().street());
        assertEquals("Cartwrightmouth", configuration.checkout().billing().city());
        assertEquals("Alaska", configuration.checkout().billing().state());
        assertEquals(70, configuration.browser().zoomPercent());
        assertTrue(configuration.browser().screenshotOnFailure());
        assertEquals(1, configuration.uiRetry().maxAttempts());
        assertFalse(configuration.uiRetry().enabled());
        assertFalse(configuration.browser().retryPolicy().enabled());
        assertEquals(ShopValidationProfile.UI_API, configuration.validationProfile());
        assertTrue(configuration.api().authenticatedPrecondition());
        assertEquals(6, configuration.api().evidenceBinding().checkpoints().size());
        assertTrue(configuration.api().evidenceBinding().includes(ShopApiCheckpoint.PRODUCT));
        assertTrue(configuration.api().evidenceBinding().includes(ShopApiCheckpoint.PAYMENT));
        assertTrue(configuration.api().evidenceBinding().includes(ShopApiCheckpoint.ORDER));
        assertTrue(configuration.uiReportDirectory().toString().endsWith("artifacts\\reports\\ui"));
        assertTrue(configuration.apiReportDirectory().toString().endsWith("artifacts\\reports\\api"));
        assertFalse(configuration.uiReportDirectory().equals(configuration.apiReportDirectory()));
        assertFalse(configuration.authenticatedE2eEnabled());
        assertFalse(configuration.authenticatedE2eAllowed());
        assertEquals(ShopAccountFixturePolicy.SOURCE_TEST_RESOURCE, configuration.accountPolicy());
        assertTrue(configuration.accountPolicy().expectedEvidence().contains("registered local account"));
        assertTrue(configuration.accountConfigured());
        assertEquals(ShopReportConfiguration.DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS,
                configuration.reporting().systemBodyPreviewCharacters());
        assertEquals(ReportScreenshotPolicy.EACH_CHECKPOINT,
                configuration.reporting().screenshotPolicy());
        assertEquals("resources", configuration.accountFile().getParent().getFileName().toString(),
                "the authenticated fixture is intentionally owned by test resources");
        assertEquals("practice-software-testing-account.properties",
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
