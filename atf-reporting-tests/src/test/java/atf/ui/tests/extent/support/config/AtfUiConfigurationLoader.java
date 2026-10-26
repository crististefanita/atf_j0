package atf.ui.tests.extent.support.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Loads only the {@code ui} section and keeps discovery deterministic. */
public final class AtfUiConfigurationLoader {
    public static final List<String> DEFAULT_CONFIG_NAMES = List.of("atf.yaml", "atf.yml");

    private AtfUiConfigurationLoader() { }

    public static UiConfiguration load() { return load(AtfUiConfigurationContext.fromProcess()); }

    public static UiConfiguration load(AtfUiConfigurationContext context) {
        Objects.requireNonNull(context, "context");
        Path explicit = context.explicitConfigPath();
        Path discovered = explicit == null ? discover(context.startDirectory()) : explicit;
        UiConfiguration loaded = explicit == null
                ? Optional.ofNullable(discovered)
                    .map(UiConfigurationYamlReader::read)
                    .orElseGet(() -> UiConfiguration.defaults(context.startDirectory()))
                : UiConfigurationYamlReader.read(explicit);
        Path reportBase = discovered == null ? context.startDirectory() : discovered.getParent();
        Path reportOverride = context.reportOverridePath(reportBase);
        return reportOverride == null ? loaded : loaded.withAuditInputReport(reportOverride);
    }

    public static UiConfiguration load(Path path) {
        Path config = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (!Files.isRegularFile(config)) {
            throw new IllegalStateException("ATF UI configuration does not exist: " + config);
        }
        return UiConfigurationYamlReader.read(config);
    }

    public static Path discover(Path start) {
        Path current = Objects.requireNonNull(start, "start").toAbsolutePath().normalize();
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

}
