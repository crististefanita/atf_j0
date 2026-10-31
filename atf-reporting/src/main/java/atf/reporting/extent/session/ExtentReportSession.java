package atf.reporting.extent.session;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.configuration.ExtentReportConfiguration;
import atf.reporting.configuration.ReportEnvironment;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.theme.AtfReportTheme;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.security.SensitiveDataRedactor;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/** Public facade for the Extent boundary; tree scopes live behind this API. */
public final class ExtentReportSession implements AtfReportPorts.Session, AutoCloseable {
    private final Path reportPath;
    private final ExtentReportSessionPublication publication;
    private final ExtentReports reports;
    private final ExtentReportTree tree;
    private final AtfConsoleSink console;
    private final SensitiveDataRedactor environmentRedactor;
    private final ReportingSettings settings;
    private final long ownerThreadId = Thread.currentThread().getId();
    private boolean closed;

    public ExtentReportSession(Path reportPath) {
        this(reportPath, ReportingSettings.defaults(), new SensitiveDataRedactor());
    }

    public ExtentReportSession(Path reportPath, ReportingSettings settings) {
        this(reportPath, settings, new SensitiveDataRedactor());
    }

    /**
     * Opens the Extent boundary with an explicit evidence redactor. The
     * default constructors always use the conservative production policy so
     * callers cannot accidentally publish raw secrets to HTML.
     */
    public ExtentReportSession(Path reportPath, ReportingSettings settings, SensitiveDataRedactor redactor) {
        this(reportPath, settings, redactor, AtfConsoleSink.noOp());
    }

    public ExtentReportSession(Path reportPath, ReportingSettings settings,
                               SensitiveDataRedactor redactor, AtfConsoleSink console) {
        this(reportPath, settings, redactor, console, Map.of());
    }

    /** Opens a session with safe, consumer-owned Overview/System metadata. */
    public ExtentReportSession(Path reportPath, ReportingSettings settings,
                               SensitiveDataRedactor redactor, AtfConsoleSink console,
                               Map<String, String> systemInfo) {
        this(reportPath, settings, redactor, console, systemInfo,
                ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    /** Opens a session with an explicit UI action disclosure policy. */
    public ExtentReportSession(Path reportPath, ReportingSettings settings,
                               SensitiveDataRedactor redactor, AtfConsoleSink console,
                               Map<String, String> systemInfo,
                               ReportUiActionDetailsMode detailsMode) {
        this(reportPath, settings, redactor, console, systemInfo, detailsMode,
                LifecycleStartProbe.noOp());
    }

    ExtentReportSession(Path reportPath, ReportingSettings settings,
                        SensitiveDataRedactor redactor, AtfConsoleSink console,
                        Map<String, String> systemInfo,
                        ReportUiActionDetailsMode detailsMode, LifecycleStartProbe startProbe) {
        this.reportPath = Objects.requireNonNull(reportPath, "reportPath").toAbsolutePath().normalize();
        this.settings = Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(redactor, "redactor");
        this.environmentRedactor = redactor;
        this.console = Objects.requireNonNull(console, "console");
        Objects.requireNonNull(detailsMode, "detailsMode");
        Objects.requireNonNull(startProbe, "startProbe");
        var environment = new java.util.LinkedHashMap<>(ReportEnvironment.with(systemInfo, redactor));
        environment.putAll(ReportEnvironment.reportingSettings(settings).systemDisplayValues());
        this.publication = ExtentReportSessionPublication.open(this.reportPath);
        try {
            var spark = new ExtentReportTimestampReporter(publication.stagedReport().toString());
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle(AtfReportLabels.REPORT_TITLE);
            spark.config().setReportName(AtfReportLabels.REPORT_TITLE);
            spark.config().setOfflineMode(true);
            spark.config().setCss(AtfReportTheme.sparkCss());
            reports = new ExtentReports();
            reports.attachReporter(spark);
            environment.forEach(reports::setSystemInfo);
            tree = new ExtentReportTree(reports, settings, redactor, this::ensureOpen, this.console,
                    detailsMode, startProbe);
        } catch (RuntimeException | Error failure) {
            try {
                publication.close();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    /** Opens a session with allowlisted, typed Overview/System metadata. */
    public ExtentReportSession(Path reportPath, ReportingSettings settings,
                               SensitiveDataRedactor redactor, AtfConsoleSink console,
                               ReportEnvironmentSnapshot systemInfo) {
        this(reportPath, settings, redactor, console,
                ReportEnvironment.with(systemInfo, redactor));
    }

    /** Opens an Extent session using the discovered atf.yaml/atf.yml configuration. */
    public static ExtentReportSession openConfigured() {
        return openConfigured(AtfReportConfigurationLoader.load());
    }

    /** Opens an Extent session using an explicit YAML configuration file. */
    public static ExtentReportSession openConfigured(Path configPath) {
        return openConfigured(AtfReportConfigurationLoader.load(configPath));
    }

    /** Opens a session from one already-resolved immutable configuration snapshot. */
    public static ExtentReportSession openConfigured(ExtentReportConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return new ExtentReportSession(configuration.resolveReportPath(Clock.systemDefaultZone()),
                configuration.reportingSettings());
    }

    /** Opens a configured session with allowlisted, typed Overview/System metadata. */
    public static ExtentReportSession openConfigured(ExtentReportConfiguration configuration,
                                                     ReportEnvironmentSnapshot systemInfo) {
        return openConfigured(configuration, systemInfo, AtfConsoleSink.noOp());
    }

    /** Opens a configured session with explicit metadata and publication diagnostics. */
    public static ExtentReportSession openConfigured(ExtentReportConfiguration configuration,
                                                     ReportEnvironmentSnapshot systemInfo,
                                                     AtfConsoleSink console) {
        Objects.requireNonNull(configuration, "configuration");
        return new ExtentReportSession(
                configuration.resolveReportPath(Clock.systemDefaultZone()),
                configuration.reportingSettings(), new SensitiveDataRedactor(),
                Objects.requireNonNull(console, "console"), systemInfo);
    }

    @Override public Path reportPath() { return reportPath; }

    /** Updates operational metadata through the same allowlist and redaction boundary as opening. */
    public void updateEnvironment(ReportEnvironmentSnapshot snapshot) {
        ensureOpen();
        var environment = new java.util.LinkedHashMap<>(Objects.requireNonNull(snapshot, "snapshot").systemDisplayValues());
        environment.putAll(ReportEnvironment.reportingSettings(settings).systemDisplayValues());
        environment.forEach((name, value) -> {
            String safe = environmentRedactor.redactField(name, value);
            reports.getReport().getSystemEnvInfo().removeIf(field -> name.equals(field.getName()));
            reports.setSystemInfo(name, safe);
        });
    }

    /** Returns the immutable semantic population recorded by this session. */
    public AtfReportSemanticSnapshot semanticSnapshot() {
        return tree.semanticSnapshot();
    }

    @Override
    public AtfReportPorts.Scenario startScenario(String testClass, String scenario, Collection<String> tags) {
        ensureOpen();
        return tree.startScenario(testClass, scenario, tags);
    }

    @Override
    public AtfReportPorts.Scenario startScenario(String testClass, String testPackage,
                                                 String scenario, Collection<String> tags) {
        ensureOpen();
        return tree.startScenario(testClass, testPackage, scenario, tags);
    }

    @Override
    public void flush() {
        ensureOpen();
        reports.flush();
    }

    @Override
    public void close() {
        ensureOwnerThread();
        if (!closed) {
            try {
                tree.completeClassRoots();
                reports.flush();
                publication.publish();
                if (!Files.isRegularFile(reportPath)) {
                    throw new IllegalStateException("Extent report publication completed without a final file: "
                            + reportPath);
                }
                try {
                    console.publish(AtfConsoleEvent.reportPublished(reportPath));
                } catch (RuntimeException ignored) {
                    // A diagnostic sink must not change the report or test verdict.
                }
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Cannot finalize Extent report: " + reportPath, error);
            } finally {
                closed = true;
                publication.close();
            }
        }
    }

    private void ensureOpen() {
        ensureOwnerThread();
        if (closed) throw new IllegalStateException("Extent report session is closed");
    }

    private void ensureOwnerThread() {
        long currentThreadId = Thread.currentThread().getId();
        if (currentThreadId != ownerThreadId) {
            throw new IllegalStateException("Extent report session is thread-confined to owner thread "
                    + ownerThreadId + ", current thread is " + currentThreadId);
        }
    }
}
