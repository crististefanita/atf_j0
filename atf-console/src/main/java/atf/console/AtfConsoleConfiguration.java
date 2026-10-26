package atf.console;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Immutable console bootstrap result, including the source used for diagnostics. */
public record AtfConsoleConfiguration(Optional<Path> source, ConsoleSettings settings) {
    public AtfConsoleConfiguration {
        source = Objects.requireNonNull(source, "source").map(path -> path.toAbsolutePath().normalize());
        settings = Objects.requireNonNull(settings, "settings");
    }

    public static AtfConsoleConfiguration defaults() {
        return new AtfConsoleConfiguration(Optional.empty(), ConsoleSettings.defaults());
    }
}
