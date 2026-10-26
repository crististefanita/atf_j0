package atf.core.files;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Reusable, root-independent file and source-root discovery utilities. */
public final class AtfPathLocator {
    private static final List<Path> ATF_TEST_SOURCE_ROOT_CANDIDATES = List.of(
            Path.of("src", "test", "java"),
            Path.of("atf-reporting", "src", "test", "java"));

    private AtfPathLocator() {
    }

    /** Finds the first configured regular file while walking from a directory to its ancestors. */
    public static Path firstRegularFileUpwards(Path start, Collection<String> fileNames) {
        Path current = normalizeStart(start);
        List<String> names = List.copyOf(Objects.requireNonNull(fileNames, "fileNames"));
        if (names.isEmpty() || names.stream().anyMatch(name -> name == null || name.isBlank())) {
            throw new IllegalArgumentException("fileNames must contain at least one non-blank name");
        }
        while (current != null) {
            for (String name : names) {
                Path candidate = current.resolve(name).normalize();
                if (candidate.getParent().equals(current) && Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Finds a source root candidate whose required package/path exists below it.
     * The returned path is the source root, not the package marker.
     */
    public static Path locateSourceRoot(Path start, Path requiredRelativePath,
                                        Collection<Path> sourceRootCandidates) {
        Path required = relativePath(requiredRelativePath, "requiredRelativePath");
        List<Path> candidates = List.copyOf(Objects.requireNonNull(
                sourceRootCandidates, "sourceRootCandidates"));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("sourceRootCandidates must not be empty");
        }
        if (candidates.stream().anyMatch(path -> path == null || path.isAbsolute())) {
            throw new IllegalArgumentException("source root candidates must be relative");
        }

        Path current = normalizeStart(start);
        while (current != null) {
            for (Path candidate : candidates) {
                Path sourceRoot = current.resolve(candidate).normalize();
                if (sourceRoot.getParent() != null
                        && sourceRoot.getParent().startsWith(current)
                        && Files.isDirectory(sourceRoot)
                        && Files.isDirectory(sourceRoot.resolve(required))) {
                    return sourceRoot;
                }
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate source root containing " + required);
    }

    /**
     * Locates one source file across supported source-root layouts. Useful for
     * architecture/component tests that inspect production source without
     * embedding their own ancestor walk or module-specific path guesses.
     */
    public static Path locateSourceFile(Path start, Path requiredRelativeFile,
                                        Collection<Path> sourceRootCandidates) {
        Path required = relativePath(requiredRelativeFile, "requiredRelativeFile");
        List<Path> candidates = List.copyOf(Objects.requireNonNull(
                sourceRootCandidates, "sourceRootCandidates"));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("sourceRootCandidates must not be empty");
        }
        if (candidates.stream().anyMatch(path -> path == null || path.isAbsolute()
                || path.normalize().startsWith(".."))) {
            throw new IllegalArgumentException("source root candidates must stay relative");
        }

        Path current = normalizeStart(start);
        while (current != null) {
            for (Path candidate : candidates) {
                Path sourceRoot = current.resolve(candidate).normalize();
                Path sourceFile = sourceRoot.resolve(required).normalize();
                if (sourceRoot.startsWith(current) && Files.isDirectory(sourceRoot)
                        && sourceFile.startsWith(sourceRoot) && Files.isRegularFile(sourceFile)) {
                    return sourceFile;
                }
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate source file " + required);
    }

    /**
     * Locates a module below the first ancestor that contains the requested
     * module path and a required relative marker below that module.
     */
    public static Path locateModuleRoot(Path start, Path moduleRelativePath,
                                        Path requiredRelativePath) {
        Path module = relativePath(moduleRelativePath, "moduleRelativePath");
        Path required = relativePath(requiredRelativePath, "requiredRelativePath");
        Path current = normalizeStart(start);
        while (current != null) {
            Path moduleRoot = current.resolve(module).normalize();
            if (moduleRoot.getParent() != null
                    && moduleRoot.getParent().startsWith(current)
                    && Files.isDirectory(moduleRoot)
                    && Files.isDirectory(moduleRoot.resolve(required))) {
                return moduleRoot;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate module root containing " + module
                + " and marker " + required);
    }

    /**
     * Resolves the conventional source-set root for a module without encoding
     * any repository policy. Maven layouts use {@code src/main/java} and
     * {@code src/test/java}; the legacy ATF test modules use {@code src/main}
     * and {@code src/test} directly.
     */
    public static Path resolveModuleSourceRoot(Path projectRoot, String module,
                                               boolean tests) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("projectRoot must be an existing directory: " + root);
        }
        if (module == null || module.isBlank() || module.contains("/") || module.contains("\\")) {
            throw new IllegalArgumentException("module must be a non-blank module directory name");
        }
        Path sourceSet = root.resolve(module).resolve(tests ? "src/test" : "src/main");
        Path conventional = sourceSet.resolve("java");
        return Files.isDirectory(conventional) ? conventional : sourceSet;
    }

    /**
     * Locates an ATF test source root using the repository's supported module
     * layouts. Test code should consume this seam instead of repeating
     * user.dir traversal and module-specific candidate lists.
     */
    public static Path locateAtfTestSourceRoot(Path start, Path requiredRelativePath) {
        return locateSourceRoot(start, requiredRelativePath, ATF_TEST_SOURCE_ROOT_CANDIDATES);
    }

    private static Path normalizeStart(Path start) {
        Path normalized = Objects.requireNonNull(start, "start").toAbsolutePath().normalize();
        return Files.isDirectory(normalized) ? normalized : normalized.getParent();
    }

    private static Path relativePath(Path path, String name) {
        Path normalized = Objects.requireNonNull(path, name).normalize();
        if (normalized.isAbsolute() || normalized.getNameCount() == 0
                || normalized.toString().isBlank() || normalized.startsWith("..")) {
            throw new IllegalArgumentException(name + " must be a non-empty relative path");
        }
        return normalized;
    }
}
