package atf.debugui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Immutable policy for the local, loopback-only debug publisher. */
public record DebugUiConfiguration(Path rootDirectory, int port) {
    public DebugUiConfiguration {
        rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(rootDirectory)) {
            throw new IllegalArgumentException("Debug UI root must be an existing directory: " + rootDirectory);
        }
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("Debug UI port must be between 0 and 65535: " + port);
        }
    }

    public static DebugUiConfiguration forHtml(Path htmlFile, int port) {
        Path file = Objects.requireNonNull(htmlFile, "htmlFile").toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Debug UI HTML file does not exist: " + file);
        }
        return new DebugUiConfiguration(file.getParent(), port);
    }
}
