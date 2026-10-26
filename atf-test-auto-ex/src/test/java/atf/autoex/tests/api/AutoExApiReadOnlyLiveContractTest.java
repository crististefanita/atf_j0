package atf.autoex.tests.api;

import atf.console.junit.AtfConsoleTestExtension;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.reporting.AtfReportPath;
import atf.autoex.api.AutoExApiJsonField;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.liveExecutor;
import atf.autoex.api.AutoExApiRequestFactory;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static atf.autoex.tests.support.api.AutoExApiTestSupport.assertResponse;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.assertSearchResponse;

/** Read-only public API sample; safe to run without creating or changing accounts. */
@AtfIntegrationTest
@AtfTestChannel(TestChannel.API)
@EnabledIfSystemProperty(named = "atf.autoex.api.readonly.live.enabled", matches = "true")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExApiContractReportExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
public class AutoExApiReadOnlyLiveContractTest {
    @Test
    void readOnlyPublishedEndpointsProduceLiveExtentEvidence() {
        AutoExConfiguration configuration = AutoExConfigurationLoader.load();
        var api = liveExecutor(configuration);
        var requests = new AutoExApiRequestFactory();

        assertResponse("API 1 products list", api,
                requests.productsList(), 200, null, AutoExApiJsonField.PRODUCTS);
        assertResponse("API 3 brands list", api,
                requests.brandsList(), 200, null, AutoExApiJsonField.BRANDS);
        assertSearchResponse("API 5 product search", api,
                requests.searchProduct(configuration.catalog().searchTerm()), 200,
                configuration.catalog().searchTerm());
    }
}
