package atf.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Root-independent manifest of source files approved for one promotion.
 *
 * <p>The manifest deliberately stores only normalized relative paths, presence,
 * size and SHA-256. It can therefore be generated from a live checkout, carried
 * to an external staging checkout and checked again without leaking local roots
 * into source control or HTML artifacts. A non-present entry is intentional: it
 * approves a new file while still guarding against a parallel live creation.</p>
 */
public final class AtfPromotionManifest {
    public static final int SCHEMA_VERSION = 1;
    public static final String SHA_256 = "SHA-256";

    private final Instant createdAt;
    private final List<Entry> entries;

    private AtfPromotionManifest(Instant createdAt, Collection<Entry> entries) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.entries = entries.stream()
                .map(Objects::requireNonNull)
                .sorted(Comparator.comparing(Entry::relativePath))
                .toList();
    }

    public static AtfPromotionManifest capture(Path root, Collection<Path> approvedFiles)
            throws IOException {
        return capture(root, approvedFiles, Clock.systemUTC());
    }

    static AtfPromotionManifest capture(Path root, Collection<Path> approvedFiles, Clock clock)
            throws IOException {
        Path normalizedRoot = normalizeRoot(root);
        Objects.requireNonNull(approvedFiles, "approvedFiles");
        Objects.requireNonNull(clock, "clock");
        List<String> relativeFiles = approvedFiles.stream()
                .map(path -> relativePath(normalizedRoot, path))
                .distinct()
                .sorted()
                .toList();
        var entries = new java.util.ArrayList<Entry>();
        for (String relative : relativeFiles) {
            entries.add(entry(normalizedRoot.resolve(relative), relative));
        }
        return new AtfPromotionManifest(clock.instant(), entries);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<Entry> entries() {
        return entries;
    }

    public List<String> relativePaths() {
        return entries.stream().map(Entry::relativePath).toList();
    }

    /** Returns the relative files whose live content no longer matches this manifest. */
    public List<String> mismatches(Path root) throws IOException {
        Path normalizedRoot = normalizeRoot(root);
        return entries.stream()
                .filter(entry -> !entry.matches(normalizedRoot.resolve(entry.relativePath())))
                .map(Entry::relativePath)
                .toList();
    }

    /** Writes a portable JSON manifest; the root path is intentionally absent. */
    public void write(Path target) throws IOException {
        Path output = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", SCHEMA_VERSION);
        document.put("createdAt", createdAt.toString());
        document.put("algorithm", SHA_256);
        document.put("files", entries);
        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(document);
        Files.writeString(output, json + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    static Path normalizeRoot(Path root) {
        return Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    static String relativePath(Path root, Path candidate) {
        Path requested = Objects.requireNonNull(candidate, "candidate");
        Path normalized = (requested.isAbsolute() ? requested : root.resolve(requested))
                .toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalArgumentException("Approved file must stay inside the manifest root: "
                    + candidate);
        }
        Path relative = root.relativize(normalized);
        if (relative.getNameCount() == 0) {
            throw new IllegalArgumentException("A manifest entry must identify a file: " + candidate);
        }
        return relative.toString().replace('\\', '/');
    }

    /** Rejects symlinked roots or path components before a guarded copy follows them. */
    static void assertSafeEntryPath(Path root, String relativePath) throws IOException {
        Path normalizedRoot = normalizeRoot(root);
        Path current = normalizedRoot;
        if (Files.isSymbolicLink(current)) {
            throw new IOException("Manifest root must not be a symbolic link: " + normalizedRoot);
        }
        for (Path component : Path.of(relativePath)) {
            current = current.resolve(component.toString());
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Manifest path must not contain a symbolic link: " + relativePath);
            }
        }
    }

    static Entry entry(Path file, String relativePath) throws IOException {
        if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) {
            return Entry.absent(relativePath);
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Manifest entry is not a regular file: " + relativePath);
        }
        return Entry.present(relativePath, Files.size(file), sha256(file));
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(SHA_256 + " is required by the promotion contract", error);
        }
    }

    public record Entry(String relativePath, boolean present, long size, String sha256) {
        public Entry {
            if (relativePath == null || relativePath.isBlank()
                    || relativePath.startsWith("/") || relativePath.startsWith("\\")
                    || relativePath.indexOf('\\') >= 0) {
                throw new IllegalArgumentException("relativePath must be a portable relative path");
            }
            if (relativePath.contains("../") || relativePath.contains("..\\")
                    || relativePath.equals("..") || relativePath.contains(":")) {
                throw new IllegalArgumentException("relativePath must not escape the root: " + relativePath);
            }
            if (!present) {
                if (size != 0 || sha256 == null || !sha256.isEmpty()) {
                    throw new IllegalArgumentException("A non-present entry must not carry file content");
                }
            } else {
                if (size < 0) throw new IllegalArgumentException("size must not be negative");
                if (sha256 == null || sha256.length() != 64) {
                    throw new IllegalArgumentException("sha256 must be a 64-character digest");
                }
            }
        }

        static Entry present(String relativePath, long size, String sha256) {
            return new Entry(relativePath, true, size, sha256);
        }

        static Entry absent(String relativePath) {
            return new Entry(relativePath, false, 0, "");
        }

        boolean matches(Path file) {
            try {
                if (Files.isSymbolicLink(file)) return false;
                if (!present) return !Files.exists(file, LinkOption.NOFOLLOW_LINKS);
                return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        && Files.size(file) == size
                        && sha256.equalsIgnoreCase(AtfPromotionManifest.sha256(file));
            } catch (IOException error) {
                return false;
            }
        }
    }
}
