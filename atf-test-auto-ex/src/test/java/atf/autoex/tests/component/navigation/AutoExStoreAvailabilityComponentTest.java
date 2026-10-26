package atf.autoex.tests.component.navigation;

import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightUiSession;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.runtime.navigation.AutoExReadOnlyNavigation;
import atf.autoex.runtime.navigation.AutoExStoreUnavailableException;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static atf.ui.config.UiRetryOperation.NAVIGATE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Actual document responses prove recovery and the non-replay boundary without remote traffic. */
@AtfComponentTest
class AutoExStoreAvailabilityComponentTest {
    private static final UiRetryPolicy POLICY = new UiRetryPolicy(3, Duration.ZERO, Set.of(NAVIGATE), "queue full");

    @Test
    void runtimeRecoversBusyProductsBeforeReadingAndPublishesEveryAttempt() throws Exception {
        var source = AutoExConfigurationLoader.load();
        try (var browser = PlaywrightUiSession.open(source.browser());
             var site = new AutoExAvailabilityFixture(browser.page(), 1)) {
            var configuration = new AutoExConfiguration(site.uri("/"), false, source.browser(),
                    source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(), POLICY);
            var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                    ReportScreenshotPolicy.FINAL_AND_FAILURE);
            var output = Path.of(System.getProperty("atf.test.reportDirectory", "target/availability-reports"))
                    .toAbsolutePath().resolve("ui");
            Path reportPath;
            try (var reports = AutoExReportSession.open(output,
                    AutoExReportSession.fileName("store-availability-contract"), AtfConsoleSink.noOp(),
                    profile, configuration.browser())) {
                reportPath = reports.report().reportPath();
                var scenario = reports.report().startScenario("AutoExStoreAvailabilityComponentTest",
                        getClass().getPackageName(), "the store recovers from queue full through an observed GET",
                        List.of("component", "intercepted-document", "store-availability"));
                var phase = scenario.startPhase("TEST BODY");
                var run = new AutoExE2eRun(configuration, browser, scenario, phase, profile, false,
                        AutoExRunLifecycleAdapter.INSTANCE);
                try {
                    run.navigate(site.uri("/products?search=Blue%20Top"));
                    String actual = run.text("#ready", "recovered catalog heading is read");
                    var checks = List.of(new AtfAssertion("Recovered catalog", "Catalog ready", actual,
                            "Catalog ready".equals(actual)));
                    run.finalScreenshot("the recovered read-only catalog is visible", checks);
                    run.finish(null);
                } catch (Exception | Error failure) {
                    run.finish(failure);
                    throw failure;
                }
            }
            assertEquals(List.of("GET /products?search=Blue%20Top", "GET /products?search=Blue%20Top"), site.calls());
            String html = Files.readString(reportPath).replaceAll(
                    "(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
            assertTrue(html.contains("queue full; document attempt 1/3"));
            assertTrue(html.contains("read-only store document recovers after queue full"));
            assertTrue(html.contains("atf-ui-action-screenshot-link"));
            assertFalse(html.contains("data-atf-evidence-status=\"fail\""));
        }
    }

    @Test
    void persistentQueueExhaustsExactlyTheConfiguredDocumentBudget() throws Exception {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
             var site = new AutoExAvailabilityFixture(browser.page(), -1)) {
            var actions = browser.newActionSequence();
            var recovery = new AutoExReadOnlyNavigation(browser.page(), actions, site.uri("/"), POLICY);
            actions.navigate(browser.page(), site.uri("/product_details/1"));
            var failure = assertThrows(AutoExStoreUnavailableException.class, recovery::recoverIfBusy);
            assertTrue(failure.getMessage().contains("document attempt 3/3"));
            assertTrue(failure.getMessage().contains("read-only recovery exhausted"));
            assertEquals(3, site.calls().size());
            assertEquals(3, actions.snapshot().stream().filter(action -> action.operation().equals("Navigate")).count());
        }
    }

    @Test
    void unsafeGetsAndPostDocumentsAreNeverReplayed() throws Exception {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
             var site = new AutoExAvailabilityFixture(browser.page(), -1)) {
            var page = browser.page();
            var actions = browser.newActionSequence();
            var recovery = new AutoExReadOnlyNavigation(page, actions, site.uri("/"), POLICY);
            for (String path : List.of("/delete_account", "/add_to_cart/1", "/checkout", "/logout")) {
                int before = site.calls().size();
                actions.navigate(page, site.uri(path));
                var failure = assertThrows(AutoExStoreUnavailableException.class, recovery::recoverIfBusy);
                assertTrue(failure.getMessage().contains("not an eligible read-only GET"));
                assertEquals(before + 1, site.calls().size(), path);
            }
            int before = site.calls().size();
            page.setContent("<form method='post' action='" + site.uri("/products")
                    + "'><button id='post'>Submit once</button></form>");
            actions.click(new AtfLocator(page.locator("#post"), "#post"), "the local POST is submitted once");
            var failure = assertThrows(AutoExStoreUnavailableException.class, recovery::recoverIfBusy);
            assertTrue(failure.getMessage().contains("POST /products"));
            assertEquals(before + 1, site.calls().size());
            assertEquals("POST /products", site.calls().getLast());
        }
    }

    @Test
    void ordinaryMissingElementsAndForeignOriginsDoNotTriggerRecovery() throws Exception {
        try (var browser = PlaywrightUiSession.open(UiBrowserConfiguration.defaults());
             var ready = new AutoExAvailabilityFixture(browser.page(), 0);
             var foreign = new AutoExAvailabilityFixture(browser.page(), -1, "foreign.fixture.test")) {
            var page = browser.page();
            var actions = browser.newActionSequence();
            var recovery = new AutoExReadOnlyNavigation(page, actions, ready.uri("/"), POLICY);
            actions.navigate(page, ready.uri("/products"));
            assertDoesNotThrow(recovery::recoverIfBusy);
            assertEquals(0, page.locator("#missing-product").count());
            assertEquals(1, ready.calls().size());
            actions.navigate(page, foreign.uri("/products"));
            assertThrows(AutoExStoreUnavailableException.class, recovery::recoverIfBusy);
            assertEquals(1, foreign.calls().size());
        }
    }
}
