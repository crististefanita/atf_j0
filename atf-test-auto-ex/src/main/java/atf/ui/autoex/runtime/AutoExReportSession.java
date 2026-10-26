package atf.ui.autoex.runtime;

import atf.api.core.reporting.console.AtfConsoleSink;
import atf.api.reporting.configuration.ReportEnvironmentField;
import atf.api.reporting.configuration.ReportEnvironmentSnapshot;
import atf.api.reporting.evidence.profile.ReportEvidenceProfile;
import atf.api.reporting.ui.UiExtentReportOptions;
import atf.api.reporting.ui.UiExtentReportSession;
import atf.api.ui.config.UiBrowserConfiguration;

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
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.CLIENT, "Playwright browser · CSS zoom %d%%".formatted(browser.zoomPercent()))
                .put(ReportEnvironmentField.BROWSER, browser.browser())
                .put(ReportEnvironmentField.HEADLESS, Boolean.toString(browser.headless()))
                .put(ReportEnvironmentField.VIEWPORT, "%d × %d".formatted(browser.viewportWidth(), browser.viewportHeight()))
                .build();
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
