package atf.shop.tests.component.geometry;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.reporting.environment.ShopReportSystemMetadata;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.playwright.UiScreenValidation;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;

import static atf.testkit.assertions.AuditedAssertions.*;

/** Consumer wiring proof; real cross-origin/nested HTTP geometry belongs to atf-ui. */
@ShopComponentTest
class ShopPointerGeometryComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"same", "opaque"})
    void configuredGeometryKeepsNativeFrameActivationAndOutputAuditable(String frame) throws Exception {
        var configuration = ShopConfigurationLoader.load();
        var browser = configuration.browser();
        var metadata = ShopReportSystemMetadata.snapshot(configuration, ReportEvidenceProfile.defaults(), "LOCAL");
        try (var session = openHeadless(browser)) {
            var fixture = new FrameFixture(session.page());
            var actions = session.newActionSequence();
            actions.click(fixture.activate(frame), "one native activation");
            assertEquals("1", fixture.value(frame, "#clicks"), "The actual child receives exactly one activation");
            assertEquals("true", fixture.value(frame, "#trusted"), "The child receives trusted browser pointer input");
            assertEquals(1, actions.snapshot().size(), "The geometry fix introduces no second activation or replay");
            assertEquals(70, browser.zoomPercent(), "The consumer YAML default remains seventy percent");
            assertEquals(2057, session.page().viewportSize().width, "The context has the effective CSS layout width");
            assertEquals(1429, session.page().viewportSize().height, "The context has the effective CSS layout height");
            assertEquals(.7, ((Number) session.page().evaluate("devicePixelRatio")).doubleValue(), .000001);
            assertEquals("", session.page().evaluate("document.documentElement.style.zoom"), "No root CSS zoom is injected");
            assertEquals("1440 × 1000 requested output · 2057 × 1429 CSS · DPR 0.70",
                    metadata.values().get(ReportEnvironmentField.VIEWPORT), "System separates output intent from effective layout");
            var evidence = UiScreenValidation.capture(session.page(), UiScreenValidationConfiguration.defaults(), "native frame geometry");
            byte[] png = Base64.getDecoder().decode(evidence.base64Png());
            var image = ImageIO.read(new ByteArrayInputStream(png));
            assertTrue(Math.abs(image.getWidth() - 1440) <= 1, "Native capture width retains output intent within pixel rounding");
            assertTrue(Math.abs(image.getHeight() - 1000) <= 1, "Native capture height retains output intent within pixel rounding");
            Path output = Files.createDirectories(Path.of("target", "geometry-proof"));
            Files.write(output.resolve(frame + "-" + UUID.randomUUID() + ".png"), png);
        }
    }

    @Test
    void anOverlayCannotBeBypassedToMakeGeometryPass() {
        try (var session = openHeadless(ShopConfigurationLoader.load().browser())) {
            var fixture = new FrameFixture(session.page());
            fixture.cover();
            var actions = session.newActionSequence();
            assertThrows(PlaywrightException.class, () -> actions.click(
                    fixture.activate("opaque"), "blocked native activation"),
                    "The normal actionability check still refuses an intercepted pointer");
            assertEquals("0", fixture.value("opaque", "#clicks"), "A refused pointer has no child side effect");
            assertEquals(1, actions.snapshot().size(), "A refusal neither retries activation nor creates a successful substitute");
            assertEquals("FAIL", actions.snapshot().getFirst().status().name(), "The attempted browser action remains an auditable failure");
        }
    }

    private static PlaywrightUiSession openHeadless(UiBrowserConfiguration configured) {
        return PlaywrightUiSession.open(new UiBrowserConfiguration(configured.baseUrl(), configured.browser(), true,
                Duration.ofMillis(850), Duration.ofSeconds(5), configured.viewportWidth(), configured.viewportHeight(),
                configured.zoomPercent(), false));
    }

    /** Local fixture selectors stay outside scenario assertions; no production support or business journey is simulated. */
    private record FrameFixture(Page page) {
        FrameFixture {
            String child = """
                    <button id="activate" style="margin:20px;width:130px;height:45px"
                    onclick="document.getElementById('clicks').textContent=Number(document.getElementById('clicks').textContent)+1;
                    document.getElementById('trusted').textContent=event.isTrusted">Native target</button>
                    <output id="clicks">0</output><output id="trusted"></output>
                    """;
            String encoded = child.replace("&", "&amp;").replace("\"", "&quot;");
            page.setContent("<iframe id='same' srcdoc=\"" + encoded + "\"></iframe>"
                    + "<iframe id='opaque' sandbox='allow-scripts' srcdoc=\"" + encoded + "\"></iframe>"
                    + "<div id='cover' style='display:none;position:fixed;inset:0;background:white;z-index:10'></div>");
        }
        AtfLocator activate(String frame) {
            return new AtfLocator(page.frameLocator("#" + frame).locator("#activate"), "#" + frame + " >> #activate");
        }
        String value(String frame, String selector) { return page.frameLocator("#" + frame).locator(selector).innerText(); }
        void cover() { page.evaluate("document.getElementById('cover').style.display='block'"); }
    }
}

