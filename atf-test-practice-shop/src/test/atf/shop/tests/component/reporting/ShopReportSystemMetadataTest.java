package atf.shop.tests.component.reporting;

import atf.reporting.configuration.ReportingSettings;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.reporting.environment.ShopReportSystemMetadata;
import atf.shop.config.ShopReportConfiguration;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.UiExtentReportOptions;

import static atf.testkit.assertions.AuditedAssertions.assertAll;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Verifies that the Practice Shop report exposes useful configuration without secrets or temp paths. */
@ShopComponentTest
class ShopReportSystemMetadataTest {
    @Test
    void nativeGeometryIsPublishedInTheSystemTable() throws Exception {
        var configuration = ShopConfigurationLoader.load();
        var environment = ShopReportSystemMetadata.snapshot(configuration);
        var directory = Files.createDirectories(Path.of("target", "system-metadata-proof", UUID.randomUUID().toString()));
        Path reportPath;
        try (var report = UiExtentReportSession.openConfigured(
                UiExtentReportOptions.builder(directory).fileName("shop-system.html")
                        .console(AtfConsoleSink.noOp()).environment(environment).build())) {
            reportPath = report.reportPath();
            var scenario = report.startScenario("ShopGeometryMetadataFixture", getClass().getPackageName(),
                    "configured geometry projection only", List.of("component"));
            scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(reportPath);
        assertTrue(html.contains(ReportEnvironmentField.VIEWPORT.label()), "System publishes the native geometry row");
        assertTrue(html.contains("1440 × 1000 requested output · 2057 × 1429 CSS · DPR 0.70"),
                "System separates requested output, CSS layout and DPR");
    }

    @Test
    void publishesTypedConsumerConfigurationAsSafeOverviewMetadata() {
        Map<String, String> metadata = ShopReportSystemMetadata.from(
                ShopConfigurationLoader.load());
        String values = metadata.toString();

        assertAll(
                () -> assertTrue(values.contains("chromium")),
                () -> assertTrue(values.contains("headed")),
                () -> assertTrue(values.contains("Client=Playwright browser")),
                () -> assertTrue(metadata.get(ReportEnvironmentField.BROWSER.label())
                        .contains(" · headed · viewport 1440 × 1000"),
                        "System/Browser must reflect the current headed YAML profile and viewport"),
                () -> assertTrue(values.contains("zoom 70%")),
                () -> assertTrue(values.contains("UI + API evidence")),
                () -> assertTrue(values.contains("Transport mode=LIVE HTTP")),
                () -> assertTrue(values.contains("Precondition=API request precondition: authenticated")),
                () -> assertFalse(values.contains("UI journey precondition")),
                () -> assertTrue(values.contains("UI action timeout=20s")),
                () -> assertTrue(values.contains("Page navigation timeout=45s")),
                () -> assertTrue(values.contains("API request timeout=20s")),
                () -> assertTrue(values.contains("Network inspector")),
                () -> assertTrue(values.contains("Body preview")),
                () -> assertTrue(values.contains("Body preview=100 characters")),
                () -> assertFalse(values.contains("PT20S")),
                () -> assertFalse(values.contains("PT45S")),
                () -> assertFalse(values.contains("Flow=")),
                () -> assertFalse(values.contains("Suite=")),
                () -> assertFalse(values.contains("Fixture=")),
                () -> assertFalse(values.contains("Test package")),
                () -> assertFalse(values.toLowerCase().contains("password")),
                () -> assertFalse(values.contains(".tmp")),
                () -> assertFalse(values.contains("practice.shop@example.test")));
    }

    @Test
    void exposesTheSameConsumerConfigurationThroughTheReportingAllowlist() {
        var snapshot = ShopReportSystemMetadata.snapshot(
                ShopConfigurationLoader.load(),
                ReportingSettings.defaults());

        assertAll(
                () -> assertTrue(snapshot.values().containsKey(ReportEnvironmentField.PROFILE)),
                () -> assertTrue(snapshot.values().containsKey(ReportEnvironmentField.UI_HOST)),
                () -> assertTrue(snapshot.values().containsKey(ReportEnvironmentField.API_ENDPOINT)),
                () -> assertTrue(snapshot.values().containsKey(ReportEnvironmentField.UI_JOURNEY_PRECONDITION)),
                () -> assertFalse(snapshot.systemDisplayValues().containsKey(
                        ReportEnvironmentField.UI_JOURNEY_PRECONDITION.label())),
                () -> assertTrue(snapshot.values().containsKey(ReportEnvironmentField.BODY_PREVIEW)),
                () -> assertTrue(snapshot.values().containsKey(ReportEnvironmentField.SCREENSHOT_POLICY)),
                () -> assertEquals("Each checkpoint",
                        snapshot.systemDisplayValues().get(ReportEnvironmentField.SCREENSHOT_POLICY.label())),
                () -> assertEquals("Checkpoint-by-checkpoint",
                        snapshot.systemDisplayValues().get(ReportEnvironmentField.UI_EVIDENCE_LAYOUT.label())),
                () -> assertEquals("20s", snapshot.displayValues().get(ReportEnvironmentField.UI_TIMEOUT.label())),
                () -> assertEquals("45s", snapshot.displayValues().get(ReportEnvironmentField.NAVIGATION_TIMEOUT.label())),
                () -> assertEquals("20s", snapshot.displayValues().get(ReportEnvironmentField.API_TIMEOUT.label())),
                () -> assertEquals("guest checkout configuration ready",
                        snapshot.displayValues().get(ReportEnvironmentField.UI_JOURNEY_PRECONDITION.label())),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.FLOW)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.FIXTURE)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.SUITE)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.TEST_PACKAGE)),
                () -> assertFalse(snapshot.displayValues().toString().contains("password")),
                () -> assertFalse(snapshot.displayValues().toString().contains(".tmp")));
    }

    @Test
    void overviewReflectsTheResolvedPresentationOverridesInsteadOfYamlDefaults() {
        var configuration = ShopConfigurationLoader.load();
        var reportProfile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY)
                .captureSource(ReportEvidenceCaptureSource.BROWSER)
                .build();

        var system = ShopReportSystemMetadata.snapshot(configuration,
                ReportingSettings.defaults(), reportProfile)
                .systemDisplayValues();

        assertAll(
                () -> assertEquals("One consolidated flow",
                        system.get(ReportEnvironmentField.UI_EVIDENCE_LAYOUT.label())),
                () -> assertEquals("Final screenshot only",
                        system.get(ReportEnvironmentField.SCREENSHOT_POLICY.label())),
                () -> assertFalse(system.containsKey(ReportEnvironmentField.UI_JOURNEY_PRECONDITION.label())),
                () -> assertFalse(system.containsKey(ReportEnvironmentField.SUITE.label())),
                () -> assertFalse(system.containsKey(ReportEnvironmentField.TEST_PACKAGE.label())));
    }

    @Test
    void uiOnlyOverviewOmitsInactiveApiConfiguration() {
        var configuration = ShopConfigurationLoader.load()
                .withValidationProfile(atf.shop.config.ShopValidationProfile.UI_ONLY);

        var snapshot = ShopReportSystemMetadata.snapshot(configuration,
                ReportingSettings.defaults());

        assertAll(
                () -> assertEquals("UI evidence",
                        snapshot.systemDisplayValues().get(ReportEnvironmentField.PROFILE.label())),
                () -> assertEquals("Playwright browser",
                        snapshot.systemDisplayValues().get(ReportEnvironmentField.CLIENT.label())),
                () -> assertEquals("External live website",
                        snapshot.systemDisplayValues().get(ReportEnvironmentField.SOURCE_BUILD.label())),
                () -> assertTrue(snapshot.systemDisplayValues().containsKey(
                        ReportEnvironmentField.UI_EVIDENCE_LAYOUT.label())),
                () -> assertTrue(snapshot.systemDisplayValues().containsKey(
                        ReportEnvironmentField.SCREENSHOT_POLICY.label())),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.API_ENDPOINT)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.TRANSPORT_MODE)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.PRECONDITION)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.API_TIMEOUT)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.API_EVIDENCE)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.NETWORK_INSPECTOR)),
                () -> assertFalse(snapshot.values().containsKey(ReportEnvironmentField.BODY_PREVIEW)));
    }

    @Test
    void keepsSystemPreviewIndependentFromRequestResponseJsonLimit() {
        var base = ShopConfigurationLoader.load();
        var configured = new atf.shop.config.ShopConfiguration(
                base.baseUrl(), base.accountFile(), base.productName(), base.productPath(),
                base.checkout(), base.browser(), base.uiRetry(), base.validationProfile(), base.api(),
                base.uiReportDirectory(), base.apiReportDirectory(), base.authenticatedE2eEnabled(),
                new ShopReportConfiguration(64));

        var values = ShopReportSystemMetadata.snapshot(configured,
                new ReportingSettings(240, 12_000)).displayValues();

        assertEquals("64 characters · full JSON remains in checkpoint evidence",
                values.get(ReportEnvironmentField.BODY_PREVIEW.label()));
    }
}
