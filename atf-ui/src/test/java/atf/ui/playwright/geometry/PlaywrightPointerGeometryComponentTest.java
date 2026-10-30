package atf.ui.playwright.geometry;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.*;
import atf.ui.tests.support.geometry.PointerGeometryFixture;
import com.microsoft.playwright.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import atf.ui.config.UiScreenValidationConfiguration;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.List;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class PlaywrightPointerGeometryComponentTest {
    @ParameterizedTest @CsvSource({"same,25","same,70","same,100","same,125","same,200",
            "cross,25","cross,70","cross,100","cross,125","cross,200",
            "nested,25","nested,70","nested,100","nested,125","nested,200"})
    void nativePointerAtConfiguredScaleRetainsItsActualTargetAndTrust(String frame,int zoom) throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,zoom)){
            var page=session.page();fixture.openReady(page);
            var target=target(page,frame,"#activate");
            var actions=session.newActionSequence();actions.click(new AtfLocator(target,"#"+frame+" >> #activate"),"one native activation");
            assertEquals("1",target(page,frame,"#clicks").innerText());
            assertEquals("true",target(page,frame,"#trusted").innerText());
            assertEquals("Click",actions.snapshot().getFirst().operation());
            assertEquals(1,actions.snapshot().size());
        }
    }
    @ParameterizedTest @ValueSource(strings={"chromium","firefox","webkit"})
    void eachSupportedEngineUsesTrustedPointerInputAtSeventyPercent(String engine) throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70,engine)){
            var page=session.page();fixture.openReady(page);
            target(page,"cross","#activate").click();
            assertEquals("1",target(page,"cross","#clicks").innerText());
            assertEquals("true",target(page,"cross","#trusted").innerText());
            assertEquals(.7,((Number)page.evaluate("devicePixelRatio")).doubleValue(),.000001);
        }
    }
    @ParameterizedTest @ValueSource(strings={"same","cross","nested"})
    void pointerFocusAndTrustedKeyboardReachTheSameFrame(String frame) throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);
            var input=target(page,frame,"#entry");var actions=session.newActionSequence();
            var element=new AtfLocator(input,"#"+frame+" >> #entry");
            actions.click(element,"input owns browser focus");
            for(char key:"geometry".toCharArray())actions.pressKey(page,String.valueOf(key),element,"trusted child key");
            assertEquals("geometry",input.inputValue());assertEquals("8",target(page,frame,"#keys").innerText());
            assertEquals(true,input.evaluate("node=>node===node.ownerDocument.activeElement"));
            assertEquals(9,actions.snapshot().size());
        }
    }
    @ParameterizedTest @CsvSource({"25,50","70,140","100,200","125,250","200,400"})
    void screenshotPixelsProveScaleRatherThanOnlyADeclaredSetting(int zoom,int expectedRedWidth) throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,zoom)){
            var page=session.page();fixture.openReady(page);
            var evidence=UiScreenValidation.capture(page,UiScreenValidationConfiguration.defaults(),"geometry capture");
            var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(evidence.base64Png())));
            assertTrue(Math.abs(image.getWidth()-1440)<=1);assertTrue(Math.abs(image.getHeight()-1000)<=1);
            int red=0,row=(int)Math.round(150*zoom/100d);
            for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,row)&0xffffff)==0xff0000)red++;
            assertTrue(Math.abs(red-expectedRedWidth)<=1,"Independent 200-CSS-pixel marker must have the configured physical width: "+red);
            assertEquals("",page.evaluate("document.documentElement.style.zoom"));
            assertEquals("",page.evaluate("document.documentElement.style.transform"));
            assertEquals("keep",page.evaluate("document.documentElement.style.getPropertyValue('--fixture-owner')"));
        }
    }
    @ParameterizedTest @ValueSource(strings={"same","cross","nested"})
    void anOverlayStillRefusesPointerActivation(String frame) throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);
            page.evaluate("document.getElementById('cover').style.display='block'");
            var actions=session.newActionSequence();
            assertThrows(PlaywrightException.class,()->actions.click(new AtfLocator(target(page,frame,"#activate"),"#"+frame+" >> #activate"),"blocked pointer"));
            assertEquals("0",target(page,frame,"#clicks").innerText());
            assertEquals(0,((Number)page.evaluate("window.childActivations")).intValue());
            assertEquals("FAIL",actions.snapshot().getFirst().status().name());assertEquals(1,actions.snapshot().size());
        }
    }
    @Test void disabledAndRemovedFramesNeverAuthorizeAnotherActivation() throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);var actions=session.newActionSequence();
            assertThrows(PlaywrightException.class,()->actions.click(new AtfLocator(target(page,"cross","#disabled"),"#cross >> #disabled"),"disabled pointer"));
            var removed=target(page,"cross","#activate");page.evaluate("document.getElementById('cross').remove()");
            assertThrows(PlaywrightException.class,()->actions.click(new AtfLocator(removed,"#cross >> #activate"),"detached frame"));
            assertEquals(0,((Number)page.evaluate("window.childActivations")).intValue());
            assertEquals(2,actions.snapshot().size());assertTrue(actions.snapshot().stream().allMatch(a->a.status().name().equals("FAIL")));
        }
    }
    @Test void fixedLayoutAndPointerRemainConsistentAfterScrolling() throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);
            page.evaluate("document.getElementById('frames').style.paddingTop='1800px'");
            page.mouse().wheel(0,900);page.waitForCondition(()->((Number)page.evaluate("scrollY")).doubleValue()>0);
            target(page,"cross","#activate").click();
            assertTrue(((Number)page.evaluate("scrollY")).doubleValue()>0);
            assertEquals("true",target(page,"cross","#trusted").innerText());
            var image=ImageIO.read(new ByteArrayInputStream(page.screenshot()));
            assertEquals(0x0000ff,image.getRGB(image.getWidth()-20,5)&0xffffff);
        }
    }
    @Test void captureMasksUseTheSameGeometryAsTheNativePointer() throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);
            var evidence=UiScreenValidation.capture(page,UiScreenValidationConfiguration.defaults(),"masked child",List.of(target(page,"same","#entry")));
            var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(evidence.base64Png())));
            assertEquals(0x343a40,image.getRGB(228,276)&0xffffff,"Mask must cover the declared child input, not a translated neighbour");
        }
    }
    @Test void navigationAndNewPagesInheritScaleWithoutAnInitScript() throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);page.navigate(fixture.home().resolve("?next=1").toString());
            var popup=page.waitForPopup(()->page.locator("#popup").click());
            try {assertEquals(.7,((Number)popup.evaluate("devicePixelRatio")).doubleValue(),.000001);
                assertEquals(2057,popup.viewportSize().width);assertEquals("",popup.evaluate("document.documentElement.style.zoom"));
            } finally {popup.close();}
        }
    }
    @Test void fullPageCaptureKeepsDocumentHeightInsteadOfCroppingToTheOutputViewport() throws Exception {
        try(var fixture=new PointerGeometryFixture();var session=open(fixture,70)){
            var page=session.page();fixture.openReady(page);
            var evidence=UiScreenValidation.capture(page,new UiScreenValidationConfiguration(true,true),"full-page geometry");
            var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(evidence.base64Png())));
            assertTrue(Math.abs(image.getWidth()-1440)<=1);assertTrue(image.getHeight()>1500);
        }
    }
    static Locator target(Page page,String frame,String selector){
        var scope=page.frameLocator("#"+frame);
        return frame.equals("nested")?scope.frameLocator("#inner").locator(selector):scope.locator(selector);
    }
    static PlaywrightUiSession open(PointerGeometryFixture fixture,int zoom){
        return open(fixture,zoom,"chromium");
    }
    static PlaywrightUiSession open(PointerGeometryFixture fixture,int zoom,String engine){
        return PlaywrightUiSession.open(new UiBrowserConfiguration(fixture.home(),engine,true,
                Duration.ofMillis(850),PointerGeometryFixture.NAVIGATION_TIMEOUT,1440,1000,zoom,true));
    }
}
