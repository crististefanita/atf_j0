package atf.reporting.publication;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/** Owns the exclusive lease and marker for one report staging directory. */
public final class ReportWorkspaceLease implements AutoCloseable {
    public static final String MARKER = ".atf-workspace.properties";
    public static final String LOCK = ".atf-workspace.lock";
    private static final String VERSION = "1";

    private final Path directory;
    private final FileChannel channel;
    private final FileLock lock;
    private boolean closed;

    private ReportWorkspaceLease(Path directory, FileChannel channel, FileLock lock) {
        this.directory = directory;
        this.channel = channel;
        this.lock = lock;
    }

    public static ReportWorkspaceLease create(Path parent, Path reportPath) throws IOException {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(reportPath, "reportPath");
        Files.createDirectories(parent);
        Path directory = Files.createTempDirectory(parent, ReportStagingWorkspace.PREFIX);
        FileChannel channel = null;
        FileLock lock = null;
        try {
            channel = FileChannel.open(directory.resolve(LOCK),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.lock();
            Properties marker = new Properties();
            marker.setProperty("version", VERSION);
            marker.setProperty("createdAt", Instant.now().toString());
            marker.setProperty("processId", Long.toString(ProcessHandle.current().pid()));
            marker.setProperty("reportPath", reportPath.toAbsolutePath().normalize().toString());
            marker.setProperty("leaseId", UUID.randomUUID().toString());
            try (var output = Files.newOutputStream(directory.resolve(MARKER), StandardOpenOption.CREATE_NEW)) {
                marker.store(output, "ATF report staging workspace");
            }
            return new ReportWorkspaceLease(directory, channel, lock);
        } catch (IOException | RuntimeException error) {
            if (lock != null) lock.release();
            if (channel != null) channel.close();
            ReportWorkspaceSweeper.deleteTree(directory);
            throw error;
        }
    }

    Path directory() {
        return directory;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            lock.release();
        } catch (IOException ignored) {
            // Cleanup remains best effort after publication.
        }
        try {
            channel.close();
        } catch (IOException ignored) {
            // Cleanup remains best effort after publication.
        }
        ReportWorkspaceSweeper.deleteTree(directory);
    }
}
