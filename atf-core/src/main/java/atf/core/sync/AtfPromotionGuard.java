package atf.core.sync;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fail-closed, selective source promotion guarded by a live baseline manifest.
 *
 * <p>Only files present in the manifest are copied. Existing live content must
 * match the baseline; an explicitly absent baseline entry allows a new file but
 * rejects a parallel live creation. Any drift aborts the whole operation before
 * the first copy. The process lock also serializes cooperating promoters without
 * adding lock files to the checkout.</p>
 */
public final class AtfPromotionGuard {
    private static final String LOCK_DIRECTORY = "atf-promotion-guards";

    private AtfPromotionGuard() { }

    public static PromotionResult promote(Path stagingRoot, Path liveRoot,
                                           AtfPromotionManifest baseline) throws IOException {
        Path stage = AtfPromotionManifest.normalizeRoot(stagingRoot);
        Path live = AtfPromotionManifest.normalizeRoot(liveRoot);
        Objects.requireNonNull(baseline, "baseline");
        if (stage.equals(live)) {
            throw new IllegalArgumentException("Staging and live roots must be different");
        }
        Path lockPath = lockPath(live);
        Files.createDirectories(lockPath.getParent());
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            for (AtfPromotionManifest.Entry entry : baseline.entries()) {
                AtfPromotionManifest.assertSafeEntryPath(live, entry.relativePath());
                AtfPromotionManifest.assertSafeEntryPath(stage, entry.relativePath());
            }
            List<String> initialConflicts = baseline.mismatches(live);
            if (!initialConflicts.isEmpty()) throw new AtfPromotionConflictException(initialConflicts);

            List<String> promoted = new ArrayList<>();
            for (AtfPromotionManifest.Entry entry : baseline.entries()) {
                Path destination = live.resolve(entry.relativePath()).normalize();
                if (!entry.matches(destination)) {
                    throw new AtfPromotionConflictException(List.of(entry.relativePath()));
                }
                Path source = stage.resolve(entry.relativePath()).normalize();
                if (!source.startsWith(stage) || !Files.isRegularFile(source)) {
                    throw new IOException("Approved staging file is missing: " + entry.relativePath());
                }
                copyAtomically(source, destination);
                promoted.add(entry.relativePath());
            }
            return new PromotionResult(promoted);
        }
    }

    private static void copyAtomically(Path source, Path destination) throws IOException {
        Path parent = destination.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = destination.resolveSibling("." + destination.getFileName()
                + ".atf-promotion-" + UUID.randomUUID() + ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path lockPath(Path liveRoot) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(liveRoot.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Path.of(System.getProperty("java.io.tmpdir"), LOCK_DIRECTORY,
                    HexFormat.of().formatHex(digest) + ".lock");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the promotion guard", error);
        }
    }

    public record PromotionResult(List<String> promotedFiles) {
        public PromotionResult {
            promotedFiles = List.copyOf(promotedFiles == null ? List.of() : promotedFiles);
        }
    }
}
