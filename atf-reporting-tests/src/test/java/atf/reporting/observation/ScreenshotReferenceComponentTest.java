package atf.reporting.observation;

import atf.reporting.support.screenshot.ScreenshotViewerFixture;
import atf.testkit.AtfComponentTest;
import atf.reporting.contract.*;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.evidence.ExtentReportScreenshotDeduplicator;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Captured PNG identity and local captions survive publication without invented transports. */
@AtfComponentTest
class ScreenshotReferenceComponentTest {
    @ParameterizedTest @ValueSource(booleans={true,false})
    void explicitObservationMediaKeepsTheImageWithoutInventingAssertions(boolean extent)throws Exception {
        var file=TestReportPaths.report("observation-media-"+extent+".html");String png=ScreenshotViewerFixture.png("square");
        try(AtfReportPorts.Session session=extent?new ExtentReportSession(file,new ReportingSettings(240,12000))
                :new HtmlReportSession(file,new ReportingSettings(240,12000))){
            var raw=session.startScenario("Observation media","Recorded visual fact; no business comparison",List.of("component","synthetic"));
            try(var scenario=UiExtentReportSession.adaptScenario(raw,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),"Synthetic captured frame");
                var phase=scenario.startPhase("TEST BODY")){
                var observe=new AtfUiActionRecord(1,"OBSERVE","Synthetic surface","Captured frame available","Captured frame retained",5,
                        AtfReportStatus.INFO,AtfUiEvidenceSource.OBSERVE);
                phase.uiValidationAsMedia("Observation only","Synthetic surface",png,List.of(),List.of(observe),AtfReportStatus.INFO);
                phase.complete(AtfReportStatus.INFO);scenario.complete(AtfReportStatus.INFO);
            }
        }
        var doc=Jsoup.parse(Files.readString(file));var image=doc.selectFirst("a[href^=data:image/png;base64,]");assertNotNull(image);
        assertEquals("data:image/png;base64,"+png,image.attr("href"));
        assertTrue(doc.select("table.helm-validation-table>tbody>tr,table.assertions>tbody>tr").isEmpty(),"No artificial business comparisons");
        assertTrue(doc.text().contains("OBSERVE"));assertTrue(doc.text().contains("Observation only"));
    }
    @Test void keepsTheLocalCaptionEscapedExactlyOnce()throws Exception {
        String first="Canonical frame",local="Local <frame> & \"quoted\" literal &#13; ♠";
        String png=ScreenshotViewerFixture.png("square");
        String html=ExtentReportScreenshotDeduplicator.deduplicate(
                ExtentReportAssets.base64ScreenshotLink(null,first,png)+ExtentReportAssets.base64ScreenshotLink(null,local,png));
        var links=Jsoup.parse(html).select("a");assertEquals(2,links.size());
        assertEquals("Open "+local,links.get(1).attr("aria-label"));
        assertEquals("Open "+local,links.get(1).attr("title"));
        assertEquals(1,Jsoup.parse(html).select("a[href^=data:image/png;base64,]").size());
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void everyReferenceRetainsItsOwnCaptionAndOriginalImage(boolean extent)throws Exception {
        var file=ScreenshotViewerFixture.publish(extent);var doc=Jsoup.parse(Files.readString(file));
        var links=doc.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]");assertEquals(45,links.size());
        int ownerZero=0,ownerOne=0;
        for(var link:links){
            String caption=link.attr("aria-label");
            if(caption.contains("Owner 0"))ownerZero++;else if(caption.contains("Owner 1"))ownerOne++;else fail("Lost source caption");
            String href=link.attr("href");if(!href.startsWith("data:image/png;base64,")){
                var canonical=doc.getElementById("atf-screenshot-"+link.attr("data-atf-screenshot-ref"));assertNotNull(canonical);href=canonical.attr("href");
            }
            String shape=caption.contains("landscape")?"landscape":caption.contains("square")?"square":"portrait";
            assertEquals("data:image/png;base64,"+ScreenshotViewerFixture.png(shape),href,"Each invocation keeps the exact original image");
        }
        assertEquals(3,ownerZero);assertEquals(42,ownerOne);
        assertEquals(extent?3:45,doc.select("a[href^=data:image/png;base64,]").size());
        assertEquals(45,doc.select(extent?"table.helm-validation-table>tbody>tr":"table.assertions>tbody>tr").size());
        assertTrue(doc.select("a[href^=file:]").isEmpty());
    }
}
