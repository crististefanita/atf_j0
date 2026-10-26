package atf.core.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Root-independent file discovery for reusable source and test guards. */
public final class AtfFileTree {
    private AtfFileTree() {
    }

    /**
     * Collects regular files below relative roots in deterministic order.
     * Selection remains explicit at the call site so this helper does not know
     * module-specific package or test rules.
     */
    public static List<Path> regularFiles(Path root, Collection<Path> relativeRoots,
                                          Predicate<Path> selection) throws IOException {
        Path normalizedRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IllegalArgumentException("root must be an existing directory: " + normalizedRoot);
        }
        List<Path> roots = List.copyOf(Objects.requireNonNull(relativeRoots, "relativeRoots"));
        if (roots.isEmpty() || roots.stream().anyMatch(path -> path == null || path.isAbsolute())) {
            throw new IllegalArgumentException("relativeRoots must contain relative paths");
        }
        Predicate<Path> predicate = Objects.requireNonNull(selection, "selection");
        try (Stream<Path> paths = roots.stream()
                .map(path -> resolveChild(normalizedRoot, path, "relative root"))
                .peek(AtfFileTree::requireDirectory)
                .flatMap(AtfFileTree::walk)) {
            return paths.filter(Files::isRegularFile)
                    .filter(predicate)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .distinct()
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    /** Finds regular files below a root whose names end with the supplied suffix. */
    public static boolean containsRegularFileEndingWith(Path root, String suffix)
            throws IOException {
        Path normalizedRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        if (suffix == null || suffix.isBlank()) {
            throw new IllegalArgumentException("suffix must be non-blank");
        }
        if (!Files.isDirectory(normalizedRoot)) return false;
        try (Stream<Path> paths = Files.walk(normalizedRoot)) {
            return paths.filter(Files::isRegularFile)
                    .anyMatch(path -> path.getFileName().toString().endsWith(suffix));
        }
    }

    private static Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot walk file root: " + root, error);
        }
    }

    private static Path resolveChild(Path root, Path relative, String label) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(label + " must remain below root: " + relative);
        }
        return resolved;
    }

    private static void requireDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("file root must be an existing directory: " + path);
        }
    }
}
