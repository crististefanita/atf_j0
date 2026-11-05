package atf.testkit.documentation.guides;

import java.nio.file.Files;
import java.nio.file.Path;

/** Seeds a private offline resolver with artifacts, never source ATF builds or old remote tracking. */
final class GuideMavenCache {
    private GuideMavenCache() { }

    static void prepare(Path work, Path seed) throws Exception {
        if (!Files.isDirectory(seed)) throw new IllegalArgumentException("Missing prepared Maven cache: " + seed);
        Path repository = Files.createDirectories(work.resolve("m2"));
        copy(seed, repository, true);
        copy(work.resolve("published"), repository, false);
    }

    private static void copy(Path source, Path target, boolean vendorsOnly) throws Exception {
        try (var files = Files.walk(source)) {
            for (Path file : files.toList()) {
                if (Files.isSymbolicLink(file)) throw new IllegalArgumentException("Resolver seed cannot contain symlinks: " + file);
                if (!Files.isRegularFile(file)) continue;
                Path relative = source.relativize(file);
                if (vendorsOnly && relative.startsWith("atf")) continue;
                String name = file.getFileName().toString();
                if (!name.endsWith(".jar") && !name.endsWith(".pom")) continue;
                Path output = target.resolve(relative);
                Files.createDirectories(output.getParent());
                Files.copy(file, output);
            }
        }
    }
}
