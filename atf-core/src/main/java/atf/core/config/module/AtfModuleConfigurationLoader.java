package atf.core.config.module;

import atf.core.files.AtfPathLocator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Loads the optional top-level {@code modules} selection from ATF YAML. */
public final class AtfModuleConfigurationLoader {
    public static final String CONFIG_PROPERTY = "atf.config";
    public static final String CONFIG_ENVIRONMENT_VARIABLE = "ATF_CONFIG";
    public static final List<String> DEFAULT_CONFIG_NAMES = List.of("atf.yaml", "atf.yml");

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private AtfModuleConfigurationLoader() {
    }

    /** Missing configuration or missing {@code modules} preserves legacy all-module behavior. */
    public static AtfModuleSet load() {
        Path start = Path.of(System.getProperty("user.dir", "."));
        String explicit = System.getProperty(CONFIG_PROPERTY);
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv(CONFIG_ENVIRONMENT_VARIABLE);
        }
        Path path = explicit == null || explicit.isBlank()
                ? discover(start)
                : resolve(start, explicit);
        return path == null ? AtfModuleSet.all() : load(path);
    }

    public static AtfModuleSet load(Path config) {
        Path file = Objects.requireNonNull(config, "config").toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("ATF module configuration does not exist: " + file);
        }
        try {
            JsonNode root = YAML.readTree(file.toFile());
            JsonNode modules = root.path("modules");
            if (modules.isMissingNode() || modules.isNull()) return AtfModuleSet.all();
            if (!modules.isArray()) {
                throw new IllegalStateException("ATF modules must be an array: modules");
            }
            List<AtfModule> selected = new ArrayList<>();
            for (JsonNode value : modules) {
                if (!value.isTextual() || value.asText().isBlank()) {
                    throw new IllegalStateException("ATF module names must be non-blank text: modules");
                }
                selected.add(AtfModule.fromKey(value.asText()));
            }
            return AtfModuleSet.explicit(selected);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read ATF module configuration: " + file, error);
        }
    }

    public static Path discover(Path start) {
        return AtfPathLocator.firstRegularFileUpwards(start, DEFAULT_CONFIG_NAMES);
    }

    private static Path resolve(Path start, String value) {
        Path configured = Path.of(value.trim());
        return configured.isAbsolute()
                ? configured.normalize()
                : start.toAbsolutePath().normalize().resolve(configured).normalize();
    }
}
