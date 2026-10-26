package atf.reporting.publication;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Scans and removes only stale, explicitly marked report workspaces. */
public final class ReportWorkspaceSweeper {
    private ReportWorkspaceSweeper() {
    }

    public static ReportStagingWorkspace.SweepResult sweep(Path reportDirectory,
                                                     Duration minimumAge,
                                                     Instant now,
                                                     int maxCandidates) {
        Objects.requireNonNull(reportDirectory, "reportDirectory");
        Objects.requireNonNull(minimumAge, "minimumAge");
        Objects.requireNonNull(now, "now");
        if (minimumAge.isNegative() || minimumAge.isZero()) {
            throw new IllegalArgumentException("minimumAge must be positive");
        }
        if (maxCandidates < 1) {
            throw new IllegalArgumentException("maxCandidates must be positive");
        }
        if (!Files.isDirectory(reportDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return ReportStagingWorkspace.SweepResult.empty();
        }

        int scanned = 0;
        int deleted = 0;
        int skipped = 0;
        boolean limitReached = false;
        List<String> failures = new ArrayList<>();
        try (var children = Files.list(reportDirectory)) {
            for (Path candidate : children.toList()) {
                if (!Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)
                        || !candidate.getFileName().toString().startsWith(ReportStagingWorkspace.PREFIX)) {
                    continue;
                }
                if (scanned >= maxCandidates) {
                    limitReached = true;
                    skipped++;
                    continue;
                }
                scanned++;
                Properties marker = readMarker(candidate);
                Instant createdAt = parseCreatedAt(marker);
                if (marker == null || createdAt == null || createdAt.plus(minimumAge).isAfter(now)) {
                    skipped++;
                    continue;
                }
                Path lockPath = candidate.resolve(ReportWorkspaceLease.LOCK);
                try (var channel = FileChannel.open(lockPath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    FileLock lock;
                    try {
                        lock = channel.tryLock();
                    } catch (OverlappingFileLockException busy) {
                        lock = null;
                    }
                    if (lock == null) {
                        skipped++;
                        continue;
                    }
                    lock.release();
                    if (deleteTree(candidate)) deleted++;
                    else failures.add(candidate.getFileName().toString());
                } catch (IOException | SecurityException error) {
                    skipped++;
                    failures.add(candidate.getFileName().toString());
                }
            }
        } catch (IOException error) {
            failures.add(reportDirectory.toString());
        }
        return new ReportStagingWorkspace.SweepResult(scanned, deleted, skipped,
                limitReached, List.copyOf(failures));
    }

    private static Properties readMarker(Path directory) {
        Path markerPath = directory.resolve(ReportWorkspaceLease.MARKER);
        if (!Files.isRegularFile(markerPath, LinkOption.NOFOLLOW_LINKS)) return null;
        try (var input = Files.newInputStream(markerPath)) {
            Properties marker = new Properties();
            marker.load(input);
            return "1".equals(marker.getProperty("version")) ? marker : null;
        } catch (IOException | SecurityException error) {
            return null;
        }
    }

    private static Instant parseCreatedAt(Properties marker) {
        if (marker == null) return null;
        try {
            return Instant.parse(marker.getProperty("createdAt", ""));
        } catch (RuntimeException error) {
            return null;
        }
    }

    public static boolean deleteTree(Path root) {
        if (root == null || !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return true;
        try (var paths = Files.walk(root)) {
            boolean[] success = {true};
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException | SecurityException error) {
                    success[0] = false;
                }
            });
            return success[0] && !Files.exists(root, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException | SecurityException error) {
            return false;
        }
    }
}
