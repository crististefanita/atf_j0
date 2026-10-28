package atf.shop.tests.api.integration;

import atf.console.AtfConsoleReporter;
import atf.console.junit.AtfConsoleTestExtension;
import atf.core.http.request.HttpMethod;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.model.ShopCredentials;
import atf.shop.support.api.run.ShopApiBusinessRun;
import atf.shop.support.api.run.ShopApiRunReport;
import atf.shop.tests.api.support.ShopApiReportMetadataFixture;
import atf.shop.tests.support.ShopReportHtmlTestSupport;
import atf.shop.tests.support.ShopTestSupport;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.extent.session.ExtentReportSession;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.AtfConsoleMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Live API oracle; run-owned execution, cart cleanup and publication belong to main. */
@Tag("api")
@AtfIntegrationTest
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@ExtendWith(AtfConsoleTestExtension.class)
@ShopTestSupport.RequiresLiveAuthenticatedApi
public class ShopStandaloneApiReportTest {
    @Test
    @DisplayName("Practice Shop live API business flow publishes real HTTP evidence")
    void publishesLiveApiBusinessEvidence() throws Exception {
        var configuration = ShopConfigurationLoader.load();
        var report = configuration.apiReportDirectory().resolve("api-atf-practice-shop-standalone_"
                + Instant.now().toString().replace(":", "-") + ".html");
        var run = new ShopApiBusinessRun(() -> ShopApiClient.live(configuration.api()),
                () -> new ShopApiRunReport(new ExtentReportSession(report,
                        AtfReportConfigurationLoader.load().reportingSettings()),
                        ShopApiReportMetadataFixture.LIVE_TEST_CLASS, ShopApiReportMetadataFixture.LIVE_TEST_PACKAGE,
                        ShopApiReportMetadataFixture.LIVE_SCENARIO, ShopApiReportMetadataFixture.LIVE_TAGS,
                        true, AtfConsoleReporter.configuredStdout()), true);
        var result = run.execute(new ShopApiBusinessRun.Inputs(configuration.productName(),
                configuration.checkout().quantity(), configuration.checkout().billing(),
                ShopPaymentMethod.from(configuration.checkout().paymentMethod())),
                () -> ShopCredentials.load(configuration.accountFile()));

        assertTrue(Files.isRegularFile(report), "live API report must be written");
        String html = Files.readString(report);
        ShopReportHtmlTestSupport.requireText(html, "LIVE / HTTP", "live provenance");
        if (result.passed()) {
            for (String contract : List.of("Authentication business contract", "Catalog business contract",
                    "Live category and price-sort contract", "Cart add business contract",
                    "Live cart mutation and cleanup contract", "Checkout business contract")) {
                ShopReportHtmlTestSupport.requireText(html, contract, "business validation");
            }
            ShopReportHtmlTestSupport.requireText(html, "REQUEST JSON", "request body disclosure");
            ShopReportHtmlTestSupport.requireText(html, "RESPONSE JSON", "response body disclosure");
            assertTrue(html.contains("Payment was successful") || html.contains("payment_method"),
                    "payment business evidence must remain visible");
            ShopReportHtmlTestSupport.requireText(html, HttpMethod.POST.value(), "write operations must remain visible");
        }
        assertTrue(result.passed(), "all live business contracts must pass; inspect the generated report for evidence");
    }
}
