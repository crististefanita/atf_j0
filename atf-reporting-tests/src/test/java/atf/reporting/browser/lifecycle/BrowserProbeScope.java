package atf.reporting.browser.lifecycle;

import java.nio.file.Path;

/** Phase channel of the exclusively owned worker; caller JVMs never acquire SDK resources. */
public final class BrowserProbeScope implements AutoCloseable {
    private static Path directory;
    private static String current = "SETUP";
    private static Throwable escaped;
    private final String previous;
    private final boolean changed;

    private BrowserProbeScope(String next) {
        if (directory == null) throw new IllegalStateException("Browser resources require an isolated Reporting worker");
        previous = current;
        changed = !next.equals(previous);
        if (changed) phase(next);
    }

    static void attach(Path owner) { directory = owner; current = "SETUP"; escaped = null; }
    public static boolean active() { return directory != null; }
    public static Path directory() { return directory; }
    public static BrowserProbeScope setup() { return new BrowserProbeScope("SETUP"); }
    public static BrowserProbeScope cleanup() { return new BrowserProbeScope("CLEANUP"); }
    public static void caseStarted() { phase("VERIFY"); }

    static void phase(String next) {
        try { BrowserProbeDiagnostics.phase(directory, next); current = next; }
        catch (Exception error) { throw new IllegalStateException("Cannot publish browser phase " + next, error); }
    }

    public static void recordFailure(Throwable original) {
        try { BrowserProbeDiagnostics.failure(directory, original); }
        catch (Exception diagnostic) { if (original != diagnostic) original.addSuppressed(diagnostic); }
    }

    public static void escapedFailure(Throwable original) {
        if (escaped == null) escaped = original;
        recordFailure(escaped);
    }

    public static void recordCleanupFailure(Throwable cleanup) {
        if (directory == null) return;
        try { BrowserProbeDiagnostics.cleanupFailure(directory, cleanup); }
        catch (Exception diagnostic) { if (cleanup != diagnostic) cleanup.addSuppressed(diagnostic); }
    }

    public static void bodyCompleted() {
        if (escaped != null) { recordFailure(escaped); return; }
        try { java.nio.file.Files.deleteIfExists(directory.resolve("failure.log")); }
        catch (java.io.IOException error) { throw new IllegalStateException("Cannot clear a handled browser diagnostic", error); }
    }

    @Override public void close() { if (changed) phase(previous); }
}
