package atf.reporting.audit;

import atf.reporting.contract.AtfReportPorts;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.extent.session.ExtentReportSession;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** Owns one report session; invocation projections borrow it and own only their scenarios. */
public final class AssertionAuditRun implements AutoCloseable {
    private final AtfReportPorts.Session session;
    private final SensitiveDataRedactor redactor;
    private final PublicationExecutor executor;
    private final ReportingSettings settings;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AssertionAuditRun(AtfReportPorts.Session session, SensitiveDataRedactor redactor) {
        this(session, redactor, ReportingSettings.defaults(), PublicationExecutor.direct());
    }

    /** Uses the same immutable settings as the supplied renderer; does not load process configuration. */
    public AssertionAuditRun(AtfReportPorts.Session session, SensitiveDataRedactor redactor, ReportingSettings settings) {
        this(session, redactor, settings, PublicationExecutor.direct());
    }

    private AssertionAuditRun(AtfReportPorts.Session session, SensitiveDataRedactor redactor,
                              ReportingSettings settings, PublicationExecutor executor) {
        this.session = Objects.requireNonNull(session, "session");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.executor = executor;
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /** The collector must bound values using this run's resolved setting, before publication. */
    public int assertionMaxDisplayCharacters() { return settings.assertionMaxDisplayCharacters(); }

    public static AssertionAuditRun extent(Path directory, String prefix, ReportEnvironmentSnapshot environment,
                                           AtfConsoleSink console) {
        if (prefix == null || !prefix.matches("[A-Za-z0-9][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("A safe report filename prefix is required");
        }
        Path output = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(console, "console");
        try { Files.createDirectories(output); }
        catch (IOException failure) { throw new UncheckedIOException("Cannot prepare component report directory", failure); }
        Path file = output.resolve(prefix + "_" + Instant.now().toString().replace(":", "-") + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".html");
        var redactor = new SensitiveDataRedactor();
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        var executor = PublicationExecutor.dedicated();
        try {
            var session = executor.call(() -> new ExtentReportSession(file, settings, redactor, console, environment));
            return new AssertionAuditRun(session, redactor, settings, executor);
        } catch (RuntimeException | Error failure) {
            executor.close();
            throw failure;
        }
    }

    public AssertionAuditReport open(AssertionAuditReport.Identity identity) {
        if (closed.get()) throw new IllegalStateException("Assertion audit run is closed");
        return executor.call(() -> {
            if (closed.get()) throw new IllegalStateException("Assertion audit run is closed");
            return AssertionAuditReport.borrowed(session, identity, redactor, executor);
        });
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { executor.run(session::close); }
        finally { executor.close(); }
    }
    /** Serializes publication on its actual session owner, without moving test execution or replaying work. */
    static final class PublicationExecutor implements AutoCloseable {
        private final ExecutorService worker;
        private volatile Thread owner;

        private PublicationExecutor(boolean dedicated) {
            worker = dedicated ? Executors.newSingleThreadExecutor(action -> {
                Thread thread = new Thread(action, "atf-component-report");
                thread.setDaemon(true);
                owner = thread;
                return thread;
            }) : null;
        }

        static PublicationExecutor direct() { return new PublicationExecutor(false); }
        static PublicationExecutor dedicated() { return new PublicationExecutor(true); }

        <T> T call(Supplier<T> action) {
            if (worker == null || Thread.currentThread() == owner) return action.get();
            var result = worker.submit(action::get);
            boolean interrupted = false;
            try {
                while (true) {
                    try { return result.get(); }
                    catch (InterruptedException interruption) { interrupted = true; }
                }
            } catch (ExecutionException failure) {
                if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
                if (failure.getCause() instanceof Error error) throw error;
                throw new IllegalStateException("Report operation failed", failure.getCause());
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }

        void run(Runnable action) { call(() -> { action.run(); return null; }); }

        @Override public void close() { if (worker != null) worker.shutdown(); }
    }

}
