package atf.shop.tests.component.configuration;

import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.reporting.environment.ShopReportSystemMetadata;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

@ShopComponentTest
@Isolated("Exercises configuration while temporarily changing the JVM locale")
@ResourceLock("java.util.Locale.default")
class ShopLocaleConfigurationComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"tr-TR", "en-US", "lt-LT"})
    void protocolAliasesAndYamlAreIndependentOfTheJvmLocale(String language, @TempDir Path directory) throws Exception {
        Path yaml = directory.resolve("locale.yaml");
        Files.writeString(yaml, "practiceShop:\n  baseUrl: https://fixture.test\n");
        Locale previous = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.forLanguageTag(language));
            for (String value : List.of("UI_API", "UI-API", "ui_api", "ui-api", "")) {
                assertEquals(ShopValidationProfile.UI_API, ShopValidationProfile.from(value), "UI_API alias: " + value);
            }
            for (String value : List.of("UI_ONLY", "UI-ONLY", "ui_only", "ui-only", "UI")) {
                assertEquals(ShopValidationProfile.UI_ONLY, ShopValidationProfile.from(value), "UI_ONLY alias: " + value);
            }
            for (ShopApiCheckpoint checkpoint : ShopApiCheckpoint.values()) {
                assertEquals(checkpoint, ShopApiCheckpoint.from(checkpoint.name()), "Uppercase checkpoint: " + checkpoint.name());
                assertEquals(checkpoint, ShopApiCheckpoint.from(checkpoint.key()), "Checkpoint key: " + checkpoint.key());
            }
            var configuration = ShopConfigurationLoader.load(yaml);
            assertEquals(ShopValidationProfile.UI_API, configuration.validationProfile(),
                    "A missing override preserves the YAML/default enum independently of locale");
            assertEquals("API request precondition: authenticated",
                    ShopReportSystemMetadata.from(configuration).get(ReportEnvironmentField.PRECONDITION.label()),
                    "Overview/System uses the same stable protocol vocabulary");
            assertThrows(IllegalArgumentException.class, () -> ShopValidationProfile.from("invalid-profile"));
            assertThrows(IllegalArgumentException.class, () -> ShopApiCheckpoint.from("invalid-checkpoint"));
        } finally {
            Locale.setDefault(previous);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }
}
