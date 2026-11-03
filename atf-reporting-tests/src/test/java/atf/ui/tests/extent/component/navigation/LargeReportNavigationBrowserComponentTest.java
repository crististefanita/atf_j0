package atf.ui.tests.extent.component.navigation;

import atf.testkit.AtfComponentTest;
import atf.reporting.support.navigation.LargeNavigationFixture;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Physical report navigation keeps the original source evidence intact. */
@AtfComponentTest
class LargeReportNavigationBrowserComponentTest {
    @Test void switchesOwnersAndOpensTheFinalCheckpointWithoutBlocking() throws Exception {
        String input=System.getProperty("atf.navigation.input");
        Path file=input==null?LargeNavigationFixture.publish():Path.of(input);
        try(var pw=Playwright.create();var browser=pw.chromium().launch();var page=browser.newPage()) {
            var errors=new ArrayList<String>();page.onPageError(errors::add);page.setDefaultTimeout(10000);
            page.setDefaultNavigationTimeout(45000);page.route("http://**/*",Route::abort);page.route("https://**/*",Route::abort);
            long began=System.nanoTime();page.navigate(file.toUri()+"#test-view");
            page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready')==='true'");
            System.out.println("LARGE_READY_MS="+(System.nanoTime()-began)/1000000);
            Object original=evidence(page);
            var owners=page.locator("#test-view .test-list-wrapper .test-list-item>li.test-item[test-id]");
            int count=owners.count();assertTrue(count>=2);
            assertEquals(count,((List<?>)original).size());
            if(input==null)for(Object row:(List<?>)original){
                var inventory=(Map<?,?>)row;assertEquals(12004,((Number)inventory.get("cells")).intValue());
                assertEquals(1,((Number)inventory.get("frames")).intValue());
            }
            var pendingBulk=selectedBulkOwners(System.getProperty("atf.navigation.expandOwners",""));
            assertTrue(pendingBulk.stream().allMatch(index->index<count),"Selected bulk owners must exist in this report");
            var sequence=new ArrayList<>(List.of(count-1,0));
            for(int owner:pendingBulk)if(!sequence.contains(owner))sequence.add(owner);
            sequence.add(count-1);
            for(int which:sequence) {
                String selectedName=owners.nth(which).locator(".test-detail .name").innerText();
                began=System.nanoTime();owners.nth(which).locator(".test-detail").click();
                com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("#test-view .test-content-detail .detail-head h5:visible")).hasText(selectedName);
                System.out.println("LARGE_OWNER_MS="+(System.nanoTime()-began)/1000000);
                var expand=page.locator("#test-view .test-content-detail [role=button][aria-label='Expand all nodes']:visible");
                began=System.nanoTime();
                boolean selectedBulk=pendingBulk.remove(which);
                if(selectedBulk)System.out.println("LARGE_BULK_OWNER="+which);
                if((input==null&&which!=0)||selectedBulk){expand.click();assertExpandedTree(page);}
                else openCheckpoint(page,which);
                System.out.println("LARGE_DISCLOSURE_MS="+(System.nanoTime()-began)/1000000);
                System.out.println("LARGE_METRICS="+page.evaluate("() => window.__atfReportPerformance"));
                var frames=page.locator("#test-view .test-content-detail a:is([data-atf-screenshot],[data-atf-screenshot-ref]):visible");
                if(frames.count()>0){
                    var frame=frames.last();String caption=frame.getAttribute("aria-label");frame.click();
                    var viewer=page.locator("dialog.atf-screenshot-viewer[open]");viewer.waitFor();
                    assertEquals("Screenshot viewer",viewer.getAttribute("aria-label"));
                    assertTrue(viewer.innerText().contains(caption.replaceFirst("^Open ","")));
                    capture(page,"owner-"+which+"-png");
                    viewer.getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("Close screenshot").setExact(true)).click();
                }
            }
            assertTrue(pendingBulk.isEmpty(),"Every explicitly selected bulk owner must be exercised once");
            capture(page,"final-owner");
            assertEquals(original,evidence(page),"Every original comparison cell and PNG reference remains byte-identical in its source owner");
            var canonical=page.locator("#test-view .test-list-wrapper a[data-atf-screenshot]").first();
            assertTrue(canonical.count()>0,"Canonical PNG bytes must exist independently of deduplicated references");
            canonical.evaluate("e => {const v=e.getAttribute('data-atf-screenshot');e.setAttribute('data-atf-screenshot',(v[0]==='A'?'B':'A')+v.slice(1));}");
            assertNotEquals(original,evidence(page),"Changing only a canonical PNG payload must invalidate the retained-evidence inventory");
            assertTrue(errors.isEmpty(),errors.toString());
        }
    }
    @Test void bulkSelectionIsBoundedAndRejectsAmbiguousInput(){
        assertEquals(Set.of(1,2),selectedBulkOwners("1,2"));assertTrue(selectedBulkOwners("").isEmpty());
        for(String invalid:List.of("all","-1","1,1","1,2,3","1,"))
            assertThrows(IllegalArgumentException.class,()->selectedBulkOwners(invalid));
    }
    private static Set<Integer> selectedBulkOwners(String input){
        var owners=new LinkedHashSet<Integer>();if(input.isBlank())return owners;
        var values=input.split(",",-1);if(values.length>2)throw new IllegalArgumentException("At most two bulk owners are allowed");
        for(String value:values){int owner=Integer.parseInt(value.trim());
            if(owner<0||!owners.add(owner))throw new IllegalArgumentException("Bulk owners must be distinct non-negative indices");}
        return owners;
    }
    private static void assertExpandedTree(Page page){
        var cards=page.locator("#test-view .test-content-detail :is([data-atf-scenario-open],[data-atf-phase-open],[data-atf-node-open])");
        assertTrue(cards.count()>0,"The selected owner must contain a real semantic disclosure tree");
        assertEquals(List.of(),cards.evaluateAll("""
            cards => cards.filter(card => ['data-atf-scenario-open','data-atf-phase-open','data-atf-node-open']
              .some(name => card.hasAttribute(name) && card.getAttribute(name)!=='true') ||
              card.querySelector(':scope>.card-header')?.getAttribute('aria-expanded')!=='true')
              .map(card => card.querySelector(':scope>.card-header')?.textContent.trim())
            """),"Every semantic scenario, phase and node in the selected owner must expand");
    }
    private static void openCheckpoint(Page page,int owner) {
        var pane=page.locator("#test-view .test-content-detail");
        var row=pane.locator(".helm-validation-table tbody tr").last();
        if(row.count()>0){openPath(row);assertTrue(row.isVisible());capture(page,"owner-"+owner+"-assertion");}
        var frame=pane.locator("a:is([data-atf-screenshot],[data-atf-screenshot-ref])").last();
        if(frame.count()>0)openPath(frame);
    }
    private static void openPath(Locator target) {
        var ancestors=target.locator("xpath=ancestor::*[self::details or contains(concat(' ',normalize-space(@class),' '),' card ')]");
        for(int pass=0;pass<ancestors.count()&&!target.isVisible();pass++){
            boolean progressed=false;
            for(Locator ancestor:ancestors.all()){
                if("details".equals(ancestor.evaluate("e=>e.localName"))){
                    var summary=ancestor.locator(":scope>summary");
                    if(summary.isVisible()&&!Boolean.TRUE.equals(ancestor.evaluate("e=>e.open"))){summary.click();progressed=true;}
                }else{
                    var header=ancestor.locator(":scope>.card-header");
                    if(header.count()>0&&header.isVisible()&&"false".equals(header.getAttribute("aria-expanded"))){
                        header.press("Enter");
                        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(header).hasAttribute("aria-expanded","true");
                        progressed=true;
                    }
                }
            }
            if(!progressed)break;
        }
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(target).isVisible();
        target.scrollIntoViewIfNeeded();
    }
    private static void capture(Page page,String name) {
        String output=System.getProperty("atf.navigation.screenshots");
        if(output!=null)page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(output).resolve(name+".png")));
    }
    private static Object evidence(Page page) {
        return page.evaluate("""
            async () => await Promise.all([...document.querySelectorAll('#test-view .test-list-wrapper .test-list-item>li.test-item[test-id]')].map(async owner => {
              const cells=[...owner.querySelectorAll('.helm-validation-table td,.atf-ui-assertions td,pre.code-block')].map(e=>e.textContent);
              const frames=[...owner.querySelectorAll('a[data-atf-screenshot],a[data-atf-screenshot-ref]')].map(e=>
                [e.getAttribute('href'),e.getAttribute('data-atf-screenshot'),e.getAttribute('data-atf-screenshot-ref'),e.getAttribute('aria-label')]);
              const bytes=new TextEncoder().encode(JSON.stringify([cells,frames]));
              const hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))].map(n=>n.toString(16).padStart(2,'0')).join('');
              return {id:owner.getAttribute('test-id'),cells:cells.length,frames:frames.length,sha256:hash};
            }))
            """);
    }
}
