package atf.shop.tests.support.lifecycle;

import atf.testkit.AtfComponentTest;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopE2eContextEvidenceProfileTest {
    @TempDir
    Path temporaryReports;

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
