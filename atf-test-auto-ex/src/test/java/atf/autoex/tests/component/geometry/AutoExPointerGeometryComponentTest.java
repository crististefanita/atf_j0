package atf.autoex.tests.component.geometry;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.geometry.AutoExPointerGeometryFixture;
import atf.autoex.tests.support.geometry.AutoExPointerGeometryFixture.PointerRefused;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.reporting.AutoExReportSystemMetadata;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import static atf.testkit.assertions.AuditedAssertions.*;

/** AutoEx configuration and report-adapter proof; neutral HTTP geometry is owned by atf-ui. */
@AutoExComponentTest
class AutoExPointerGeometryComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"same", "opaque"})
    void configuredGeometryKeepsNativeFrameActivationAndOutputAuditable(String frame) throws Exception {
        var configuration = AutoExConfigurationLoader.load();
        var metadata = AutoExReportSystemMetadata.snapshot(configuration, ReportEvidenceProfile.defaults(), false, "LOCAL");
        try (var fixture = new AutoExPointerGeometryFixture(configuration.browser())) {
            fixture.activate(frame);
            assertEquals("1", fixture.clicks(frame), "The actual child receives exactly one activation");
            assertEquals("true", fixture.trusted(frame), "The child receives trusted browser pointer input");
            assertEquals(1, fixture.records().size(), "The geometry fix introduces no second activation or replay");
            assertEquals(70, configuration.browser().zoomPercent(), "The consumer YAML default remains seventy percent");
            assertEquals(2057, fixture.layoutWidth(), "The context has the effective CSS layout width");
            assertEquals(1429, fixture.layoutHeight(), "The context has the effective CSS layout height");
            assertEquals(.7, fixture.density(), .000001);
            assertEquals("", fixture.rootZoom(), "No root CSS zoom is injected");
            assertEquals("1440 × 1000 requested output · 2057 × 1429 CSS · DPR 0.70",
                    metadata.values().get(ReportEnvironmentField.VIEWPORT), "System separates output intent from effective layout");
            byte[] png = fixture.screenshot();
            var image = ImageIO.read(new ByteArrayInputStream(png));
            assertTrue(Math.abs(image.getWidth() - 1440) <= 1, "Native capture width retains output intent within pixel rounding");
            assertTrue(Math.abs(image.getHeight() - 1000) <= 1, "Native capture height retains output intent within pixel rounding");
            Path output = Files.createDirectories(Path.of("target", "geometry-proof"));
            Files.write(output.resolve(frame + "-" + UUID.randomUUID() + ".png"), png);
        }
    }

    @Test
    void anOverlayCannotBeBypassedToMakeGeometryPass() {
        try (var fixture = new AutoExPointerGeometryFixture(AutoExConfigurationLoader.load().browser())) {
            fixture.cover();
            var refused = assertThrows(PointerRefused.class, () -> fixture.activate("opaque"),
                    "The normal actionability check still refuses an intercepted pointer");
            assertTrue(refused.getCause().getMessage().contains("intercepts pointer events"),
                    "The rejection retains the actual native interception diagnostic");
            assertEquals("0", fixture.clicks("opaque"), "A refused pointer has no child side effect");
            assertEquals(1, fixture.records().size(), "A refusal creates no successful substitute or replay");
            assertEquals("FAIL", fixture.records().getFirst().status().name(), "The attempted action remains an auditable failure");
        }
    }
}
