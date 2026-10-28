package atf.shop.tests.support.lifecycle;

import atf.shop.tests.support.ShopComponentTest;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopValidationProfile;
import atf.shop.tests.support.reporting.ShopReportSystemMetadata;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ReportEnvironmentField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.shop.tests.support.lifecycle.ShopE2eContext.startReportScenario;

@ShopComponentTest
class ShopE2eContextEvidenceProfileTest {
    @TempDir
    Path temporaryReports;

    @Test
    void mixedSuiteMetadataAndSiblingCaptureProfilesStayFaithfulOnSuccessAndFailure() throws Exception {
        var source = ShopConfigurationLoader.load();
        var apiConfiguration = source.withValidationProfile(ShopValidationProfile.UI_API);
        var uiConfiguration = source.withValidationProfile(ShopValidationProfile.UI_ONLY);
        var apiProfile = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT).build();
        var uiProfile = ReportEvidenceProfile.builder().flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.NONE)
                .actionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY).build();
        Path output = Path.of(System.getProperty("atf.test.reportDirectory", temporaryReports.toString()))
                .toAbsolutePath().resolve("ui").resolve("consumer-contracts");
        var isolated = new ShopConfiguration(source.baseUrl(), source.accountFile(), source.productName(),
                source.productPath(), source.checkout(), source.browser(), source.uiRetry(), source.validationProfile(),
                source.api(), output, output, false, source.reporting());
        Path reportPath;
        try (var report = ShopE2eContext.openReport(isolated, apiProfile)) {
            reportPath = report.reportPath();
            report.includeScenarioEnvironment(ShopReportSystemMetadata.snapshot(apiConfiguration, apiProfile, "COMPONENT"));
            var positive = startReportScenario(report, apiProfile, "SharedProfilePositive", "configured API profile succeeds");
            var phase = positive.startPhase("TEST BODY");
            phase.step(AtfGherkinKeyword.THEN, "the observed order is confirmed", "/checkout", "",
                    List.of(new AtfAssertion("Order", "confirmed", "confirmed", true)), AtfReportStatus.PASS);
            phase.completeFinal(AtfReportStatus.PASS);
            positive.complete(AtfReportStatus.PASS);
            report.includeScenarioEnvironment(ShopReportSystemMetadata.snapshot(uiConfiguration, uiProfile, "COMPONENT"));
            var negative = startReportScenario(report, uiProfile, "SharedProfileNegative", "a missing order remains a failure");
            var failedPhase = negative.startPhase("TEST BODY");
            failedPhase.step(AtfGherkinKeyword.THEN, "the order must be confirmed", "/checkout", "",
                    List.of(new AtfAssertion("Order", "confirmed", "missing", false)), AtfReportStatus.FAIL);
            failedPhase.completeFinal(AtfReportStatus.FAIL);
            negative.complete(AtfReportStatus.FAIL);
            assertEquals(ReportScreenshotPolicy.EACH_CHECKPOINT, report.evidenceProfile().screenshotPolicy());
        }
        String html = Files.readString(reportPath).replaceAll(
                "(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertTrue(html.contains(ReportEnvironmentSnapshot.MIXED_VALUE));
        assertTrue(html.contains(ReportEnvironmentField.SCREENSHOT_POLICY.label()));
        assertTrue(html.contains(ReportEnvironmentField.UI_ACTION_DETAILS.label()));
        for (var field : List.of(ReportEnvironmentField.PROFILE, ReportEnvironmentField.SCREENSHOT_POLICY,
                ReportEnvironmentField.UI_ACTION_DETAILS, ReportEnvironmentField.ENVIRONMENT,
                ReportEnvironmentField.API_ENDPOINT)) {
            assertEquals(1, Pattern.compile("<td[^>]*>\\s*" + Pattern.quote(field.label()) + "\\s*</td>")
                    .matcher(html).results().count(), "System must contain one current row: " + field);
        }
        assertTrue(html.contains("SharedProfilePositive"));
        assertTrue(html.contains("SharedProfileNegative"));
        assertTrue(html.contains("missing"));
        assertTrue(html.contains("data-atf-evidence-status=\"fail\""));
    }

    @Test
    void reportCompositionHonorsExplicitCapturePolicyOverYamlDefault() {
        ShopConfiguration yaml = ShopConfigurationLoader.load();
        ShopConfiguration isolated = new ShopConfiguration(
                yaml.baseUrl(), yaml.accountFile(), yaml.productName(), yaml.productPath(),
                yaml.checkout(), yaml.browser(), yaml.uiRetry(), yaml.validationProfile(), yaml.api(),
                temporaryReports, temporaryReports, yaml.authenticatedE2eEnabled(),
                yaml.reporting());
        ReportEvidenceProfile explicit = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.NONE)
                .build();

        try (var report = ShopE2eContext.openReport(isolated, explicit)) {
            assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, report.evidenceProfile().flowMode());
            assertEquals(ReportScreenshotPolicy.NONE,
                    report.evidenceProfile().screenshotPolicy());
        }
    }

    @Test
    void shopScenarioAggregatesSetupAndBusinessPhaseOnlyForUnifiedProfile() throws Exception {
        ShopConfiguration yaml = ShopConfigurationLoader.load();
        ShopConfiguration isolated = new ShopConfiguration(
                yaml.baseUrl(), yaml.accountFile(), yaml.productName(), yaml.productPath(),
                yaml.checkout(), yaml.browser(), yaml.uiRetry(), yaml.validationProfile(), yaml.api(),
                temporaryReports, temporaryReports, yaml.authenticatedE2eEnabled(), yaml.reporting());
        var profile = ReportEvidenceProfile.builder()
                .flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.NONE).build();
        Path path;
        try (var report = ShopE2eContext.openReport(isolated, profile)) {
            path = report.reportPath();
            var scenario = ShopE2eContext.startReportScenario(report, profile,
                    "ShopUnifiedContract", "authenticated checkout");
            var setup = scenario.startPhase("SETUP");
            setup.setup(AtfGherkinKeyword.GIVEN, "account is ready", "/login",
                    List.of(), AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase("TEST BODY");
            body.step(AtfGherkinKeyword.THEN, "order is confirmed", "/checkout", "",
                    List.of(new AtfAssertion("Order", "confirmed", "confirmed", true)),
                    AtfReportStatus.PASS);
            body.completeFinal(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(path);
        assertEquals(1, html.split("<td>EVIDENCE TYPE</td><td>unified-ui-flow</td>", -1).length - 1);
        assertTrue(html.contains("FLOW CHECKPOINT 1 PHASE"));
        assertTrue(html.contains("FLOW CHECKPOINT 2 PHASE"));
        int setupPhase = html.indexOf("<td>FLOW CHECKPOINT 1 PHASE</td><td>SETUP</td>");
        int businessPhase = html.indexOf("<td>FLOW CHECKPOINT 2 PHASE</td><td>TEST BODY</td>");
        assertTrue(setupPhase >= 0 && businessPhase > setupPhase,
                "unified report must preserve setup before the business phase");
    }

}
