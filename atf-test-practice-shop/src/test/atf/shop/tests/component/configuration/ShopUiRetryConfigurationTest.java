package atf.shop.tests.component.configuration;

import atf.shop.tests.support.ShopComponentTest;
import atf.ui.config.UiRetryOperation;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopUiRetryConfigurationTest {
    @Test
    void loadsTypedReadOnlyRetryFromYamlAndKeepsMutationsIneligible() throws Exception {
        Path root = Files.createTempDirectory("practice-shop-retry-");
        Path yaml = root.resolve("practice-shop.yaml");
        Files.writeString(yaml, """
                practiceShop:
                  baseUrl: https://practicesoftwaretesting.com
                  retry:
                    maxAttempts: 3
                    backoff: PT0S
                    eligibleOperations: [wait-for-visible, wait-for-text]
                    reason: DOM stabilization
                """);

        ShopConfiguration configuration = ShopConfigurationLoader.load(yaml);

        assertTrue(configuration.uiRetry().enabled());
        assertEquals(3, configuration.uiRetry().maxAttempts());
        assertEquals("DOM stabilization", configuration.uiRetry().reason());
        assertTrue(configuration.uiRetry().eligibleOperations()
                .contains(UiRetryOperation.WAIT_FOR_VISIBLE));
        assertTrue(configuration.browser().retryPolicy().allows("Wait for visible", 1));
        assertFalse(configuration.browser().retryPolicy().allows("Click", 1));
        assertEquals("enabled; attempts=3; backoff=PT0S; eligible=wait-for-text,wait-for-visible; reason=DOM stabilization",
                configuration.uiRetry().summary());
    }
}
