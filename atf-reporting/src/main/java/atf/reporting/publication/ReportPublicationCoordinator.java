package atf.reporting.publication;

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
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns the final-file publication boundary for report sessions.
 *
 * <p>Each session renders in its private staging directory. This coordinator
 * serializes only the short move into the requested final path, both inside
 * the current JVM and across JVM processes. The lock metadata lives under the
 * process temporary directory, never beside the user-facing report.</p>
 */
public final class ReportPublicationCoordinator {
    /*
     * Entries are intentionally retained for the life of the JVM. Removing a
     * lock after an apparently idle check has a small but real race: another
     * publisher may already hold the old reference while a third publisher
     * creates a new one for the same path.
     */
    private static final ConcurrentHashMap<Path, ReentrantLock> JVM_LOCKS = new ConcurrentHashMap<>();
    private static final String LOCK_DIRECTORY = "atf-report-publication-locks";
    private static final int MAX_MOVE_ATTEMPTS = 8;
    private static final long MOVE_RETRY_DELAY_MILLIS = 50L;

    private ReportPublicationCoordinator() { }

    public static void publishAtomic(Path stagedReport, Path finalReport) throws IOException {
        Path staged = Objects.requireNonNull(stagedReport, "stagedReport").toAbsolutePath().normalize();
        Path destination = Objects.requireNonNull(finalReport, "finalReport").toAbsolutePath().normalize();
        Path lockPath = lockPath(destination);
        ReentrantLock jvmLock = JVM_LOCKS.computeIfAbsent(destination, ignored -> new ReentrantLock(true));
        jvmLock.lock();
        try {
            Files.createDirectories(lockPath.getParent());
            try (FileChannel channel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                Path parent = destination.getParent();
                if (parent != null) Files.createDirectories(parent);
                if (Files.isDirectory(destination)) {
                    throw new IOException("Report destination is a directory: " + destination);
                }
                moveWithRetry(staged, destination);
            }
        } finally {
            jvmLock.unlock();
        }
    }

    /**
     * Windows can briefly keep a freshly rendered report or its destination
     * open while the browser/debug server and the filesystem settle. The
     * publication boundary is allowed a short, bounded retry window; a real
     * permission or ownership error is still surfaced after the final attempt.
     */
    private static void moveWithRetry(Path staged, Path destination) throws IOException {
        IOException lastFailure = null;
        boolean atomicMoveAvailable = true;
        for (int attempt = 1; attempt <= MAX_MOVE_ATTEMPTS; attempt++) {
            try {
                if (atomicMoveAvailable) {
                    try {
                        Files.move(staged, destination,
                                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        return;
                    } catch (AtomicMoveNotSupportedException unsupported) {
                        atomicMoveAvailable = false;
                    }
                }
                Files.move(staged, destination, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException failure) {
                lastFailure = failure;
                if (attempt == MAX_MOVE_ATTEMPTS) throw failure;
                pauseBeforeRetry();
            }
        }
        throw lastFailure == null
                ? new IOException("Report publication did not attempt a move")
                : lastFailure;
    }

    private static void pauseBeforeRetry() throws IOException {
        try {
            TimeUnit.MILLISECONDS.sleep(MOVE_RETRY_DELAY_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Report publication retry was interrupted", interrupted);
        }
    }

    private static Path lockPath(Path destination) {
        String digest = sha256(destination.toString());
        return Path.of(System.getProperty("java.io.tmpdir"), LOCK_DIRECTORY, digest + ".lock");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required for report publication ownership", error);
        }
    }
}
