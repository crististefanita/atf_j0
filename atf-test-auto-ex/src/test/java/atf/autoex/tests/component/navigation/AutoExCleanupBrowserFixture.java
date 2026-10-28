package atf.autoex.tests.component.navigation;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.AccountPage;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.runtime.navigation.AutoExStoreUnavailableException;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Route;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static atf.testkit.assertions.AuditedAssertions.*;
import static atf.ui.config.UiRetryOperation.NAVIGATE;

/** Browser-only fixture for deterministic owned-cleanup contracts; no remote traffic. */
final class AutoExCleanupBrowserFixture implements AutoCloseable {
    final List<String> calls = new ArrayList<>();
    final PlaywrightUiSession browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
    final AutoExReportSession reports;
    final AutoExE2eRun run;
    final AutoExTestAccount account;
    int deletions;
    AutoExCleanupBrowserFixture(boolean foreignIdentity, boolean busyHome) throws Exception {
        var source = AutoExConfigurationLoader.load();
        account = AutoExTestAccount.create(source.registration());
        URI base = URI.create("https://autoex.cleanup.fixture.test/");
        browser.page().route(base + "**", route -> {
            String path = URI.create(route.request().url()).getPath();
            if (route.request().isNavigationRequest()) calls.add(route.request().method() + " " + path);
            boolean busy = path.equals("/payment") || busyHome && calls.size() > 1 && path.equals("/");
            String body;
            if (busy) body = "<h2>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h2>";
            else if (path.equals("/delete_account")) {
                deletions++;
                body = "<h2 data-qa='account-deleted'>ACCOUNT DELETED!</h2><a data-qa='continue-button' href='/'>Continue</a>";
            } else body = "<header id='header'><a><i class='fa-user'></i><b>"
                    + (foreignIdentity ? "Another customer" : account.signupName())
                    + "</b></a><a href='/delete_account'>Delete account</a></header>"
                    + "<form method='post' action='/payment'><button id='pay'>Pay once</button></form>";
            route.fulfill(new Route.FulfillOptions().setStatus(busy ? 503 : 200).setContentType("text/html").setBody(body));
        });
        var config = new AutoExConfiguration(base, false, UiBrowserConfiguration.defaults(),
                source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(),
                new UiRetryPolicy(3, Duration.ZERO, Set.of(NAVIGATE), "queue full"));
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.FINAL_AND_FAILURE);
        var output = Path.of(System.getProperty("atf.test.reportDirectory", "target/cleanup-reports")).toAbsolutePath();
        reports = AutoExReportSession.open(output.resolve("cleanup"), AutoExReportSession.fileName("owned-recovery"),
                AtfConsoleSink.noOp(), profile, config.browser());
        var scenario = reports.report().startScenario("OwnedCleanupFixture", getClass().getPackageName(),
                "owned cleanup after a failed payment document", List.of("component", "cleanup", "local-browser"));
        run = new AutoExE2eRun(config, browser, scenario, scenario.startPhase("TEST BODY"), profile, false,
                AutoExRunLifecycleAdapter.INSTANCE);
        run.navigate(base);
        new AccountPage(run).trackDisposableAccountCreation(account);
        run.accountLifecycle().creationConfirmed();
    }
    Throwable failPayment() {
        browser.page().locator("#pay").click();
        return assertThrows(AutoExStoreUnavailableException.class,
                () -> run.text("#not-rendered", "payment page must be available"));
    }
    String html() throws Exception { reports.close(); return Files.readString(reports.report().reportPath()); }
    @Override public void close() { try (reports; browser) { } }
}
