package atf.reporting.configuration;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable process boundary used to resolve report configuration.
 *
 * <p>The YAML parser must not sample global process state while a report is
 * being opened. The default factory is the only place that reads process
 * state; tests, parallel runners and embedding applications can pass a fully
 * explicit context instead.</p>
 */
public record AtfReportConfigurationContext(Path startDirectory,
                                            String systemPropertyConfig,
                                            String environmentConfig) {
    public AtfReportConfigurationContext {
        startDirectory = Objects.requireNonNull(startDirectory, "startDirectory")
                .toAbsolutePath().normalize();
        systemPropertyConfig = normalize(systemPropertyConfig);
        environmentConfig = normalize(environmentConfig);
    }

    /** Captures process state once, at the composition boundary. */
    public static AtfReportConfigurationContext fromProcess() {
        return new AtfReportConfigurationContext(
                Path.of(System.getProperty("user.dir", ".")),
                System.getProperty(AtfReportConfigurationLoader.CONFIG_PROPERTY),
                System.getenv(AtfReportConfigurationLoader.CONFIG_ENVIRONMENT_VARIABLE));
    }

    /** System property intentionally wins over the environment variable. */
    public Path explicitConfigPath() {
        String value = systemPropertyConfig != null ? systemPropertyConfig : environmentConfig;
        if (value == null) {
            return null;
        }
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
