package atf.shop.tests.component.billing;

import atf.shop.config.model.ShopBillingAddress;
import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopBillingPage;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.billing.ShopBillingLookupSite;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopBillingSynchronizationComponentTest {
    @ParameterizedTest(name="billing protocol · {0} · configured={1}")
    @CsvSource({"race,true,true","ready,false,true","pending,false,true","unchanged,false,true","duplicate,false,true",
            "ready,true,true","no-lookup,true,true","no-lookup,false,false","wrong-query,false,false","error-status,false,false",
            "schema,false,false","old-late,false,false","document,false,false"})
    void onlyTheCurrentAppliedLookupCanPrecedeObservationOrConfiguredInput(String mode, boolean configured, boolean succeeds) throws Exception {
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofMillis(1100), Duration.ofSeconds(3), 1440, 1000, 70, true);
        try (var browser = PlaywrightUiSession.open(configuration);
             var site = new ShopBillingLookupSite(browser.page(), mode)) {
            var actions = browser.newActionSequence();
            {
                var billing = new ShopBillingPage(site.page(), new ShopLocators(), actions);
                site.open();
                var address = new ShopBillingAddress("United States of America (the)", "53703", "10",
                        "Wayne Lock", "Cartwrightmouth", "Alaska");
                Runnable operation = () -> { if (configured) billing.fill(address, site.endpoint()); else billing.fillFromPostalLookup(address, site.endpoint()); };
                if (succeeds) {
                    assertDoesNotThrow(operation::run);
                    assertEquals(List.of("Wayne Lock", "Cartwrightmouth", "Alaska"), site.address(), "Expected fixture values stay independent of the observed response");
                    assertEquals(site.requests(), site.completed(), "No current or earlier owned lookup remains in flight");
                    assertTrue(mode.equals("no-lookup") && configured || site.applied() > 0, "Application completion, rather than pre-existing non-empty fields, is required");
                    if (mode.equals("duplicate")) assertEquals(2, site.requests(), "Both delayed current-tuple requests must settle");
                } else {
                    var failure = assertThrows(RuntimeException.class, operation::run);
                    assertNotNull(failure.getCause(), "The original lookup or deadline cause stays inspectable");
                    assertEquals("FAIL", actions.snapshot().getLast().status().name());
                }
                for (String field : List.of("street", "city", "state")) assertEquals(configured && succeeds ? 1 : 0,
                        site.inputs(field), "Generated fields are never cleared or retyped during lookup observation");
                assertFalse(site.state().equals("AlaskaAlaska"), "The configured state must not append to an asynchronously completed lookup");
                if (mode.equals("no-lookup")) assertEquals(0, site.requests(), "Old non-empty values cannot trigger an invented lookup");
            }
        }
    }
}
