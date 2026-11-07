package atf.ui.tests.extent.component.screenshot;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.support.screenshot.ScreenshotViewerFixture;
import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** End-to-end browser contract with mocked capture inputs in both native renderers. */
@AtfComponentTest
@ReportBrowserTest
class ScreenshotViewerBrowserComponentTest {
    @ParameterizedTest @ValueSource(booleans={true,false})
    void deduplicatedCaptureKeepsOwnerDisclosuresScrollAndFocus(boolean extent)throws Exception {
        var file=ScreenshotViewerFixture.publish(extent);
        try(var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value()));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value()))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(5000);page.setViewportSize(1280,800);var errors=new ArrayList<String>();page.onPageError(errors::add);
                page.route("http://**/*",Route::abort);page.route("https://**/*",Route::abort);
                page.navigate(file.toUri()+(extent?"#test-view":""));
                if(extent){
                    page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");
                    page.locator(".test-list-wrapper .test-list-item>li.test-item[test-id]").nth(1).click();
                    page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Expand all nodes").setExact(true)).click();
                }else {
                    page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("Tests").setExact(true)).click();
                    page.locator("#tests details").evaluateAll("nodes => nodes.forEach(n => n.open=true)");
                }
                var frames=page.locator(extent?".test-content-detail a:is([data-atf-screenshot],[data-atf-screenshot-ref]):visible":"#tests a[data-atf-screenshot]:visible");
                for(int which:List.of(39,40,41)) {
                    var anchor=frames.nth(extent?which:which+3);anchor.scrollIntoViewIfNeeded();anchor.focus();
                    String caption=anchor.getAttribute("aria-label");
                    assertTrue(caption.contains("Owner 1"),"Deduplication retains the local capture caption");
                    for(String action:List.of("click","Enter"," ")) {
                        anchor.focus();Object before=state(page,extent);
                        if(action.equals("click"))anchor.click();else anchor.press(action);
                        var dialog=page.getByRole(AriaRole.DIALOG,new Page.GetByRoleOptions().setName("Screenshot viewer"));
                        dialog.waitFor();var image=dialog.locator("img");
                        String shape=which==39?"portrait":which==40?"landscape":"square";
                        assertEquals("data:image/png;base64,"+ScreenshotViewerFixture.png(shape),image.getAttribute("src"));
                        assertTrue(dialog.innerText().contains(caption.replaceFirst("^Open ","")),"Local caption belongs to clicked owner");
                        page.waitForFunction("() => document.querySelector('.atf-screenshot-viewer img')?.complete===true");
                        assertFit(page);
                        if(action.equals("click")){page.setViewportSize(320,480);assertFit(page);}
                        dialog.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Original size").setExact(true)).click();
                        assertEquals("original",dialog.getAttribute("data-view-mode"));
                        page.waitForFunction("e => {const r=e.getBoundingClientRect();return Math.abs(r.width-e.naturalWidth)<1&&Math.abs(r.height-e.naturalHeight)<1;}",image.elementHandle());
                        dialog.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Fit to screen").setExact(true)).click();
                        assertFit(page);if(action.equals("click")){page.setViewportSize(1280,800);assertFit(page);}
                        if(action.equals("Enter"))page.keyboard().press("Escape");
                        else dialog.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Close screenshot").setExact(true)).click();
                        if(action.equals("click"))assertContext(before,state(page,extent));else assertRestored(before,state(page,extent));
                        page.waitForFunction("e => {const r=e.getBoundingClientRect();return r.height>0&&r.bottom>0&&r.top<innerHeight;}",anchor.elementHandle());
                        assertEquals(true,anchor.evaluate("e => document.activeElement===e"),"Focus returns without scrolling");
                    }
                }
                assertTrue(errors.isEmpty(),errors.toString());
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
    private static Object state(Page page,boolean extent){
        return page.evaluate("""
            extent => ({hash:location.hash,selected:document.querySelector('.test-list-item>.test-item.active')?.getAttribute('test-id'),
             owner:document.querySelector('.test-content-detail .detail-head h5')?.textContent,
             nodes:[...document.querySelectorAll(extent?'.test-content-detail .card-header[aria-expanded]':'#tests details')].map(e=>
                 extent?e.getAttribute('aria-expanded'):e.open),
             scroll:[...document.querySelectorAll(extent?'.test-content-detail':'html,body')].map(e=>[Math.round(e.scrollTop),Math.round(e.scrollLeft)]),
             ranges:[...document.querySelectorAll(extent?'.test-content-detail':'html,body')].map(e=>[Math.max(0,e.scrollHeight-e.clientHeight),Math.max(0,e.scrollWidth-e.clientWidth)])})
            """,extent);
    }
    @SuppressWarnings("unchecked")
    private static void assertContext(Object before,Object after){
        var expected=new LinkedHashMap<>((Map<String,Object>)before);var actual=new LinkedHashMap<>((Map<String,Object>)after);
        for(String key:List.of("scroll","ranges")){expected.remove(key);actual.remove(key);}assertEquals(expected,actual,"Same owner and disclosures after viewport reflow");
    }
    @SuppressWarnings("unchecked")
    private static void assertRestored(Object before,Object after){
        var expected=new LinkedHashMap<>((Map<String,Object>)before);var actual=new LinkedHashMap<>((Map<String,Object>)after);
        var positions=(List<List<Number>>)expected.get("scroll");var ranges=(List<List<Number>>)actual.get("ranges");
        expected.put("scroll",clamped(positions,ranges));expected.remove("ranges");actual.remove("ranges");
        assertEquals(expected,actual,"Same owner/disclosures; scroll restored within the document's current legal range");
    }
    private static List<List<Integer>> clamped(List<? extends List<? extends Number>> positions,List<? extends List<? extends Number>> ranges){
        var result=new ArrayList<List<Integer>>();
        for(int i=0;i<positions.size();i++)result.add(List.of(Math.min(positions.get(i).get(0).intValue(),ranges.get(i).get(0).intValue()),
                Math.min(positions.get(i).get(1).intValue(),ranges.get(i).get(1).intValue())));
        return result;
    }
    @Test void aViewportReflowPermitsOnlyTheExactLegalScrollClamp(){
        assertEquals(List.of(List.of(10795,0)),clamped(List.of(List.of(11064,0)),List.of(List.of(10795,0))));
        var before=Map.<String,Object>of("owner","original","scroll",List.of(List.of(11064,0)),"ranges",List.of(List.of(12000,0)));
        var wrong=Map.<String,Object>of("owner","original","scroll",List.of(List.of(0,0)),"ranges",List.of(List.of(10795,0)));
        assertThrows(AssertionError.class,()->assertRestored(before,wrong));
    }
    private static void assertFit(Page page){
        page.waitForFunction("""
            () => {const d=document.querySelector('.atf-screenshot-viewer'),i=d?.querySelector('img');if(!i?.naturalWidth)return false;
             const r=i.getBoundingClientRect(),s=d.querySelector('.atf-screenshot-stage').getBoundingClientRect();
             return r.width>0&&r.left>=s.left-1&&r.right<=s.right+1&&r.top>=s.top-1&&r.bottom<=s.bottom+1
                &&Math.abs(r.width/r.height-i.naturalWidth/i.naturalHeight)<0.003;}
            """);
        assertEquals(true,page.evaluate("""
            () => {const d=document.querySelector('.atf-screenshot-viewer'),r=d.getBoundingClientRect(),s=d.querySelector('.atf-screenshot-stage');
             return Math.abs(r.width-innerWidth)<=1&&Math.abs(r.height-innerHeight)<=1&&s.scrollWidth<=s.clientWidth&&s.scrollHeight<=s.clientHeight;}
            """));
    }
}
