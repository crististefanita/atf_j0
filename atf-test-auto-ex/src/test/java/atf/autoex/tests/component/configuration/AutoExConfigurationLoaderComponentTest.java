package atf.autoex.tests.component.configuration;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.ui.config.UiRetryOperation;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.ui.network.UiNetworkCapturePolicy;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static atf.autoex.config.AutoExSite.PUBLIC_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the AutoEx YAML contract and path override without starting a browser or public-site call. */
@AutoExComponentTest
class AutoExConfigurationLoaderComponentTest {
    private static final URI OVERRIDE_BASE_URI = URI.create("https://autoex-configuration.invalid");

    @TempDir
    Path temporaryDirectory;

    @Test
    void evidenceAxesAreConfiguredIndependentlyAndUnknownValuesFailBeforeResources() throws IOException {
        String original = Files.readString(defaultYaml());
        String evidence = "uiBaseDirectory: artifacts/reports/ui\n"
                + "    evidence:\n      flowMode: unified-flow\n      screenshotPolicy: none\n"
                + "      actionDetailsMode: summary-only";
        Path yaml = temporaryDirectory.resolve("evidence.yaml");
        Files.writeString(yaml, original.replace("uiBaseDirectory: artifacts/reports/ui", evidence));
        var profile = AutoExConfigurationLoader.load(yaml).evidence().profile(ReportScreenshotPolicy.EACH_CHECKPOINT);
        assertEquals(ReportEvidenceFlowMode.UNIFIED_FLOW, profile.flowMode());
        assertEquals(ReportScreenshotPolicy.NONE, profile.screenshotPolicy());
        assertEquals(ReportUiActionDetailsMode.SUMMARY_ONLY, profile.actionDetailsMode());
        var inherited = AutoExConfigurationLoader.load().evidence().profile(ReportScreenshotPolicy.FINAL_AND_FAILURE);
        assertEquals(ReportScreenshotPolicy.FINAL_AND_FAILURE, inherited.screenshotPolicy());
        for (String invalid : new String[]{"flowMode: unknown", "actionDetailsMode: unknown", "typo: none"}) {
            Files.writeString(yaml, original.replace("uiBaseDirectory: artifacts/reports/ui",
                    "uiBaseDirectory: artifacts/reports/ui\n    evidence:\n      " + invalid));
            assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(yaml));
        }
    }

    @Test
    void packagedTestYamlDefinesTheSafeAndCompleteDefaultProfile() {
        AutoExConfiguration configuration = AutoExConfigurationLoader.load();

        assertAll(
                () -> assertEquals(PUBLIC_BASE_URI, configuration.baseUrl()),
                () -> assertFalse(configuration.e2eEnabled(), "live E2E must remain opt-in by default"),
                () -> assertEquals("chromium", configuration.browser().browser()),
                () -> assertEquals(20, configuration.browser().timeout().toSeconds()),
                () -> assertEquals(45, configuration.browser().navigationTimeout().toSeconds()),
                () -> assertEquals(1440, configuration.browser().viewportWidth()),
                () -> assertEquals(1000, configuration.browser().viewportHeight()),
                () -> assertEquals(70, configuration.browser().zoomPercent(),
                        "AutoEx uses 70%; only the vignette Close opts into DOM activation"),
                () -> assertEquals(Path.of("artifacts/reports/ui"), configuration.uiReportBaseDirectory()),
                () -> assertEquals("Blue Top", configuration.catalog().productName()),
                () -> assertEquals(2, configuration.catalog().quantity()),
                () -> assertEquals("ATF AutoEx", configuration.registration().displayName()),
                () -> assertEquals("Rs. 1000", configuration.expectedCartTotal()));
    }

    @Test
    void explicitYamlPathOverridesPackagedDefaults() throws IOException {
        String yaml = Files.readString(defaultYaml())
                .replace(PUBLIC_BASE_URI.toString(), OVERRIDE_BASE_URI.toString())
                .replace("e2eEnabled: false", "e2eEnabled: true")
                .replace("zoomPercent: 70", "zoomPercent: 100")
                .replace("searchTerm: Blue Top", "searchTerm: Green Top")
                .replace("productName: Blue Top", "productName: Green Top");
        Path override = temporaryDirectory.resolve("autoex-local.yaml");
        Files.writeString(override, yaml);

        AutoExConfiguration configuration = AutoExConfigurationLoader.load(override);

        assertTrue(configuration.e2eEnabled());
        assertEquals(100, configuration.browser().zoomPercent(),
                "an explicit browser scale override is not silently coerced to the default");
        assertEquals(OVERRIDE_BASE_URI, configuration.baseUrl());
        assertEquals(OVERRIDE_BASE_URI, configuration.browser().baseUrl(),
                "the public site constant must not override the configured browser endpoint");
        assertEquals("Green Top", configuration.catalog().searchTerm());
        assertEquals("Green Top", configuration.catalog().productName());
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
    void configuredSystemPropertySelectsTheExternalYamlForDirectIdeRuns() throws IOException {
        String previous = System.getProperty(AutoExConfigurationLoader.CONFIG_PROPERTY);
        try {
            String yaml = Files.readString(defaultYaml()).replace("quantity: 2", "quantity: 3");
            Path override = temporaryDirectory.resolve("ide-profile.yaml");
            Files.writeString(override, yaml);
            System.setProperty(AutoExConfigurationLoader.CONFIG_PROPERTY, override.toString());

            assertEquals(3, AutoExConfigurationLoader.load().catalog().quantity());
        } finally {
            if (previous == null) System.clearProperty(AutoExConfigurationLoader.CONFIG_PROPERTY);
            else System.setProperty(AutoExConfigurationLoader.CONFIG_PROPERTY, previous);
        }
    }

    @Test
    void rejectsInvalidBaseUrlAndEscapingReportDirectory() throws IOException {
        String configuredBaseUrl = "baseUrl: " + PUBLIC_BASE_URI;
        String invalidBaseUrl = configuredBaseUrl.replace("https:", "ftp:");
        Path invalidUrl = writeVariant(configuredBaseUrl, invalidBaseUrl);
        Path escapingReports = writeVariant("uiBaseDirectory: artifacts/reports/ui",
                "uiBaseDirectory: ../outside");
        Path misspelledSetting = writeVariant("navigationTimeout: PT45S", "navigationTimout: PT30S");
        Path invalidTimeout = writeVariant("timeout: PT20S", "timeout: twenty seconds");

        assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(invalidUrl));
        assertThrows(IllegalStateException.class,
                () -> AutoExConfigurationLoader.load(escapingReports));
        assertThrows(IllegalStateException.class,
                () -> AutoExConfigurationLoader.load(misspelledSetting),
                "unknown YAML keys must not silently fall back to defaults");
        assertThrows(IllegalStateException.class,
                () -> AutoExConfigurationLoader.load(invalidTimeout));
    }

    @Test
    void redactsRegistrationAndDemoPaymentWhenConfigurationIsRenderedAsText() {
        String rendered = AutoExConfigurationLoader.load().toString();

        assertAll(
                () -> assertFalse(rendered.contains("5550100100")),
                () -> assertFalse(rendered.contains("4242424242424242")),
                () -> assertFalse(rendered.contains("123")),
                () -> assertFalse(rendered.contains("10 Test Avenue")),
                () -> assertEquals("Payment[configured=true]", AutoExConfigurationLoader.load()
                        .payment().toString()),
                () -> assertEquals("Registration[configured=true]", AutoExConfigurationLoader.load()
                        .registration().toString()));
    }

    @Test
    void storeQueueRecoveryIsConfiguredSeparatelyFromOrdinaryActionRetries() throws IOException {
        var defaults = AutoExConfigurationLoader.load();
        assertEquals(3, defaults.storeAvailabilityPolicy().maxAttempts());
        assertEquals(Duration.ofSeconds(2), defaults.storeAvailabilityPolicy().backoff());
        assertEquals(Set.of(UiRetryOperation.NAVIGATE),
                defaults.storeAvailabilityPolicy().eligibleOperations());
        assertFalse(defaults.browser().retryPolicy().enabled());
        var once = AutoExConfigurationLoader.load(writeVariant("maxAttempts: 3", "maxAttempts: 1"));
        assertFalse(once.storeAvailabilityPolicy().enabled());
        var unconfigured = AutoExConfigurationLoader.load(writeVariant(
                "    storeAvailability:\n      maxAttempts: 3\n      backoff: PT2S\n", ""));
        assertFalse(unconfigured.storeAvailabilityPolicy().enabled(),
                "absence of the explicit YAML section must not enable recovery");
    }

    @Test
    void invalidStoreQueuePolicyCannotSilentlyFallBack() throws IOException {
        assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(
                writeVariant("maxAttempts: 3", "maxAttempts: 0")));
        assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(
                writeVariant("backoff: PT2S", "backoff: PT-1S")));
        assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(
                writeVariant("maxAttempts: 3", "maxAttemps: 3")));
    }

    private Path writeVariant(String original, String replacement) throws IOException {
        String baseline = Files.readString(defaultYaml()).replace("\r\n", "\n").replace('\r', '\n');
        if (!baseline.contains(original)) {
            throw new IllegalStateException("Fixture substitution did not match the default YAML: " + original);
        }
        String yaml = baseline.replace(original, replacement);
        Path file = temporaryDirectory.resolve("variant-" + Math.abs(replacement.hashCode()) + ".yaml");
        Files.writeString(file, yaml);
        return file;
    }

    @Test
    void uiApiProfileIncludesCartXhrOutsideApiPrefixAndDocumentsRemainExplicit() throws IOException {
        var network = AutoExConfigurationLoader.load().browser().networkInspection();
        var policy = network.capturePolicy();
        var cartType = UiNetworkCapturePolicy.ResourceKind.XHR;
        var document = UiNetworkCapturePolicy.ResourceKind.DOCUMENT;
        String cart = PUBLIC_BASE_URI.resolve("/add_to_cart/1").toString();
        String checkout = PUBLIC_BASE_URI.resolve("/checkout").toString();
        assertFalse(network.enabled(), "Only the UI_API channel enables browser capture");
        assertTrue(network.withEnabled(true).accepts(cart));
        assertTrue(policy.accepts(cart, cartType), "The real cart XHR does not use an /api/ prefix");
        assertFalse(policy.accepts(checkout, document));
        Path override = writeVariant("includeDocuments: false", "includeDocuments: true");
        var configured = AutoExConfigurationLoader.load(override).browser().networkInspection();
        assertTrue(configured.capturePolicy().accepts(checkout, document));
        assertEquals(256, configured.withEnabled(true).maxObservations());
        assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(
                writeVariant("includeDocuments: false", "includeDocuments: yesplease")));
        assertThrows(IllegalStateException.class, () -> AutoExConfigurationLoader.load(
                writeVariant("maxObservations: 256", "maxObservations: 0")));
    }

    private static Path defaultYaml() {
        try {
            var resource = AutoExConfigurationLoaderComponentTest.class.getClassLoader()
                    .getResource(AutoExConfigurationLoader.DEFAULT_RESOURCE);
            if (resource == null) throw new IllegalStateException("AutoEx test YAML is not on the test classpath");
            return Path.of(resource.toURI());
        } catch (URISyntaxException error) {
            throw new IllegalStateException("Cannot resolve AutoEx test YAML resource", error);
        }
    }
}
