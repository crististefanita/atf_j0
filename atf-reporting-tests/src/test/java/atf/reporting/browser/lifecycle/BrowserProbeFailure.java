package atf.reporting.browser.lifecycle;

import java.nio.file.Path;

/** A non-terminal browser probe is a failure, with its original remote diagnostic retained. */
public final class BrowserProbeFailure extends AssertionError {
    private final String phase;
    private final long pid;
    private final Path directory;

    BrowserProbeFailure(String phase, long pid, Path directory, String reason, String diagnostic) {
        super(reason + " [phase=" + phase + ", pid=" + pid + ", evidence=" + directory + "]",
                diagnostic.isBlank() ? null : new IllegalStateException(diagnostic));
        this.phase = phase;
        this.pid = pid;
        this.directory = directory;
    }

    public String phase() { return phase; }
    public long pid() { return pid; }
    public Path directory() { return directory; }
}
