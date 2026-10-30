package atf.shop.tests.component.billing;

import atf.shop.config.model.ShopBillingAddress;
import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopBillingPage;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.catalog.ShopPostalLookupFixture;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopPostalLookupComponentTest {
    private static final ShopBillingAddress ADDRESS = new ShopBillingAddress("US","99501","10","Fixture street","Fixture city","Fixture state");

    @ParameterizedTest @ValueSource(strings={"current","same"})
    void onlyTheCompletedCurrentLookupCanConfirmTheAddress(String mode) throws Exception {
        try(var fixture=new ShopPostalLookupFixture(mode);var session=open(fixture)) {
            var page=session.page();page.navigate(fixture.home().toString());var actions=session.newActionSequence();
            var observed=new ShopBillingPage(page,new ShopLocators(),actions).fillFromPostalLookup(ADDRESS,fixture.endpoint());
            assertEquals(mode.equals("same")?"Old state":"New state",observed.state());
            assertEquals("1",fixture.completions(page));
            assertEquals(1,fixture.requests.get());assertEquals(1,((Number)page.evaluate("window.inputs")).intValue());
            assertTrue(actions.snapshot().stream().anyMatch(a->a.operation().equals("Shop address lookup")&&a.status().name().equals("PASS")));
        }
    }
    @ParameterizedTest @ValueSource(strings={"missing","stale-only","wrong-query","http-error","malformed","blank","duplicate-query","foreign-origin","transport"})
    void oldOrUnconfirmedAddressNeverProducesAPass(String mode) throws Exception {
        try(var fixture=new ShopPostalLookupFixture(mode);var session=open(fixture)) {
            var page=session.page();page.navigate(fixture.home().toString());
            if(mode.equals("stale-only")) {page.evaluate("window.startOld()");page.waitForCondition(()->fixture.requests.get()==1);}
            if(mode.equals("transport")) page.route("**/postcode-lookup?**",route->route.abort("connectionfailed"));
            var actions=session.newActionSequence();
            var endpoint=mode.equals("foreign-origin")?java.net.URI.create(fixture.endpoint().toString().replace("127.0.0.1","localhost")):fixture.endpoint();
            assertThrows(RuntimeException.class,()->new ShopBillingPage(page,new ShopLocators(),actions).fillFromPostalLookup(ADDRESS,endpoint));
            assertTrue(actions.snapshot().stream().noneMatch(a->a.operation().equals("Shop address lookup")&&a.status().name().equals("PASS")));
            assertTrue(actions.snapshot().stream().anyMatch(a->a.status().name().equals("FAIL")));
            assertEquals(1,((Number)page.evaluate("window.inputs")).intValue());
            assertFalse(actions.snapshot().toString().contains("PRIVATE_POSTCODE_CANARY"));
        }
    }
    private PlaywrightUiSession open(ShopPostalLookupFixture fixture) {
        return PlaywrightUiSession.open(new UiBrowserConfiguration(fixture.home(),"chromium",true,
                Duration.ofSeconds(2),Duration.ofSeconds(5),1280,900,100,true),event->{
                    if(event.type()==atf.core.reporting.console.AtfConsoleEvent.Type.START
                            &&event.name().startsWith("Await visible ->")) fixture.release();
                });
    }
}
