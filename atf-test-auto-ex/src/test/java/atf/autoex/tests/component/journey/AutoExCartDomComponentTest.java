package atf.autoex.tests.component.journey;

import atf.autoex.business.cart.AutoExCartFlow;
import atf.autoex.business.cart.AutoExCartValidationException;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.flow.cart.AutoExCartUiFlow;
import atf.autoex.page.CartPage;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.support.checkout.AutoExCheckoutAssertions;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.ui.failure.UiCheckpointFailure;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Business port → real page/recorder → Extent, with a controlled DOM and no remote mutations. */
@AutoExComponentTest
class AutoExCartDomComponentTest {
    @TempDir Path temporary;

    @Test
    void cartGateReadsTheObservedRowAndOnlyAPassingSnapshotCanEnterCheckout() throws Exception {
        var configuration = AutoExConfigurationLoader.load();
        var product = configuration.catalog();
        var expected = AutoExCheckoutAssertions.expectedLine(product, product.expectedTotal());
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.FINAL_AND_FAILURE);
        Path output = Path.of(System.getProperty("atf.test.reportDirectory", temporary.toString()))
                .toAbsolutePath().resolve("ui").resolve("auto-ex");
        Path reportPath;
        try (var reports = AutoExReportSession.open(output, AutoExReportSession.fileName("cart-gate-component"),
                AtfConsoleSink.noOp(), profile, configuration.browser())) {
            reportPath = reports.report().reportPath();
            for (boolean correct : List.of(true, false)) {
                var scenario = reports.report().startScenario("AutoExCartGate" + (correct ? "Positive" : "Negative"),
                        getClass().getPackageName(), correct ? "a valid cart enters checkout" : "a wrong price blocks checkout",
                        List.of("component", "controlled-dom", "cart-gate"));
                var browser = PlaywrightUiSession.open(configuration.browser(), AtfConsoleSink.noOp());
                var run = new AutoExE2eRun(configuration, browser, scenario, scenario.startPhase("TEST BODY"),
                        profile, false, AutoExRunLifecycleAdapter.INSTANCE);
                Throwable failure = null;
                try {
                    browser.page().setContent("""
                            <html><body><h1>Cart review</h1><table><tbody><tr id='product-%s'>
                            <td><h4>%s</h4></td><td class='cart_price'>%s</td>
                            <td class='cart_quantity'><button>%s</button></td><td class='cart_total'>%s</td>
                            </tr></tbody></table><a class='check_out' href='#checkout'>Proceed to checkout</a></body></html>
                            """.formatted(product.productId(), product.productName(),
                            correct ? product.unitPrice() : "Rs. 999", product.quantity(), product.expectedTotal()));
                    var cart = new AutoExCartUiFlow(new CartPage(run), product);
                    if (correct) {
                        var result = AutoExCartFlow.checkout(cart, expected, snapshot -> run.checkpoint(
                                AutoExPurchaseTraceStep.CART_VERIFIED, AutoExCheckoutAssertions.cart(snapshot)));
                        assertTrue(browser.page().url().endsWith("#checkout"));
                        run.finalScreenshot("the validated cart is ready for checkout", AutoExCheckoutAssertions.cart(result));
                    } else {
                        var rejected = assertThrows(AutoExCartValidationException.class, () -> AutoExCartFlow.checkout(
                                cart, expected, snapshot -> run.checkpoint(AutoExPurchaseTraceStep.CART_VERIFIED,
                                        AutoExCheckoutAssertions.cart(snapshot))));
                        failure = rejected;
                        assertInstanceOf(UiCheckpointFailure.class, rejected.getCause());
                        assertFalse(browser.page().url().contains("#checkout"));
                        assertEquals("Rs. 999", rejected.result().observed().unitPrice());
                    }
                } catch (Exception | Error unexpected) {
                    failure = unexpected;
                    throw unexpected;
                } finally {
                    run.finish(failure);
                }
            }
        }
        String html = Files.readString(reportPath).replaceAll(
                "(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertTrue(html.contains("Rs. 999"));
        assertTrue(html.contains("atf-ui-action-screenshot-link"));
        assertTrue(html.contains("Get text"));
        assertFalse(html.contains("FAILURE CAUSE"), "a published gate failure is not a second unrelated failure");
    }
}
