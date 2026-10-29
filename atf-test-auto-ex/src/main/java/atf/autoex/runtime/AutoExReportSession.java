package atf.autoex.runtime;

import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.autoex.runtime.reporting.AutoExReportSystemMetadata;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;
import atf.ui.config.UiBrowserConfiguration;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Consumer-owned report resource; its lifetime is chosen by the caller. */
public final class AutoExReportSession implements AutoCloseable {
    private static final String PREFIX = "ui-atf-auto-ex-purchase-suite_";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private final UiExtentReportSession report;

    private AutoExReportSession(UiExtentReportSession report) { this.report = report; }

    public static AutoExReportSession open(Path output, String name, AtfConsoleSink console,
                                           ReportEvidenceProfile evidence, UiBrowserConfiguration browser) {
        return openWithEnvironment(output, name, console, evidence, AutoExReportSystemMetadata.browserOnly(browser, evidence));
    }

    public static AutoExReportSession openWithEnvironment(Path output, String name, AtfConsoleSink console,
                                                           ReportEvidenceProfile evidence, ReportEnvironmentSnapshot environment) {
        return new AutoExReportSession(UiExtentReportSession.openConfigured(UiExtentReportOptions.builder(output)
                .fileName(name).console(AutoExConsoleBridge.reportingSink(console))
                .evidenceProfile(evidence).environment(environment).build()));
    }

    public static String fileName(String method) {
        return PREFIX + method.replaceAll("[^A-Za-z0-9_-]", "-") + "_"
                + TIME.format(LocalDateTime.now()) + "_" + UUID.randomUUID().toString().substring(0, 8) + ".html";
    }

    public UiExtentReportSession report() { return report; }
    @Override public void close() { report.close(); }
}
