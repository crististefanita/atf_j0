package atf.reporting.support.screenshot;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

/** Neutral repeated screenshots; no Android, HTTP, SQL or business execution. */
public final class ScreenshotViewerFixture {
    private ScreenshotViewerFixture() { }
    public static String png(String shape) throws Exception {
        int width = shape.equals("portrait") ? 360 : shape.equals("landscape") ? 640 : 320;
        int height = shape.equals("portrait") ? 640 : shape.equals("landscape") ? 360 : 320;
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x=0;x<width;x++) for(int y=0;y<height;y++) {
            int color = x<width/2 ? (y<height/2 ? 0xffcc3344 : 0xff33aa66) : (y<height/2 ? 0xff3388cc : 0xffccaa33);
            image.setRGB(x,y,color);
        }
        try(var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image,"png",bytes); return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }
    }
    public static Path publish(boolean extent) throws Exception {
        Path report=TestReportPaths.report("screenshot-viewer-"+extent+".html");
        var settings=new ReportingSettings(240,12000);
        try(AtfReportPorts.Session session=extent?new ExtentReportSession(report,settings):new HtmlReportSession(report,settings)) {
            for(int owner=0;owner<2;owner++) {
                var raw=session.startScenario("PngOwner"+owner,"atf.fixture","Captured frame",List.of("component","synthetic"));
                try(var scenario=UiExtentReportSession.adaptScenario(raw,
                        ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),"Synthetic capture");
                    var phase=scenario.startPhase("TEST BODY")) {
                    for(int step=0;step<(owner==0?1:40);step++)
                        phase.uiValidation("Owner "+owner+" · portrait "+step,"Synthetic portrait",png("portrait"),
                                List.of(new AtfAssertion("Captured index",Integer.toString(step),Integer.toString(step),true)),AtfReportStatus.PASS);
                    for(String shape:List.of("landscape","square"))
                        phase.uiValidation("Owner "+owner+" · "+shape,"Synthetic "+shape,png(shape),
                                List.of(new AtfAssertion("Complete frame",shape,shape,true)),AtfReportStatus.PASS);
                    phase.complete(AtfReportStatus.PASS);scenario.complete(AtfReportStatus.PASS);
                }
            }
        }
        return report;
    }
}
