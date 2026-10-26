package atf.validation.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Resolves JSON schemas without making relative paths depend on the caller's
 * current working directory. Relative locations are confined to the declared
 * roots; absolute locations remain supported for backwards compatibility.
 */
public final class SchemaResourceResolver {
    private final List<Path> roots;

    public SchemaResourceResolver(Collection<Path> roots) {
        Objects.requireNonNull(roots, "roots");
        this.roots = roots.stream()
                .filter(Objects::nonNull)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .distinct()
                .toList();
        if (this.roots.isEmpty()) {
            throw new IllegalArgumentException("at least one schema root is required");
        }
    }

    public SchemaResourceResolver(Path... roots) {
        this(Arrays.asList(roots));
    }

    public static SchemaResourceResolver fromWorkingDirectory() {
        return new SchemaResourceResolver(Path.of(System.getProperty("user.dir", ".")));
    }

    public List<Path> roots() {
        return roots;
    }

    /** Returns a real regular file, or {@code null} when it is not allowed or missing. */
    public Path resolve(String location) {
        Objects.requireNonNull(location, "location");
        if (location.isBlank()) return null;

        Path requested = Path.of(location);
        if (requested.isAbsolute()) {
            return realFile(requested.toAbsolutePath().normalize());
        }

        for (Path root : roots) {
            Path candidate = root.resolve(requested).normalize();
            if (!candidate.startsWith(root)) continue;
            Path resolved = realFile(candidate);
            if (resolved != null && resolved.startsWith(realRoot(root))) return resolved;
        }
        return null;
    }

    private static Path realFile(Path candidate) {
        if (!Files.isRegularFile(candidate)) return null;
        try {
            return candidate.toRealPath();
        } catch (IOException error) {
            return null;
        }
    }

    private static Path realRoot(Path root) {
        try {
            return Files.isDirectory(root) ? root.toRealPath() : root;
        } catch (IOException error) {
            return root;
        }
    }
}
