package atf.shop.tests.support.lifecycle;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopE2eFailureReportComponentTest {
    private static final String REPORT_FILE = "shop-e2e-setup-failure-component.html";
    private static final String FIXTURE_SECRET = "private-password-value";
    private static final String LOCATOR = "[data-test='login-submit']";

    @Test
    void accountSetupAdapterKeepsItsPhaseBusinessLabelAndSensitiveDataBoundary() throws Exception {
        Path outputDirectory = Files.createTempDirectory("shop-e2e-failure-report-");
        Path reportPath;
        try (var report = UiExtentReportSession.openConfigured(
                UiExtentReportOptions.builder(outputDirectory)
                        .fileName(REPORT_FILE)
                        .console(AtfConsoleSink.noOp())
                        .build())) {
            reportPath = report.reportPath();
            var scenario = report.startScenario("ShopAuthenticatedPurchaseE2eTest",
                    "atf.shop.tests.e2e", "authenticated purchase setup", List.of("ui", "e2e"));
            var setup = scenario.startPhase("SETUP");

            ShopE2eContext.publishSetupFailureEvidence(setup, "https://shop.example/auth/login",
                    List.of(new AtfUiActionRecord(1, "Wait for visible", LOCATOR,
                            "state=Visible", "TimeoutError: Timeout 20000ms exceeded", 20_000,
                            AtfReportStatus.FAIL)),
                    new TimeoutError("Timeout 20000ms exceeded. waiting for locator(\""
                            + LOCATOR + "\"); password=" + FIXTURE_SECRET), "");

            setup.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(reportPath);
        assertTrue(html.contains("SETUP"), "the failure remains attached to the setup phase");
        assertTrue(html.contains("authenticated account setup failure"));
        assertFalse(html.contains(FIXTURE_SECRET), "raw credential text never reaches the report");
        assertTrue(html.contains("FAIL"), "the report keeps the actual failed outcome");
    }

    private static final class TimeoutError extends RuntimeException {
        private TimeoutError(String message) { super(message); }
    }
}
