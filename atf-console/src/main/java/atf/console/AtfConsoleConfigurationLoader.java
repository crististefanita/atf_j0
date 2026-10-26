package atf.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Loads only reporting.console and never parses or owns the Extent configuration. */
public final class AtfConsoleConfigurationLoader {
    public static final List<String> DEFAULT_CONFIG_NAMES = List.of("atf.yaml", "atf.yml");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private AtfConsoleConfigurationLoader() { }

    public static AtfConsoleConfiguration load() {
        return load(AtfConsoleConfigurationContext.fromProcess());
    }

    public static AtfConsoleConfiguration load(AtfConsoleConfigurationContext context) {
        Objects.requireNonNull(context, "context");
        Path explicit = context.explicitConfigPath();
        if (explicit != null) return load(explicit);
        Path discovered = discover(context.startDirectory());
        return discovered == null ? AtfConsoleConfiguration.defaults() : load(discovered);
    }

    public static AtfConsoleConfiguration load(Path configPath) {
        Objects.requireNonNull(configPath, "configPath");
        Path absolute = configPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IllegalStateException("ATF console configuration does not exist: " + absolute);
        }
        try {
            JsonNode root = YAML.readTree(absolute.toFile());
            JsonNode console = root.path("reporting").path("console");
            boolean enabled = bool(console, "enabled", ConsoleSettings.DEFAULT_ENABLED,
                    "reporting.console.enabled");
            boolean details = bool(console, "detailsEnabled", ConsoleSettings.DEFAULT_DETAILS_ENABLED,
                    "reporting.console.detailsEnabled");
            return new AtfConsoleConfiguration(java.util.Optional.of(absolute),
                    new ConsoleSettings(enabled, details));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read ATF console configuration: " + absolute, error);
        }
    }

    public static Path discover(Path startDirectory) {
        Path current = Objects.requireNonNull(startDirectory, "startDirectory")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(current)) current = current.getParent();
        while (current != null) {
            for (String name : DEFAULT_CONFIG_NAMES) {
                Path candidate = current.resolve(name);
                if (Files.isRegularFile(candidate)) return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean bool(JsonNode parent, String name, boolean defaultValue, String key) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return defaultValue;
        if (value.isBoolean()) return value.booleanValue();
        throw new IllegalStateException("ATF console configuration value must be boolean: " + key);
    }
}
