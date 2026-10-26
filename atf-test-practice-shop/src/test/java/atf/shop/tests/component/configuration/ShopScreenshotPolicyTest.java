package atf.shop.tests.component.configuration;

import atf.api.testkit.AtfComponentTest;
import atf.api.reporting.evidence.ReportScreenshotPolicy;
import atf.api.reporting.evidence.profile.ReportEvidenceCaptureSource;
import atf.api.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.reporting.ShopScreenshotPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Component contract for the Practice Shop checkpoint screenshot policy. */
@AtfComponentTest
class ShopScreenshotPolicyTest {
    @Test
    void usesTheConfiguredPolicyWithoutChangingTheGenericEvidenceShape() {
        var allCheckpoints = ShopScreenshotPolicy.profile(ReportScreenshotPolicy.EACH_CHECKPOINT);
        var disabled = ShopScreenshotPolicy.profile(ReportScreenshotPolicy.NONE);

        assertEquals(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS, allCheckpoints.flowMode());
        assertEquals(ReportEvidenceCaptureSource.BROWSER, disabled.captureSource());
        assertEquals(ReportScreenshotPolicy.EACH_CHECKPOINT, allCheckpoints.screenshotPolicy());
        assertEquals(ReportScreenshotPolicy.NONE, disabled.screenshotPolicy());
    }

    @Test
    void yamlCanExplicitlyProhibitScreenshotsEvenWhenTheLegacySwitchAllowsThem(@TempDir Path temp)
            throws Exception {
        Path yaml = temp.resolve("practice-shop.yaml");
        Files.writeString(yaml, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  screenshotOnFailure: true
                  reporting:
                    screenshotPolicy: none
                """);

        var configuration = ShopConfigurationLoader.load(yaml);

        assertEquals(ReportScreenshotPolicy.NONE, configuration.reporting().screenshotPolicy());
    }

    @Test
    void legacyYamlCanStillRestrictCapturesWhenNoTypedPolicyIsPresent(@TempDir Path temp)
            throws Exception {
        Path yaml = temp.resolve("practice-shop.yaml");
        Files.writeString(yaml, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  screenshotOnFailure: false
                """);

        var configuration = ShopConfigurationLoader.load(yaml);

        assertEquals(ReportScreenshotPolicy.FAILURE_ONLY, configuration.reporting().screenshotPolicy());
    }
}
