package atf.ui.playwright.geometry;

import atf.testkit.AtfComponentTest;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.support.geometry.PointerGeometryFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Setup readiness and pointer actionability have independent oracles and unchanged, non-renewable limits. */
@AtfComponentTest
class PointerGeometryPreparationComponentTest {
    @ParameterizedTest @ValueSource(ints={70,100})
    void initializationLongerThanThePointerCapBelongsToPreparation(int zoom) throws Exception {
        try(var fixture=new PointerGeometryFixture(Duration.ofMillis(1200));
            var session=PlaywrightPointerGeometryComponentTest.open(fixture,zoom)) {
            var prepared=fixture.openReady(session.page());
            assertEquals(5,prepared.documents());
            assertEquals("CSS1Compat",session.page().evaluate("document.compatMode"),"The original standards-mode root remains unchanged");
            assertEquals("BackCompat",session.page().frameLocator("#nested").locator("body")
                    .evaluate("body=>body.ownerDocument.compatMode"),"Preparation preserves the original quirks-mode wrapper");
            assertTrue(prepared.elapsedMillis()>=1200,"The delayed fixture actually initialized before input");
            var image=ImageIO.read(new ByteArrayInputStream(prepared.screenshot()));
            assertTrue(Math.abs(image.getWidth()-1440)<=1);assertTrue(Math.abs(image.getHeight()-1000)<=1);
            var button=PlaywrightPointerGeometryComponentTest.target(session.page(),"cross","#activate");
            assertEquals("0",PlaywrightPointerGeometryComponentTest.target(session.page(),"cross","#clicks").innerText(),"Preparation cannot preflight or replay a pointer");
            var actions=session.newActionSequence();actions.click(new AtfLocator(button,"#cross >> #activate"),"one native activation after actual fixture rendering");
            assertEquals("1",PlaywrightPointerGeometryComponentTest.target(session.page(),"cross","#clicks").innerText());
            assertEquals("true",PlaywrightPointerGeometryComponentTest.target(session.page(),"cross","#trusted").innerText());
            assertEquals(1,actions.snapshot().size());assertEquals("PASS",actions.snapshot().getFirst().status().name());
        }
    }
    @Test void navigationTimeIsNotGrantedAgainToInitializationOrCapture() throws Exception {
        try(var fixture=new PointerGeometryFixture(Duration.ofMillis(2800),Duration.ofMillis(3000));
            var session=PlaywrightPointerGeometryComponentTest.open(fixture,100)) {
            var failure=assertThrows(IllegalStateException.class,()->fixture.openReady(session.page()));
            assertTrue(failure.getMessage().contains("5000ms navigation budget"));assertNotNull(failure.getCause());
            assertEquals("0",PlaywrightPointerGeometryComponentTest.target(session.page(),"same","#clicks").innerText(),"An unprepared page never dispatches a pointer");
            assertEquals(0,((Number)session.page().evaluate("window.childActivations")).intValue(),"No child frame emitted an activation during failed preparation");
        }
    }
    @Test void aMovingFixtureCannotAuthorizeInputThroughItsReadyMarkers() throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=PlaywrightPointerGeometryComponentTest.open(fixture,70)) {
            session.page().addInitScript("""
                if (window === window.top) addEventListener('DOMContentLoaded', () => {
                    let position = 0;
                    const move = () => {
                        document.getElementById('frames').style.marginLeft = (++position % 20) + 'px';
                        requestAnimationFrame(move);
                    };
                    requestAnimationFrame(move);
                });
                """);
            var failure=assertThrows(IllegalStateException.class,()->fixture.openReady(session.page()));
            assertNotNull(failure.getCause());
            assertEquals("true",session.page().locator("body").getAttribute("data-geometry-ready"));
            assertEquals(0,((Number)session.page().evaluate("window.childActivations")).intValue());
            assertEquals("0",PlaywrightPointerGeometryComponentTest.target(session.page(),"cross","#clicks").innerText());
        }
    }
}
