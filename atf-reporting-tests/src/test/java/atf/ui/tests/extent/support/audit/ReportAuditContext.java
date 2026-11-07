package atf.ui.tests.extent.support.audit;

import atf.reporting.browser.lifecycle.BrowserProbeScope;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.reporting.api.tests.support.console.ReportingTestConsole;
import atf.reporting.ui.audit.input.ReportAuditInputKind;
import atf.debugui.DebugUiConfiguration;
import atf.debugui.DebugUiServer;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.artifact.ReportArtifactIdentity;
import atf.reporting.ui.UiExtentReportSession;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.config.environment.UiReportEnvironmentSnapshotFactory;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.ReportingBrowserSession;
import atf.ui.playwright.UiScreenValidation;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.ui.contract.ReportUiContract;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Objects;

/** Shared read-only lifecycle for generated and existing Extent report audits. */
public final class ReportAuditContext implements AutoCloseable {
    private final ReportAuditInput input;
    private final Path inputReport;
    private final UiExtentReportSession auditSession;
    private final DebugUiServer server;
    private final ReportingBrowserSession browser;
    private final ReportUiLocators locators = new ReportUiLocators();
    private final CopyOnWriteArrayList<String> consoleErrors;
    private final CopyOnWriteArrayList<String> pageErrors;
    private final CopyOnWriteArrayList<String> requestFailures;

    private ReportAuditContext(ReportAuditInput input, UiExtentReportSession auditSession,
                               DebugUiServer server, ReportingBrowserSession browser,
                               CopyOnWriteArrayList<String> consoleErrors,
                               CopyOnWriteArrayList<String> pageErrors,
                               CopyOnWriteArrayList<String> requestFailures) {
        this.input = Objects.requireNonNull(input, "input");
        this.inputReport = input.report();
        this.auditSession = auditSession;
        this.server = server;
        this.browser = browser;
        this.consoleErrors = consoleErrors;
        this.pageErrors = pageErrors;
        this.requestFailures = requestFailures;
    }

    public static ReportAuditContext open(UiConfiguration configuration, Path inputReport,
                                          String auditSuffix) {
        return open(configuration, ReportAuditInput.existing(inputReport), auditSuffix);
    }

    /** Opens either a freshly generated API artifact or an existing report read-only. */
    public static ReportAuditContext open(UiConfiguration configuration, ReportAuditInput input,
                                          String auditSuffix) {
        Objects.requireNonNull(configuration, "configuration");
        ReportAuditInput resolved = Objects.requireNonNull(input, "input");
        Path inputReport = resolved.report();
        if (!Files.isRegularFile(inputReport)) {
            throw new IllegalArgumentException("Extent input report does not exist: " + inputReport);
        }
        var inspection = ReportArtifactInspector.inspect(inputReport);
        boolean readableHtml = inspection.readError().isEmpty()
                && inspection.satisfies("html exists")
                && inspection.satisfies("doctype");
        if (!readableHtml) {
            throw new IllegalArgumentException("Extent input report is not readable: "
                    + inspection.violations());
        }

        String auditFileName = configuration.reserveTimestampedReportPath(auditSuffix,
                Clock.systemDefaultZone()).getFileName().toString();
        var console = ReportingTestConsole.configured();
        UiExtentReportSession audit = UiExtentReportSession.openConfiguredResolved(
                configuration.reportDirectory(), auditFileName,
                console.reporting(),
                UiReportEnvironmentSnapshotFactory.audit(configuration,
                        "Extent UI audit", "artifact inspection"));
        DebugUiServer debugUi = null;
        ReportingBrowserSession browser = null;
        try {
            debugUi = DebugUiServer.start(DebugUiConfiguration.forHtml(inputReport, 0));
            browser = BrowserResources.create(() -> ReportingBrowserSession.open(configuration.browser(), console.core()));
            CopyOnWriteArrayList<String> consoleErrors = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<String> pageErrors = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<String> requestFailures = new CopyOnWriteArrayList<>();
            browser.page().onConsoleMessage(message -> {
                if ("error".equalsIgnoreCase(message.type())) {
                    consoleErrors.add(message.text());
                }
            });
            browser.page().onPageError(error -> pageErrors.add(String.valueOf(error)));
            browser.page().onRequestFailed(request -> requestFailures.add(
                    request.method() + " " + request.url() + " :: " + request.failure()));
            browser.navigate(debugUi.uriFor(inputReport));
            return new ReportAuditContext(resolved, audit, debugUi, browser,
                    consoleErrors, pageErrors, requestFailures);
        } catch (RuntimeException failure) {
            BrowserProbeScope.recordFailure(failure);
            try { BrowserResources.closeAll(browser, debugUi, audit); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public Path inputReport() { return inputReport; }

    /** Identity shown in UI setup evidence so the UI→API pair is auditable. */
    public ReportArtifactIdentity inputArtifactIdentity() {
        return ReportArtifactIdentity.read(inputReport);
    }

    public ReportAuditInput input() { return input; }

    public ReportAuditInputKind inputKind() { return input.kind(); }

    /** Browser diagnostics collected for the whole read-only audit lifecycle. */
    public BrowserDiagnostics browserDiagnostics() {
        return new BrowserDiagnostics(consoleErrors, pageErrors, requestFailures);
    }

    public Path reportPath() { return auditSession.reportPath(); }

    public UiExtentReportSession.UiScenario startScenario(String testClass, String testPackage,
                                                           String scenario, List<String> tags) {
        return auditSession.startScenario(testClass, testPackage, scenario, tags);
    }

    public PlaywrightActionSequence newActionSequence() { return browser.newActionSequence(); }

    /** Observes tab selection without leaking the browser into a declarative test. */
    public boolean isTabSelected(ReportUiContract.Tab tab) {
        Objects.requireNonNull(tab, "tab");
        return browser.page().url().endsWith(tab.hash())
                && locators.tabView(browser.page(), tab).isVisible();
    }

    /** Read-only evidence access; never reloads the page or collapses the inspected subtree. */
    public Page selectedTab(ReportUiContract.Tab tab) {
        if (!isTabSelected(tab)) {
            throw new IllegalStateException("Report evidence tab is not selected: " + tab);
        }
        return browser.page();
    }

    /** Captures the current page only when the configured failure policy enables it. */
    public UiScreenValidation.Evidence captureFailure(UiConfiguration configuration, String title) {
        Objects.requireNonNull(configuration, "configuration");
        return UiScreenValidation.capture(browser.page(),
                new UiScreenValidationConfiguration(configuration.browser().screenshotOnFailure(),
                        configuration.screenValidation().fullPage(),
                        configuration.screenValidation().screenshotTimeout()), title);
    }

    public Page openTab(String hash, PlaywrightActionSequence actions) {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(actions, "actions");
        Page page = browser.page();
        ReportUiContract.Tab tab = locators.tabByHash(hash);
        // Load the shell first and activate the tab through the report's own
        // navigation contract.  A hash supplied during the first document
        // load is not reliable across the Extent vendor runtime: the target
        // view can remain hidden even though the same hash works after the
        // navigation script has initialized.  This keeps every section
        // module deterministic regardless of execution order.
        actions.navigate(page, URI.create(server.uriFor(inputReport).toString()),
                "API Extent report " + inputReport.getFileName() + " is loaded");
        actions.waitForVisible(locators.runtimeRoot(page), "html");
        actions.waitForVisible(locators.navTabAction(page, tab));
        actions.click(locators.navTabAction(page, tab), "open " + tab.name() + " tab");
        actions.waitForVisible(locators.tabView(page, tab), hash);
        actions.observation("Tab visible", hash, "selected tab is visible", hash);
        return page;
    }

    @Override
    public void close() {
        BrowserResources.closeAll(browser, server, auditSession);
    }

    public record BrowserDiagnostics(List<String> consoleErrors,
                                    List<String> pageErrors,
                                    List<String> requestFailures) {
        public BrowserDiagnostics {
            consoleErrors = List.copyOf(consoleErrors == null ? List.of() : consoleErrors);
            pageErrors = List.copyOf(pageErrors == null ? List.of() : pageErrors);
            requestFailures = List.copyOf(requestFailures == null ? List.of() : requestFailures);
        }

        public boolean valid() {
            return consoleErrors.isEmpty() && pageErrors.isEmpty() && requestFailures.isEmpty();
        }

        public List<String> violations() {
            return java.util.stream.Stream.of(
                            consoleErrors.stream().map(value -> "console error: " + value),
                            pageErrors.stream().map(value -> "page error: " + value),
                            requestFailures.stream().map(value -> "request failed: " + value))
                    .flatMap(stream -> stream)
                    .toList();
        }
    }
}
