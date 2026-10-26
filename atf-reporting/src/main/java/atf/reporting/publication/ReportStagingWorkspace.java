package atf.reporting.publication;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Small facade for one Extent staging directory.
 *
 * <p>Lease ownership and stale-workspace sweeping are separate policies so
 * publication code cannot accidentally change cleanup semantics.</p>
 */
public final class ReportStagingWorkspace implements AutoCloseable {
    public static final String PREFIX = ".atf-extent-session-";
    public static final int DEFAULT_SWEEP_LIMIT = 256;

    private final ReportWorkspaceLease lease;

    private ReportStagingWorkspace(ReportWorkspaceLease lease) {
        this.lease = lease;
    }

    public static ReportStagingWorkspace create(Path parent, Path reportPath) throws IOException {
        return new ReportStagingWorkspace(ReportWorkspaceLease.create(parent, reportPath));
    }

    public Path directory() {
        return lease.directory();
    }

    @Override
    public void close() {
        lease.close();
    }

    public static SweepResult sweep(Path reportDirectory, Duration minimumAge) {
        return sweep(reportDirectory, minimumAge, Instant.now(), DEFAULT_SWEEP_LIMIT);
    }

    public static SweepResult sweep(Path reportDirectory, Duration minimumAge, Instant now) {
        return sweep(reportDirectory, minimumAge, now, DEFAULT_SWEEP_LIMIT);
    }

    public static SweepResult sweep(Path reportDirectory, Duration minimumAge,
                                    Instant now, int maxCandidates) {
        return ReportWorkspaceSweeper.sweep(reportDirectory, minimumAge, now, maxCandidates);
    }

    public record SweepResult(int scanned, int deleted, int skipped,
                              boolean limitReached, List<String> failures) {
        public SweepResult {
            failures = List.copyOf(failures == null ? List.of() : failures);
        }

        public static SweepResult empty() {
            return new SweepResult(0, 0, 0, false, List.of());
        }
    }
}
