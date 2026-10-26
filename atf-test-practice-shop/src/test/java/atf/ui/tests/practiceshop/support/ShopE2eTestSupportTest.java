package atf.ui.tests.practiceshop.support;

import atf.api.testkit.AtfUnitTest;
import atf.api.testkit.reporting.AtfReportPath;
import atf.api.testkit.reporting.AtfReportPathPolicyResolver;
import atf.api.testkit.channel.AtfTestChannel;
import atf.api.testkit.channel.TestChannel;
import atf.api.testkit.evidence.AtfUiEvidence;
import atf.api.testkit.evidence.AtfUiEvidenceFlow;
import atf.api.testkit.evidence.AtfUiScreenshotPolicy;
import atf.api.testkit.scope.ScopedTestValue;
import atf.api.testkit.scope.TestRunScope;
import atf.ui.tests.practiceshop.suites.ShopE2eSuite;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.api.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.ui.practiceshop.config.api.ShopApiCheckpoint;
import atf.ui.practiceshop.config.ShopConfigurationLoader;
import atf.ui.practiceshop.config.model.ShopCredentials;
import atf.ui.practiceshop.config.ShopReportConfiguration;
import atf.ui.practiceshop.config.ShopValidationProfile;
import atf.api.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.practiceshop.flow.ShopPurchaseFlow;
import atf.ui.practiceshop.support.validation.ShopPurchaseConfirmation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ShopE2eTestSupportTest {
    @AtfTestChannel(TestChannel.UI)
    private static class UiChannel { }

    @AtfTestChannel(TestChannel.UI_API)
    private static class CombinedChannel { }

    @AtfTestChannel(TestChannel.API)
    private static class ApiChannel { }

    @AtfTestChannel(TestChannel.UI)
    @ShopE2eTestSupport.WithE2eHarness(profile = ShopE2eTestSupport.HarnessProfile.UI_API)
    private static class ConflictingChannel { }

    @Test
    void channelPreflightSelectsEvidenceAndRejectsUnsupportedOrMissingCapabilities() {
        var configured = ShopConfigurationLoader.load();
        var ui = ShopE2eTestSupport.configurationForChannel(configured, null, UiChannel.class);
        var combined = ShopE2eTestSupport.configurationForChannel(configured, null, CombinedChannel.class);
        assertEquals(ShopValidationProfile.UI_ONLY, ui.validationProfile());
        assertTrue(ui.api().evidenceBinding().checkpoints().isEmpty());
        assertEquals(ShopValidationProfile.UI_API, combined.validationProfile());
        assertFalse(combined.api().evidenceBinding().checkpoints().isEmpty());
        assertThrows(IllegalStateException.class,
                () -> ShopE2eTestSupport.configurationForChannel(configured, null, ApiChannel.class));
        assertThrows(IllegalStateException.class, () -> ShopE2eTestSupport.configurationForChannel(
                configured, ConflictingChannel.class.getAnnotation(
                        ShopE2eTestSupport.WithE2eHarness.class), ConflictingChannel.class));
        var disabledBrowser = configured.browser();
        var withoutNetwork = new atf.api.ui.config.UiBrowserConfiguration(
                disabledBrowser.baseUrl(), disabledBrowser.browser(), disabledBrowser.headless(),
                disabledBrowser.timeout(), disabledBrowser.navigationTimeout(),
                disabledBrowser.viewportWidth(), disabledBrowser.viewportHeight(),
                disabledBrowser.zoomPercent(), disabledBrowser.screenshotOnFailure(),
                disabledBrowser.retryPolicy(), UiNetworkInspectionConfiguration.disabled());
        var invalid = new atf.ui.practiceshop.config.ShopConfiguration(
                configured.baseUrl(), configured.accountFile(), configured.productName(),
                configured.productPath(), configured.checkout(), withoutNetwork,
                configured.uiRetry(), configured.validationProfile(), configured.api(),
                configured.uiReportDirectory(), configured.apiReportDirectory(),
                configured.authenticatedE2eEnabled(), configured.reporting());
        assertThrows(IllegalStateException.class,
                () -> ShopE2eTestSupport.configurationForChannel(invalid, null, CombinedChannel.class));
        assertThrows(IllegalStateException.class,
                () -> ShopE2eTestSupport.configurationForChannel(
                        configured.withValidationProfile(ShopValidationProfile.UI_ONLY), null,
                        CombinedChannel.class));
    }

    @Test
    void explicitlyEmptyYamlCheckpointBindingFailsUiApiChannelPreflight(@TempDir Path temporary)
            throws Exception {
        Path yaml = temporary.resolve("empty-api-evidence.yaml");
        Files.writeString(yaml, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  validation:
                    profile: ui-api
                    apiEvidence: []
                  networkInspector:
                    enabled: true
                    allowedHosts: [api.practicesoftwaretesting.com]
                """);
        var configuration = ShopConfigurationLoader.load(yaml);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ShopE2eTestSupport.configurationForChannel(
                        configuration, null, CombinedChannel.class));

        assertTrue(failure.getMessage().contains("requires selected API checkpoints"));
        assertTrue(configuration.api().evidenceBinding().checkpoints().isEmpty());
    }

    @Test
    void purchaseJourneysDeclareDistinctEvidenceProfiles() throws ClassNotFoundException {
        assertEquals(TestChannel.UI,
                Class.forName("atf.ui.tests.practiceshop.e2e.ShopUiOnlyPurchaseE2eTest")
                        .getAnnotation(AtfTestChannel.class).value());
        assertEquals(TestChannel.UI_API,
                Class.forName("atf.ui.tests.practiceshop.e2e.ShopPurchaseE2eTest")
                        .getAnnotation(AtfTestChannel.class).value());

        Class<?> singleFlowOwner = Class.forName(
                "atf.ui.tests.practiceshop.e2e.ShopUiOnlySingleFlowPurchaseE2eTest");
        var singleFlow = singleFlowOwner.getAnnotation(ShopE2eTestSupport.WithE2eHarness.class);
        var singleFlowEvidence = singleFlowOwner.getAnnotation(AtfUiEvidence.class);
        assertEquals(ShopE2eTestSupport.HarnessProfile.CONFIGURED, singleFlow.profile());
        assertEquals(TestChannel.UI,
                Class.forName("atf.ui.tests.practiceshop.e2e.ShopUiOnlySingleFlowPurchaseE2eTest")
                        .getAnnotation(AtfTestChannel.class).value());
        assertEquals(AtfUiEvidenceFlow.UNIFIED_FLOW, singleFlowEvidence.flow());
        var reportProfile = ShopE2eTestSupport.reportProfile(
                ShopConfigurationLoader.load(), singleFlow, singleFlowEvidence);
        assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, reportProfile.flowMode());
        assertEquals(ReportScreenshotPolicy.EACH_CHECKPOINT, reportProfile.screenshotPolicy());
        assertEquals(ReportEvidenceCaptureSource.BROWSER, reportProfile.captureSource());
        assertEquals(ReportUiActionDetailsMode.INCLUDED_COLLAPSED, reportProfile.actionDetailsMode());
        assertFalse(atf.ui.tests.practiceshop.support.lifecycle.ShopE2eContext.Scenario
                .shouldCaptureCheckpointScreenshot(reportProfile, true, true),
                "unified flow reserves its one screenshot for the final screen");
        assertTrue(atf.ui.tests.practiceshop.support.lifecycle.ShopE2eContext.Scenario
                .shouldCaptureCheckpointScreenshot(reportProfile, true, false),
                "failed checkpoints still retain failure screenshots");
        assertFalse(atf.ui.tests.practiceshop.support.lifecycle.ShopE2eContext.Scenario
                .shouldCaptureCheckpointScreenshot(reportProfile, false, false));

        var yamlConfiguration = ShopConfigurationLoader.load();
        var yamlDisabled = new atf.ui.practiceshop.config.ShopConfiguration(
                yamlConfiguration.baseUrl(), yamlConfiguration.accountFile(), yamlConfiguration.productName(),
                yamlConfiguration.productPath(), yamlConfiguration.checkout(), yamlConfiguration.browser(),
                yamlConfiguration.uiRetry(), yamlConfiguration.validationProfile(), yamlConfiguration.api(),
                yamlConfiguration.uiReportDirectory(), yamlConfiguration.apiReportDirectory(),
                yamlConfiguration.authenticatedE2eEnabled(), new ShopReportConfiguration(
                yamlConfiguration.reporting().systemBodyPreviewCharacters(), ReportScreenshotPolicy.NONE));
        assertEquals(ReportScreenshotPolicy.NONE,
                ShopE2eTestSupport.reportProfile(yamlDisabled, singleFlow).screenshotPolicy(),
                "class annotations may choose report flow but cannot override the YAML screenshot policy");

        var newAccountPurchaseClass = Class.forName(
                "atf.ui.tests.practiceshop.e2e.ShopNewAccountPurchaseE2eTest");
        var newAccountPurchaseHarness = newAccountPurchaseClass
                .getAnnotation(ShopE2eTestSupport.WithE2eHarness.class);
        assertEquals(ReportScreenshotPolicy.EACH_CHECKPOINT,
                ShopE2eTestSupport.reportProfile(ShopConfigurationLoader.load(),
                        newAccountPurchaseHarness).screenshotPolicy());

        var summaryOnly = ShopE2eTestSupport.reportProfile(
                ShopConfigurationLoader.load(), SummaryOnlyActions.class
                        .getAnnotation(ShopE2eTestSupport.WithE2eHarness.class));
        assertEquals(ReportUiActionDetailsMode.SUMMARY_ONLY, summaryOnly.actionDetailsMode());
        var included = ShopE2eTestSupport.reportProfile(
                ShopConfigurationLoader.load(), IncludedActions.class
                        .getAnnotation(ShopE2eTestSupport.WithE2eHarness.class));
        assertEquals(ReportUiActionDetailsMode.INCLUDED_COLLAPSED, included.actionDetailsMode());

        var authenticated = Class.forName(
                        "atf.ui.tests.practiceshop.e2e.ShopAuthenticatedPurchaseE2eTest")
                .getAnnotation(ShopTestSupport.AuthenticatedE2e.class);
        var authenticatedHarness = Class.forName(
                        "atf.ui.tests.practiceshop.e2e.ShopAuthenticatedPurchaseE2eTest")
                .getAnnotation(ShopE2eTestSupport.WithE2eHarness.class);
        assertEquals(ReportScreenshotPolicy.EACH_CHECKPOINT,
                ShopE2eTestSupport.reportProfile(ShopConfigurationLoader.load(),
                        authenticatedHarness).screenshotPolicy());
        assertEquals(ShopTestSupport.AccountSetupMode.REUSE_FIXTURE, authenticated.accountSetup());
        assertEquals("atf-test-practice-shop/src/test/resources/"
                        + "practice-software-testing-account.local.properties",
                authenticated.accountFile());
        assertEquals(ShopTestSupport.RegistrationProfileMode.LOCAL,
                authenticated.registrationProfile());

        var configured = ShopConfigurationLoader.load();
        var uiOnly = ShopE2eTestSupport.configurationForHarnessProfile(
                configured, ShopE2eTestSupport.HarnessProfile.UI_ONLY);
        var uiApi = ShopE2eTestSupport.configurationForHarnessProfile(
                configured.withValidationProfile(ShopValidationProfile.UI_ONLY),
                ShopE2eTestSupport.HarnessProfile.UI_API);

        assertEquals(ShopValidationProfile.UI_ONLY, uiOnly.validationProfile());
        assertTrue(uiOnly.api().evidenceBinding().checkpoints().isEmpty());
        assertEquals(ShopValidationProfile.UI_API, uiApi.validationProfile());
        assertTrue(uiApi.api().evidenceBinding().includes(ShopApiCheckpoint.PRODUCT));
        assertTrue(uiApi.api().evidenceBinding().includes(ShopApiCheckpoint.REGISTRATION));
        assertTrue(uiApi.api().evidenceBinding().includes(ShopApiCheckpoint.ORDER));
    }

    @Test
    void sharedEvidenceAnnotationOverridesOnlyItsDeclaredAxes() {
        var configured = ShopConfigurationLoader.load();
        var noScreenshots = ShopE2eTestSupport.reportProfile(configured, null,
                NoScreenshotsEvidence.class.getAnnotation(AtfUiEvidence.class));
        var configuredEvidence = ShopE2eTestSupport.reportProfile(configured, null,
                ConfiguredEvidence.class.getAnnotation(AtfUiEvidence.class));

        assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, noScreenshots.flowMode());
        assertEquals(ReportScreenshotPolicy.NONE, noScreenshots.screenshotPolicy());
        assertEquals(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS, configuredEvidence.flowMode());
        assertEquals(configured.reporting().screenshotPolicy(), configuredEvidence.screenshotPolicy());
    }

    @Test
    void suiteHarnessCompositionPreservesTheConfiguredScreenshotPolicy() {
        var yamlConfiguration = ShopConfigurationLoader.load();
        for (ReportScreenshotPolicy policy : ReportScreenshotPolicy.values()) {
            var configured = yamlConfiguration.withScreenshotPolicy(policy);
            for (ShopE2eTestSupport.HarnessProfile harnessProfile
                    : ShopE2eTestSupport.HarnessProfile.values()) {
                var composed = ShopE2eTestSupport.configurationForHarnessProfile(
                        configured, harnessProfile);
                assertEquals(policy, ShopE2eTestSupport.reportProfile(composed, null)
                                .screenshotPolicy(),
                        () -> harnessProfile + " must preserve YAML screenshotPolicy=" + policy);
            }
        }
    }

    @Test
    void suiteExecutionDeclaresItsScopeWithoutInspectingJUnitRootNames() {
        var parameters = java.util.Arrays.stream(ShopE2eSuite.class.getAnnotationsByType(
                        org.junit.platform.suite.api.ConfigurationParameter.class))
                .collect(java.util.stream.Collectors.toMap(
                        org.junit.platform.suite.api.ConfigurationParameter::key,
                        org.junit.platform.suite.api.ConfigurationParameter::value));
        assertEquals("suite", parameters.get(TestRunScope.CONFIGURATION_PARAMETER));
        assertEquals("org.junit.jupiter.api.ClassOrderer$OrderAnnotation",
                parameters.get("junit.jupiter.testclass.order.default"));
        assertEquals("false", parameters.get("junit.jupiter.execution.parallel.enabled"));
    }

    @Test
    void shopE2eHarnessOptsReportOwnersIntoThePublishedPath() throws ClassNotFoundException {
        Class<?> purchase = Class.forName(
                "atf.ui.tests.practiceshop.e2e.ShopPurchaseE2eTest");

        assertEquals(AtfReportPath.Policy.ENABLED,
                AtfReportPathPolicyResolver.resolve(purchase));
    }

    @Test
    void terminalPurchaseScreenRequiresTheObservedOrderAndInvoiceConfirmation() {
        var confirmed = ShopPurchaseConfirmation.assertions(
                new ShopPurchaseFlow.PurchaseResult(
                        "Pliers", "2", "Cash on Delivery", "Thanks for your order! INV-123"));
        var missingInvoice = ShopPurchaseConfirmation.assertions(
                new ShopPurchaseFlow.PurchaseResult(
                        "Pliers", "2", "Cash on Delivery", "Thanks for your order!"));
        var absentConfirmation = ShopPurchaseConfirmation.assertions(
                new ShopPurchaseFlow.PurchaseResult(
                        "Pliers", "2", "Cash on Delivery", null));

        assertEquals(1, confirmed.size());
        assertTrue(confirmed.getFirst().passed());
        assertFalse(missingInvoice.getFirst().passed());
        assertFalse(absentConfirmation.getFirst().passed());
        assertEquals("no confirmation was observed", absentConfirmation.getFirst().actual());
    }

    @ShopE2eTestSupport.WithE2eHarness(
            actionDetails = ShopE2eTestSupport.ActionDetailsMode.SUMMARY_ONLY)
    private static class SummaryOnlyActions { }

    @ShopE2eTestSupport.WithE2eHarness(
            actionDetails = ShopE2eTestSupport.ActionDetailsMode.INCLUDED_COLLAPSED)
    private static class IncludedActions { }

    @AtfUiEvidence(flow = AtfUiEvidenceFlow.UNIFIED_FLOW,
            screenshots = AtfUiScreenshotPolicy.NONE)
    private static class NoScreenshotsEvidence { }

    @AtfUiEvidence
    private static class ConfiguredEvidence { }

    @Test
    void classRunRegistrationValueCreatesOnceAndReturnsTheSameCredentials() {
        var account = new ScopedTestValue<ShopCredentials>();
        var first = new ShopCredentials("live@practice-shop.invalid-domain", "secret", "ATF", "One");
        var second = new ShopCredentials("live-two@practice-shop.invalid-domain", "secret", "ATF", "Two");

        assertTrue(account.current().isEmpty());
        assertSame(first, account.getOrCreate(() -> first).value());
        assertSame(first, account.getOrCreate(() -> second).value());
        assertSame(first, account.current().orElseThrow());
        account.close();
        assertTrue(account.current().isEmpty());
    }

    @Test
    void uiOnlyRegistrationStillReportsTheObservedBrowserResponseStatus() {
        var success = ShopAuthenticatedAccountTestSupport.registrationAssertions(201, java.util.List.of());
        var failure = ShopAuthenticatedAccountTestSupport.registrationAssertions(409, java.util.List.of());

        assertEquals("REGISTER_PER_RUN", success.get(0).actual());
        assertEquals("HTTP 201", success.get(1).actual());
        assertTrue(success.get(1).passed());
        assertTrue(success.get(2).passed());
        assertEquals("HTTP 409", failure.get(1).actual());
        assertFalse(failure.get(1).passed());
        assertFalse(failure.get(2).passed());

        var inconsistent = ShopAuthenticatedAccountTestSupport.registrationAssertions(201,
                java.util.List.of(new atf.api.reporting.contract.AtfAssertion(
                        "Registration journal", "registration confirmed", "no matching exchange", false)));
        assertTrue(inconsistent.get(1).passed());
        assertFalse(inconsistent.get(2).passed());
    }
}
