package atf.console;

import java.nio.file.Path;
import java.util.Objects;

/** Captured process boundary used by the console YAML loader. */
public record AtfConsoleConfigurationContext(Path startDirectory,
                                             String systemPropertyConfig,
                                             String environmentConfig) {
    public static final String CONFIG_PROPERTY = "atf.report.config";
    public static final String CONFIG_ENVIRONMENT_VARIABLE = "ATF_REPORT_CONFIG";

    public AtfConsoleConfigurationContext {
        startDirectory = Objects.requireNonNull(startDirectory, "startDirectory")
                .toAbsolutePath().normalize();
        systemPropertyConfig = normalize(systemPropertyConfig);
        environmentConfig = normalize(environmentConfig);
    }

    public static AtfConsoleConfigurationContext fromProcess() {
        return new AtfConsoleConfigurationContext(
                Path.of(System.getProperty("user.dir", ".")),
                System.getProperty(CONFIG_PROPERTY),
                System.getenv(CONFIG_ENVIRONMENT_VARIABLE));
    }

    public Path explicitConfigPath() {
        String value = systemPropertyConfig != null ? systemPropertyConfig : environmentConfig;
        if (value == null) return null;
        Path configured = Path.of(value);
        return configured.isAbsolute()
                ? configured.normalize()
                : startDirectory.resolve(configured).normalize();
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
