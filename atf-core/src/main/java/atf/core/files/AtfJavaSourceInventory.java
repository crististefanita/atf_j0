package atf.core.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Reads Java package declarations into a deterministic source inventory.
 * Package-size limits and ownership decisions intentionally stay outside core.
 */
public final class AtfJavaSourceInventory {
    private static final Pattern PACKAGE_DECLARATION =
            Pattern.compile("(?m)^package\\s+([^;]+);");

    private AtfJavaSourceInventory() {
    }

    /**
     * Groups declared-package Java files below a source root.
     * Files without a package declaration are ignored because they do not
     * belong to a named Java package.
     */
    public static Map<String, List<Path>> packages(Path sourceRoot) throws IOException {
        Path root = Objects.requireNonNull(sourceRoot, "sourceRoot")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("sourceRoot must be an existing directory: " + root);
        }

        Map<String, List<Path>> inventory = new TreeMap<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                var match = PACKAGE_DECLARATION.matcher(Files.readString(file));
                if (match.find()) {
                    inventory.computeIfAbsent(match.group(1), ignored -> new ArrayList<>())
                            .add(file.toAbsolutePath().normalize());
                }
            }
        }
        inventory.values().forEach(list -> list.sort(Path::compareTo));
        Map<String, List<Path>> immutable = new TreeMap<>();
        inventory.forEach((packageName, files) -> immutable.put(packageName, List.copyOf(files)));
        return Collections.unmodifiableMap(immutable);
    }
}
